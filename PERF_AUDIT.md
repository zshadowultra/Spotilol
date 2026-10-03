# Spotilol WebView Performance Audit

**Date:** 2026-10-03
**Scope:** All JS/CSS injected into the Spotify WebView (`app/src/main/java/com/project/lol/webview/`)
**Method:** Read every injection file, measured payload sizes, inventoried all timers/observers/selectors. No code modified.

## Injection architecture (what runs when)

| Phase | Trigger | Payload | Size |
|---|---|---|---|
| Document-start | `onPageStarted` → `buildEarlyJs()` (also registered via `WebViewCompat.addDocumentStartJavaScript`) | BrowserSpoof, FetchOverride, AdStateHook, WorkerNeutralize, GaBlocker, PowerSave, SettingsFix, VideoPark, CookieBypass + flags | ~38 KB |
| Post-login | `onPageFinished` + 500ms delay → `injectPlayerControl()` | PlayerCore, TrackObserver, ClassicBridge, MediaUpdater, LibraryFetcher, LibraryParser, PlaybackControls, AndroidAuto, MainLoop, AutoFeatures, AndroidTracker, SearchOverlay, DownloadButton, DownloadProgress, CollectionDownload, ContextMenuDownload, CssHack, ModalFix, ErrorDialogRestyle, ToastFix, LyricsSyncFix, QueueAutoClose, LibraryAutoClose, PlaylistSort, SpotilolPlayer (+ Amoled/Accent/CustomCss/LyricsTheme) | ~275 KB |
| **Total per full page load** | | | **~313 KB JS** |

Notes:
- SPA navigations inside Spotify do NOT re-trigger `onPageFinished`, so the big payload runs once per full document load. Good.
- `evaluateJavascript` with a ~275 KB string crosses JNI and must be parsed + compiled. On low-end devices this is 100–300 ms of main-thread JS work right after page load.
- `shouldInterceptRequest` runs on every resource request (IO thread, not UI thread — OK), but does a **blocking `HttpURLConnection`** (5 s timeouts) for Google auth URLs.

## Timer / observer inventory

| Source | Kind | Interval | Work per tick |
|---|---|---|---|
| `SpotilolPlayer.kt:484` | rAF loop (`rafUpdate`) | ~100 ms | `splUpdate()`: ~15 `getElementById` + 6 `querySelector` + `innerHTML` rewrites + `classList` toggles |
| `PlayerCore.kt:30` | `setInterval` (floaters dispatcher) | 250 ms | calls `LyricsSyncFix.tick` + `ToastFix.splTick` |
| `LyricsSyncFix.kt:156` | floater `tick` | 250 ms | `reposition()` (rect+computed-style per tracked btn); `rebalance()` every 800 ms (`querySelectorAll` + up to 7× `getComputedStyle` per button) |
| `SpotilolPlayer.kt:270` | `setInterval` (PiP fill) | 700 ms | fill apply (only when PiP active) |
| `AndroidTracker.kt:39` | MutationObserver (now-playing-bar subtree, **no attributeFilter**) | on mutation, debounced 200 ms | `readTrackState()`: ~8 `querySelector` + `splIsPlayingSticky()` |
| `TrackObserver.kt` | MutationObserver (widget-scoped) + watchdog | 1000 ms | 1 `querySelector`; observer is well-scoped |
| `PlaylistSort.kt:549` | `setInterval(decorate)` | 1000 ms | `injectStyle()` + `querySelectorAll([role=columnheader])` + `setAttribute` per cell + `injectMenu()` |
| `CollectionDownload.kt:308` | `setInterval(tick)` | 2000 ms | `hijack()` (`querySelectorAll` action-bar-row + buttons) + `ensureButtons()` + `syncButtons()` |
| `SearchOverlay.kt:665` | `setInterval` | 2000 ms | `navBar.isConnected` + `bindSearchIcon()` (1 `querySelector`) |
| `MainLoop.kt:7` | `setInterval` (pfint) | 5000 ms | 2–3 `querySelector` |
| `AutoFeatures.kt:11` | `setInterval` (afint) | 5000 ms | `closeNowPlay()` + 2 `querySelector` + `splIsPlaying()` |
| `CssHack.kt:32` | `setInterval` (cssint) | 5000 ms | ~8 `querySelector` (never terminates; `:not(.fuckd)` guards make repeat scans cheap but the traversal still happens) |
| `DownloadButton.kt:36` | `setInterval` | 5000 ms | 2 `querySelector` |
| `SettingsFix.kt:110` | MutationObserver (**documentElement, whole subtree, unfiltered callback**) | **every DOM mutation** | `interceptAll()`: 3 full-document `querySelectorAll` |
| `ContextMenuDownload.kt:250` | MutationObserver (body subtree) | on mutation, debounced 60 ms | filtered for context-menu nodes only — OK |
| `LyricsSyncFix.kt:161` | MutationObserver (body subtree) | on mutation, debounced 120 ms | `rebalance()` — expensive (see above) |
| `ErrorDialogRestyle.kt:95` | MutationObserver (body subtree) | on mutation | scans addedNodes only — OK |
| `CookieBypass.kt:18` | MutationObserver | — | disconnects after 15 s — OK |
| `PowerSave.kt:78` | MutationObserver (body subtree) | — | only when PowerSave pref on — OK |

---

## P0 — Fix first (biggest wins)

### 1. `splUpdate()` rewrites play-button `innerHTML` 10×/second unconditionally
**File:** `webview/injections/SpotilolPlayer.kt:393`
```js
if(pp)pp.innerHTML=ph;
if(ppm)ppm.innerHTML=ph;
```
`splUpdate()` runs every ~100 ms via the perpetual `rafUpdate` loop (`SpotilolPlayer.kt:480-487`). The play/pause SVG `innerHTML` is reassigned on **every tick even when nothing changed** — destroying and recreating DOM nodes, forcing layout + paint 10×/second, forever, including when the tab is visible but idle.

**Fix:** cache last state, only touch DOM on change:
```js
if(pp && pp.__splPh !== ph){ pp.innerHTML = ph; pp.__splPh = ph; }
if(ppm && ppm.__splPh !== ph){ ppm.innerHTML = ph; ppm.__splPh = ph; }
```
Same guard already exists for the shuffle sparkle (`hasSparkle` check) — extend the pattern. Zero functionality change.

### 2. `splUpdate()` 10 Hz DOM scrape is far more frequent than needed
**File:** `webview/injections/SpotilolPlayer.kt:359-430`, driven by `rafUpdate` at `:480-487`

Each tick does ~15 `getElementById` (cheap) plus 6 `document.querySelector` with attribute selectors (not cheap), plus `splIsPlayingSticky()` → `splIsPlaying()` (`PlayerCore.kt`) which does more `querySelector` + `getAttribute('d')` + locale regex tests. Track title/artist/cover/shuffle/repeat state changes at most a few times per minute; progress fill is already transform-based.

**Fix (no functionality change):**
- Throttle `rafUpdate` from 100 ms to 500 ms (progress text is 1-second resolution anyway).
- Cache the `getElementById` node references once at init instead of re-resolving 15 IDs per tick.
- Skip the whole tick when `document.visibilityState === 'hidden'` or `window.__splBg`.
- Better: drive play/pause icon updates from the existing `trackchange` event + a `MutationObserver` on the now-playing widget instead of polling.

**Estimated impact:** removes ~90% of per-second DOM read/write volume in the app — this is the single hottest loop.

### 3. `SettingsFix` observer re-scans the entire document on every DOM mutation
**File:** `webview/injections/SettingsFix.kt:110-114`
```js
var obs = new MutationObserver(function() { interceptAll(); });
obs.observe(document.documentElement, { childList: true, subtree: true });
```
`interceptAll()` (`:67-99`) runs **3 full-document `querySelectorAll`** (`a[target="_blank"]`, `ms-store-badge`, `img[src*="get.microsoft.com"]`) on *every* mutation batch. Spotify's SPA does heavy DOM churn on navigation and playlist render — dozens of mutation batches per navigation, each triggering 3 whole-document walks. The `__splIntercepted` flag makes repeat *work* cheap but the *scans* still cost.

**Fix:** filter mutations before scanning (same pattern `ContextMenuDownload.kt:250` already uses):
```js
var obs = new MutationObserver(function(muts){
    var dirty = false;
    for (var i = 0; i < muts.length && !dirty; i++)
        if (muts[i].addedNodes.length) dirty = true;
    if (dirty) interceptAll();
});
```
Or debounce `interceptAll()` to 500 ms. Either preserves behavior — the Microsoft badge/img blocking is not time-critical.

---

## P1 — Fix next

### 4. Eleven `:has()` selectors in injected CSS, including one over all descendants
**File:** `webview/injections/CssHack.kt` (style string)

`:has()` forces reverse selector matching — the browser must check children for every candidate element, on every style recalc:
- `li:has(>a[href*="spotify.com/premium"])`, `li:has(>a[href*="support.spotify.com"])`, `li:has(>a[href*="spotify.com/download"])` — every `<li>` in the app re-evaluated per DOM change.
- `[data-tippy-root]:not(:has([role=menu]))` and **`[data-tippy-root]:not(:has([role=menu])) *`** — the `*` variant runs a `:has()` check against **every descendant** of every tooltip.
- `@media(orientation:landscape){#global-nav-bar > div:has([data-testid="home-button"]){...}}` ×3.

**Fix:** replace with JS-added marker classes. E.g. on injection (or in the existing `cssint` warden), do `document.querySelectorAll('li > a[href*="spotify.com/premium"]')` once and add a `spl-hide` class to the parent `li`; CSS becomes `li.spl-hide{display:none!important}`. Same for the tippy-root case: add/remove a class when a menu opens/closes instead of `:has([role=menu])`. One-time DOM cost instead of per-recalc cost.

### 5. Universal selector `*{--content-spacing:10px}`
**File:** `webview/injections/CssHack.kt` (style string)

Forces the style engine to match every element in the document. It's only setting a custom property, but it still participates in every style invalidation.

**Fix:** scope it: `div[data-testid=root]{--content-spacing:10px}` (the file already targets `div[data-testid=root]` elsewhere) or set the property via JS on `document.documentElement.style`.

### 6. Permanent `will-change:transform` on the player bar
**File:** `webview/injections/CssHack.kt` — `#spotilolPlayerControls{...will-change:transform!important;...}`

Pins a dedicated compositor layer for the player forever. The bar is `position:fixed` (already composited); the permanent `will-change` wastes GPU memory and can hurt on low-end devices.

**Fix:** toggle it only during the mini/full transition (the CSS already transitions `max-height`/`transform` on `.spl-mini`): add `will-change` via JS before toggling the class, remove on `transitionend`. Or drop it — `transform`/`opacity` transitions are composited without the hint.

### 7. `LyricsSyncFix.rebalance()` does `getComputedStyle` × up to 7 per button, every 800 ms + on DOM mutations
**File:** `webview/injections/LyricsSyncFix.kt:37-75,133-156,161-180`

`visibleEl()` calls `getBoundingClientRect()` + `getComputedStyle()` on the button, then walks up to 6 ancestors calling `getComputedStyle()` on each — a layout/style-read cascade. `pickCandidate()` runs this for every `button[data-encore-id="buttonPrimary"]` on the page. Triggered every 800 ms via the floater **and** on every body DOM mutation (120 ms debounce). Each `getComputedStyle` after DOM writes forces synchronous style recalc.

**Fix:**
- Throttle `rebalance()` to 2000 ms and skip entirely unless a lyrics view is likely open (guard on `document.querySelector('[data-testid="lyrics"]')` or similar).
- In `visibleEl()`, check `offsetParent !== null` / `getClientRects().length` before the expensive computed-style walk (cheap visibility pre-filter).
- The mutation observer can stay but should early-return when `tracked` is empty and no candidate exists.

### 8. `PlaylistSort` re-decorates every second
**File:** `webview/injections/PlaylistSort.kt:549` — `setInterval(decorate, 1000)`

`decorate()` (`:250-273`) calls `injectStyle()` + `querySelectorAll('[role="columnheader"]')` + `setAttribute`/`removeAttribute` per cell + `injectMenu()` **every second, forever**. Attribute writes invalidate style; `injectMenu()` re-scans for the columns menu.

**Fix:** make it event-driven — the file already has `menuObserver` (`:408`); extend observation to the column headers (observe the thead/grid container with `childList:true, subtree:true`) and drop the 1 s interval, keeping a 5 s fallback warden. `injectStyle()` should early-return if the style element already exists (check before doing work).

---

## P2 — Worth doing

### 9. Consolidate the seven "warden" intervals
`MainLoop pfint` (5 s), `AutoFeatures afint` (5 s), `CssHack cssint` (5 s), `DownloadButton` (5 s), `SearchOverlay` (2 s), `CollectionDownload tick` (2 s), `TrackObserver watchdog` (1 s) — all do "is the thing I wired still there?" scans. Each wakes the JS thread independently, preventing the renderer from ever going idle.

**Fix:** single `window.__splWarden` 5 s interval in `PlayerCore.kt` that calls registered check functions (same registry pattern as `__splFloaters`). Keep `TrackObserver`'s 1 s watchdog (remount detection needs to be snappy) but move the rest in. Also add `if(window.__splBg) return` to the ones missing it (`PlaylistSort`, `CollectionDownload tick` has it, `DownloadButton` doesn't).

### 10. `AndroidTracker` observer has `attributes:true` with no `attributeFilter`
**File:** `webview/injections/AndroidTracker.kt:44`

```js
obs.observe(npTarget, { childList: true, subtree: true, attributes: true, characterData: true });
```
Every attribute mutation anywhere in the now-playing-bar subtree (class toggles from React re-renders, `aria-*` updates) fires the callback → debounced 200 ms → `readTrackState()` (8 `querySelector` + `splIsPlayingSticky()`).

**Fix:** add `attributeFilter: ['aria-checked', 'aria-label', 'href', 'src']` — the only attributes `readTrackState` actually reads. Cuts observer fire rate dramatically during React re-renders.

### 11. `splShuffleBtn()` full-document button scan fallback
**File:** `webview/injections/PlaybackControls.kt:170-175`

```js
var bs = document.querySelectorAll('button');
for(var i=0;i<bs.length;i++){ var ic = bs[i].querySelector('svg path'); ... }
```
Only reached when the cheap `data-testid` lookups fail, but when reached it's O(all buttons × svg lookup). Called from `splUpdate` (10 Hz) and `readTrackState`.

**Fix:** cache the found button in a variable; re-resolve only if `!cached.isConnected`. With finding #2's throttle this becomes a non-issue, but cache anyway — it's 3 lines.

### 12. Split the 275 KB post-login payload: critical vs deferred
**File:** `webview/SpotifyWebViewClient.kt:335-384` (`injectPlayerControl`)

Everything injects at once 500 ms after page finish: player UI (needed immediately) alongside downloads UI (`CollectionDownload` 15 KB, `ContextMenuDownload` 10 KB), `PlaylistSort` (28 KB), `SearchOverlay` (40 KB) — none needed until the user opens those surfaces.

**Fix:** inject the player core set immediately; defer the rest with `requestIdleCallback` (fallback `setTimeout 3000`) or lazily on first use (e.g. `PlaylistSort` on first playlist navigation, `SearchOverlay` on first search-icon tap). Cuts time-to-interactive JS parse cost roughly in half. Requires care: `MainLoop.wirePlayBtn` calls `addCSSJSHack()` etc. — keep the call graph intact, just delay the `evaluateJavascript` chunks.

---

## P3 — Minor / opportunistic

### 13. `FetchOverride` runs 4 regex `.match()` on every `fetch` URL
**File:** `webview/injections/FetchOverride.kt:28-58` — plus `AdStateHook.kt` wraps `fetch` a second time, so every request passes two wrapper layers and state-endpoint responses get cloned + parsed twice.

**Fix:** cheap `indexOf` pre-checks before the regexes (most already have them — move the regexes behind the `indexOf` guards uniformly). Consider merging the two wrappers into one. Impact is small (~µs per request) — do it only while touching the file.

### 14. `shouldInterceptRequest` does blocking network I/O for Google auth URLs
**File:** `webview/SpotifyWebViewClient.kt:243-296` — `HttpURLConnection` with 5 s connect/read timeouts on the IO thread for every `*.google.com`/`*.youtube.com` request.

OK on the IO thread, but a slow Google response stalls that resource. Only affects login flow. Consider shrinking timeouts to 2 s — login pages are small.

### 15. `backdrop-filter: blur()` on toasts and error dialogs
`ToastFix.kt` sets `backdrop-filter: blur(14px)` via `style.setProperty` on toasts; `ErrorDialogRestyle.kt` uses `blur(6px)`/`blur(24px)`. Backdrop blur forces the compositor to re-raster the backdrop on every frame of the toast's in/out animation. Toasts are small and transient — acceptable, but if toasts ever feel janky, replace with a semi-opaque solid background.

### 16. `mask-image` gradient on mini-player cover
`CssHack.kt`: `#spotilolPlayerControls.spl-mini .spl-cover img{...mask-image:linear-gradient(...)}` — masks force software rasterization of the image layer. Minor; only in mini mode. Could use a pseudo-element fade instead.

---

## Already good — do not regress

- `contain: layout style` on `div[data-testid=root]` and `.YourLibraryX` (`CssHack.kt`) — keep, and consider `contain: strict` on `#spotilolPlayerControls` (it's `position:fixed`, fully isolated).
- `WorkerNeutralize.kt:66-76` throttles Spotify's own aggressive 250 ms intervals to 500 ms when `BlockServiceWorker` is on (default) — genuine win, keep.
- `PowerSave.kt:104-112` throttles page `setInterval` when PowerSave is on — keep.
- `JsUtils.stripConsoleLogs` is indexOf-driven + LRU-memoized — already optimal.
- Style elements reused by ID (`spotilol-amoled-theme`, `spotilol-custom-css`, `spotilol-lyrics-style`) — no duplication.
- `CookieBypass.kt:23` disconnects its observer after 15 s — good pattern; apply it to other one-shot observers.
- Progress fill uses `transform: scaleX` (`CssHack.kt` `.spl-fill`) — compositor-friendly, keep.
- System font stack, no webfont loading — keep.
- `WebView` has `LAYER_TYPE_HARDWARE`, `LOAD_DEFAULT` cache, back-forward cache enabled (`MainActivity.kt:454-483`) — sane.

## Suggested implementation order

1. #1 (innerHTML guard) + #2 (throttle/cache splUpdate) — one file, biggest win
2. #3 (SettingsFix observer filter) — one file, second biggest win
3. #4 (replace `:has()` with classes) + #5 (scope `*`) + #6 (will-change toggle) — CSS file, one pass
4. #7 (LyricsSyncFix throttle) + #8 (PlaylistSort event-driven) + #10 (attributeFilter)
5. #9 (warden consolidation) + #11 (button cache) + #12 (payload split)
6. #13–#16 opportunistically
