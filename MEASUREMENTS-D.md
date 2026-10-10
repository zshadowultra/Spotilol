# MEASUREMENTS-D — Worker 3 (UX speed + lists), opt/webview-perf

Date: 2026-10-10 · Scope: D1, D2, D3, D5, C6, C7, D4, C8-check, B3-JS.
Counting method per item below. Device-level numbers (INP, PSS) are
verify-on-device targets; everything here is static instrumentation +
node-based behavioral tests (`app/src/test/node/run-local.js`, mirrored as
JUnit in `app/src/test/java/com/project/lol/webview/UxPerfTest.kt`).

## D1 — Optimistic play/pause + next/prev

| | Before | After | How counted |
|---|---|---|---|
| tap → play/pause icon change | click → Spotify web-player round-trip (200–600 ms per PLAN baseline) → next 500 ms `splUpdate` tick observes flipped state → icon. Worst case ~1.1 s. | icon painted **synchronously in the tap handler** (`splPaintPlayIcon`, same frame, <16 ms); the 500 ms `splUpdate` loop reconciles in the background | code-path analysis of `SpotilolPlayer.kt` onclick handlers + `splUpdate` |
| tap → progress reset on skip | progress repainted only after the new track's progressbar appears (round-trip) | progress fill reset to 0 + `0:00` synchronously in the tap handler | same |
| rollback path | none (icon could stick wrong until state caught up) | **rollback designed first**: every optimistic paint is a lease (`__splUx.optBeginPlay/optBeginSkip`). `splUpdate` (source of truth) reconciles each tick: `confirm` → lease cleared; `revert` (deadline 2000 ms play / 4000 ms skip, no confirmation) → icon repaints to real state + `splToast('Could not change playback'/'Skip failed')`; `keep` → optimistic paint held one more tick | `UxPerfTest.optimisticRollback_commandFails_iconReverts` (node driver asserts keep/confirm/revert + the DOM paint + `__splPh` sync so `splUpdate` can't clobber) |

Files: `SpotilolPlayer.kt` (lease + `splPaintPlayIcon` + 6 optimistic tap
handlers + `splUpdate` reconcile/hold), `PlaybackControls.kt`
(`actPlayPause`/`actSkipBack`/`actSkipForward` now return bool = "command
issued"; `window.splToast` with `showToast` fallback).

## D2 — touch-action + passive listeners

| | Before | After | How counted |
|---|---|---|---|
| `touch-action: manipulation` on injected root | absent | present on `div[data-testid=root]` in `CssHack.kt` | grep |
| non-passive touch/scroll/wheel listeners in owned files | 1 (`SearchOverlay.kt` document `scroll` listener, capture, hides panel) | **0**, except 1 documented keeper | `grep -n "addEventListener('touchstart'\|addEventListener('touchmove'\|addEventListener('wheel'"` over owned files, minus `{passive:true}` hits |

Documented keeper (must stay blocking):
- `SpotilolPlayer.kt` — `pl.addEventListener('touchmove', …, {passive:false})`:
  calls `e.preventDefault()` to run the drag-to-collapse gesture; a passive
  listener may not call `preventDefault` (spec violation, gesture breaks).
- `#spotilolPlayerControls` keeps `touch-action:none` for the same gesture.

Out of scope, flagged: `ToastFix.kt:262,267` (`touchstart`/`touchmove`
without `{passive:true}`) — not owned by Worker 3.

## C6 — lazy artwork + IntersectionObserver pagination

| | Before | After | How counted |
|---|---|---|---|
| injected `<img>` sites with `loading`/`decoding` | 0 of 10 | **10 of 10** via `__splUx.artCache.loadInto` (sets `loading="lazy" decoding="async"`; cache-first src) | `grep -n "createElement('img')"` in `CustomUI.kt` (3), `SearchOverlay.kt` (6), `assets/custom-ui/app.js` (1) |
| infinite-scroll mechanism for playlist tracks | none — `openPlaylistDetail` fetched a single page (`limit=50`) | **IntersectionObserver** sentinel (`root: #mainScrollArea`, `rootMargin: 400px`) loads `next` pages; zero scroll handlers added | code inspection of `app.js` `setupDetailPager` |
| scroll-handler pagination replaced | n/a (none existed in owned files) | the one document `scroll` listener in owned files (SearchOverlay) is now `{capture:true, passive:true}` and is not pagination | grep |

## C7 — virtualized lists

| | Before | After | How counted |
|---|---|---|---|
| live DOM rows for a 2000-track playlist | ~2000 `.song-row` nodes (+2000 `<img>`) | **23 rows at top, 33 mid-list** (viewport + overscan 10), recycled pool; scroll height preserved via spacer so scroll position never jumps | node driver `virtualize.js` with fake DOM: `virt.liveCount()`, `virt.range()`, spacer height, `scrollTop` untouched, per-node rebinding verified |
| virtualization threshold | — | lists ≤40 rows render directly (no virtualization overhead); search results (≤16) unaffected | `renderPlaylistTracks` in `app.js` |
| `content-visibility: auto` | absent | present on `.spl-virt-row` (`styles.css`) + `.spl-mem-moderate` hook | grep |
| click behavior | per-row `bindTap` | preserved exactly: one `bindTap` per pooled node + per-item `__splTapFn` swapped on recycle (`wireTrackTap`) | `virtualize.js` asserts every visible node is bound to the item at its index |

## C8 — search debounce

Worker 1 implemented it: `SearchOverlay.kt` `onInput` now debounces
**250 ms** (was 220 ms) before `doSearch`. Per the task ("do it only if not
already done") Worker 3 made no change here. The shared
`__splUx.debounce` helper is unit-tested (`debounce.js`: 5 rapid calls →
exactly 1 invocation ~250 ms after the last).

## D3 — dwell prefetch (LRU 20)

| | Before | After | How counted |
|---|---|---|---|
| prefetch on dwell | none | `mouseenter`/`focus` > **400 ms** on playlist/album/artist/show rows (search results, home cards, library rows) arms `__splUx.prefetch`; `mouseleave`/`blur`/navigation cancels | `prefetch.js` driver: arm→disarm fires 0 fetches; arm→dwell fires 1; `cancelAll` after navigation fires 0 |
| cache | none | **LRU cap 20** (`lru.js`: 21st insert evicts LRU victim, access refreshes) | unit tests |
| cache-hit path | playlist open always hit network | `openPlaylistDetail` serves from the prefetch LRU on hit (**0 ms network**); `prime()` also fills it | code path in `app.js` |
| hit logging | none | `prefetch.stats()` → `{hits, misses, size, inflight}`; hits/misses logged via `AndBridge.dbg` | `prefetch.js` asserts stats |

In-flight detail loads are aborted (`AbortController`) when another playlist
is opened — "cancel on navigation away".

## D5 — skeleton screens (exact geometry)

| | Before | After | How counted |
|---|---|---|---|
| Home (grid + sections) | mock content swapped for real data (count/layout changes → shift) | skeleton `recent-card` ×4 + `release-card` ×6 shown immediately, swapped for real rows | `skeleton.js` |
| Library (`#tabLibrary .song-list`) | static mock rows | skeleton `.song-row` ×8 → real library rows (Liked Songs + playlists + albums + artists, cap 60) via new `loadRealLibraryContent()` | `skeleton.js` |
| Playlist detail | single "Loading..." row (different geometry than `.song-row` list) | 8 skeleton `.song-row`s | `skeleton.js` (app.js skeleton ≡ library skeleton row) |
| geometry equality | — | **asserted**: skeleton vs final builders produce identical normalized class structures for `recent-card`, `release-card`, `song-row`; the `.spl-skel` shimmer CSS is asserted to contain **no box-model declarations** (no margin/padding/border/width/height/top/left) — `::after` overlay only | `UxPerfTest.skeletonGeometry_matchesFinalRows` |

Note: skeleton artwork uses `<div>` where final uses `<img>`; both get
identical geometry from the shared CSS classes (dimensions come from the
stylesheet, not the tag) — the test normalizes `img→div` and documents this.

## D4 — Service Worker spike → FALLBACK (documented)

**Outcome: NO — a Service Worker cannot be registered from the injected layer.**

Exact failure reasons (two independent blockers):
1. **The app deliberately neuters registration.** `WorkerNeutralize.kt`
   (document-start injection, before any post-login code) overrides
   `navigator.serviceWorker.register` with
   `() => Promise.reject(new Error('SW blocked by Spotilol'))` and unregisters
   existing registrations. Gated by the `BlockServiceWorker`
   SharedPreferences flag (default **true**). Any `register()` call from
   injected JS therefore rejects with `'SW blocked by Spotilol'`.
   A runtime probe is provided: `window.__splUx.swProbe()` → resolves
   `{ok:false, reason:'register rejected: SW blocked by Spotilol'}`.
2. **Even with the neutralizer off, injected JS has no same-origin script URL
   to register.** `register()` requires a script URL on the page's origin;
   serving `https://open.spotify.com/__spl-sw.js` would need
   `shouldInterceptRequest` in `SpotifyWebViewClient.kt` (not owned by
   Worker 3; flagged for the parent).

**Fallback implemented** (in-memory + disk LRU for artwork, injected layer):
`window.__splUx.artCache` — memory LRU (cap 60, object URLs) in front of an
IndexedDB LRU (cap 100 entries, timestamp-trimmed); `loadInto(img, url)` is
cache-first and never blocks render (direct `src` + background populate;
any failure degrades to plain `src`). Wired into all 10 artwork sites
(C6 table). Memory-pressure hook drops it (B3-JS).

## B3-JS — `window.__spotilol.onMemoryPressure(level)`

| Level | Behavior | How verified |
|---|---|---|
| `MODERATE` / `RUNNING_MODERATE` / `LOW` / `UI_HIDDEN` / `BACKGROUND` (or codes 5/10/20/40/60) | virtualized overscan 10→**2**, artwork memory cache cleared, prefetch dwell queue cancelled | `mempressure.js` |
| `CRITICAL` / `RUNNING_CRITICAL` / `COMPLETE` (or codes 15/80) | above + overscan **0**, artwork hidden via `.spl-mem-critical` CSS, `window.__splMinimalUi=true` (minimal player UI) | `mempressure.js` |
| unknown level | ignored, never crashes | `mempressure.js` |
| recovery | `window.__spotilol.clearMemoryPressure()` restores overscan 10, clears flags | `mempressure.js` |

Before: the hook did not exist (`grep __spotilol.onMemoryPressure` → 0 hits).
Native side calls it (Worker 2 owns the native `onTrimMemory` wiring).

## JS validity gate (E2)

`node --check` on JS extracted from every modified `.kt` + `app.js` — all pass:
- `PlaybackControls.kt`, `SpotilolPlayer.kt`, `CssHack.kt`, `SearchOverlay.kt`, `CustomUI.kt`, `assets/custom-ui/app.js`

## Unit tests (E3)

`app/src/test/java/com/project/lol/webview/UxPerfTest.kt` (+ `NodeRunner.kt`);
drivers in `app/src/test/node/drivers/`; local runner
`app/src/test/node/run-local.js` (no Gradle/JDK needed):

| Test | Driver | Asserts |
|---|---|---|
| optimisticRollback_commandFails_iconReverts | `optimistic.js` | keep/confirm/revert transitions, revert paints real icon, same-frame DOM paint, `__splPh` sync |
| virtualizedWindowing_visibleRangeAndRecycling | `virtualize.js` | exact ranges at top/middle/bottom/empty, 23 live rows for 2000 tracks, node rebinding, scroll preserved, pagination append |
| prefetchLru_evictionAndDwell | `lru.js` + `prefetch.js` | cap-20 eviction order, access refresh, dwell gating, cancel, hit/miss stats |
| debounceTiming_coalescesRapidInput | `debounce.js` | 5 keystrokes → 1 call ~250 ms after last |
| skeletonGeometry_matchesFinalRows | `skeleton.js` | class-structure equality ×3 lists, shimmer CSS box-model-free |
| onMemoryPressure_levelBehavior | `mempressure.js` | moderate/critical/numeric/unknown/clear |

Result: **ALL DRIVERS PASS** (7/7, node v24.20.0).
