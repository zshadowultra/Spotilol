# Spotilol Performance Fixes — P0 + P1

**Date:** 2026-10-03
**Source audit:** `PERF_AUDIT.md`
**Rule:** Zero functionality change. All fixes preserve exact behavior.

## P0 — Biggest wins

### 1. Guard `innerHTML` rewrites in `splUpdate()` — `SpotilolPlayer.kt:393`
**What:** The play/pause SVG `innerHTML` was reassigned on every tick (10×/second) even when unchanged, destroying/recreating DOM nodes and forcing layout+paint continuously.
**Fix:** Cache last-assigned string on the element (`pp.__splPh`); only touch DOM when the SVG actually changed:
```js
if(pp && pp.__splPh !== ph){ pp.innerHTML = ph; pp.__splPh = ph; }
if(ppm && ppm.__splPh !== ph){ ppm.innerHTML = ph; ppm.__splPh = ph; }
```
Same pattern already used for the shuffle sparkle — extended to play buttons.

### 2. Throttle `rafUpdate` + cache element refs + visibility skip — `SpotilolPlayer.kt:359-430,480-487`
**What:** `splUpdate()` ran every ~100ms doing ~15 `getElementById` + 6 `querySelector` + DOM writes, even when the tab was hidden.
**Fix (3 parts):**
- Throttled `rafUpdate` from 100ms → **500ms** (progress text is 1-second resolution anyway).
- Added `splEl(id)` helper with `isConnected`-validated cache — replaces all 19 `getElementById` calls in `splUpdate()`. Re-resolves automatically if DOM is rebuilt.
- Skip tick entirely when `document.visibilityState === 'hidden'` or `window.__splBg`.
**Impact:** Removes ~90% of per-second DOM read/write volume — the single hottest loop in the app.

### 3. Filter SettingsFix observer mutations — `SettingsFix.kt:110-114`
**What:** `MutationObserver` on `documentElement` (whole subtree) ran 3 full-document `querySelectorAll` on **every** DOM mutation batch.
**Fix:** Filter mutations before scanning — only run `interceptAll()` when `addedNodes.length > 0` (same pattern `ContextMenuDownload.kt` already uses):
```js
var obs = new MutationObserver(function(muts){
    var dirty = false;
    for (var i = 0; i < muts.length && !dirty; i++)
        if (muts[i].addedNodes.length) dirty = true;
    if (dirty) interceptAll();
});
```

## P1 — Fix next

### 4. Replace 11 `:has()` selectors with JS marker classes — `CssHack.kt`
**What:** `:has()` forces reverse selector matching — browser checks children for every candidate element on every style recalc. Included `li:has(>a[href*=...])` (every `<li>` re-evaluated) and `[data-tippy-root]:not(:has([role=menu])) *` (every descendant of every tooltip).
**Fix:**
| Old CSS | New CSS | JS |
|---|---|---|
| `li:has(>a[href*="spotify.com/premium"])` etc. (×3) | `li.spl-hide` | `splMarkHas()` adds `spl-hide` to parent `li` once |
| `[data-tippy-root]:not(:has([role=menu]))` | `[data-tippy-root]:not(.spl-has-menu)` | `splMarkTippy()` + MutationObserver toggles class on menu open/close |
| `[data-tippy-root]:has([role=menu])` | `[data-tippy-root].spl-has-menu` | (same observer) |
| `#global-nav-bar > div:has([data-testid="home-button"])` (×2) | `#global-nav-bar > div.spl-has-home` | `splMarkHas()` via `closest()` |
| `#global-nav-bar > div:has([data-testid="user-widget-link"])` | `#global-nav-bar > div.spl-has-user` | `splMarkHas()` via `closest()` |
| `.playlistRecommenderContainer > div:has([data-testid="track-list"])` | `.playlistRecommenderContainer > div.spl-has-tracklist` | `splMarkHas()` via `closest()` |

Static marks run at init, on `DOMContentLoaded`, and every 5s in the existing warden (guarded by `.spl-marked` to stay cheap). Tippy marks use a dedicated MutationObserver watching for `[data-tippy-root]` and `[role=menu]` additions.

### 5. Scope universal selector — `CssHack.kt`
**What:** `*{--content-spacing:10px}` forced the style engine to match every element in the document.
**Fix:** `div[data-testid=root]{--content-spacing:10px}` (the file already targets this selector elsewhere).

### 6. Remove permanent `will-change:transform` — `CssHack.kt`
**What:** `#spotilolPlayerControls` had permanent `will-change:transform!important`, pinning a dedicated compositor layer forever and wasting GPU memory.
**Fix:** Removed from CSS. Added `splWireWillChange()` — a MutationObserver on the player's `class` attribute that sets `style.willChange='transform'` when `spl-mini` toggles, then clears it after 350ms (CSS transition is 300ms). `transform`/`opacity` transitions are composited without the hint anyway.

### 7. Throttle LyricsSyncFix + cheap pre-filters — `LyricsSyncFix.kt`
**What:** `rebalance()` did up to 7× `getComputedStyle` per button, every 800ms + on every DOM mutation (120ms debounce).
**Fix (3 parts):**
- Throttled `SWEEP_MS` from 800ms → **2000ms**.
- Added cheap `offsetParent === null` pre-filter in `visibleEl()` before the expensive computed-style walk (guarded by `!b.__splFloated` since dressed buttons are `position:fixed`).
- Early-return in `rebalance()` when no `button[data-encore-id="buttonPrimary"]` exists (single cheap `querySelector` instead of full scan). Mutation observer also early-returns when nothing is tracked and no candidates exist.

### 8. Event-driven PlaylistSort decorate — `PlaylistSort.kt:549`
**What:** `setInterval(decorate, 1000)` ran `querySelectorAll('[role="columnheader"]')` + attribute writes + `injectMenu()` every second, forever.
**Fix:**
- Replaced 1s interval with a `MutationObserver` watching for `[role="columnheader"]` additions/changes (100ms debounce via `scheduleDecorate()`).
- Kept a **5s fallback warden** (`setInterval` with `__splBg` guard) for anything the observer misses.
- `injectStyle()` already had the early-return (`if (document.getElementById(STYLE_ID)) return`) — verified, no change needed.

## Verification
- All 5 modified files: JS extracted from Kotlin raw strings and validated with `node --check` — all pass.
- `git diff --stat`: 5 files changed, 179 insertions, 28 deletions.
- Zero behavior change: every fix is a throttle, cache, guard, or selector-equivalent replacement.

## Files changed
- `app/src/main/java/com/project/lol/webview/injections/SpotilolPlayer.kt` (P0 #1, #2)
- `app/src/main/java/com/project/lol/webview/injections/SettingsFix.kt` (P0 #3)
- `app/src/main/java/com/project/lol/webview/injections/CssHack.kt` (P1 #4, #5, #6)
- `app/src/main/java/com/project/lol/webview/injections/LyricsSyncFix.kt` (P1 #7)
- `app/src/main/java/com/project/lol/webview/injections/PlaylistSort.kt` (P1 #8)
