# Stream C measurements — Worker 1 (JS computation)

**Date:** 2026-10-10 · **Branch:** `opt/webview-perf` · **Scope:** `PlayerCore.kt`,
`MainLoop.kt`, `AutoFeatures.kt`, `CssHack.kt` (warden parts), `DownloadButton.kt`,
`SearchOverlay.kt`, `CollectionDownload.kt`, `AndroidTracker.kt`,
`PlaybackControls.kt`, `PlaylistSort.kt`

Counting method: `grep -c` per file from
`app/src/main/java/com/project/lol/webview/injections/`, plus manual
classification of each hit (recurring vs one-shot, guarded vs unguarded).
BEFORE = pre-change code (recorded 2026-10-10 before edits); AFTER = after this
work. `node --check` run on JS extracted from every modified `.kt` (10/10 pass).

## C1 — Warden consolidation

| Metric | Before | After | How counted |
|---|---|---|---|
| Independently-scheduled recurring intervals | 9 | 3 | classified each `setInterval(` hit: before = floaters 250ms, pfint, afint, cssint, `__splMarkInt`, dlbtn 5s, search 2s, coldl tick 2s, PlaylistSort fallback 5s; after = floaters 250ms, `__splWarden` 5s, PlaylistSort fallback 5s |
| `__splWarden`-registered checks | 0 | 7 | `grep -c "__splWardenAdd("` per file: splPf, splAf, splCss, splCssMark, splDlBtn, splSearch, splColDl |
| Raw `setInterval(` call sites (10 files) | 14 | 15 | `grep -c "setInterval("` summed per file; +1 is the warden timer itself — fallbacks keep old call sites as text, so raw count is not the goal metric |

TrackObserver's 1s watchdog intentionally untouched (remount detection must stay
snappy); it is outside this worker's file set.

## C2 — AndroidTracker attributeFilter

| Metric | Before | After | How counted |
|---|---|---|---|
| Observers watching attributes **without** `attributeFilter` | 1 (AndroidTracker) | 0 | `grep -n "attributeFilter"` vs `attributes: true` in observe calls |
| Observers watching attributes **with** `attributeFilter` | 2 (PlaylistSort headerObserver, CssHack will-change) | 3 (+ AndroidTracker `['aria-checked','aria-label','href','src']`) | same |

ChildList-only observers (SearchOverlay navMo, CollectionDownload, CssHack
tippy) don't watch attributes — filter N/A.

## C3 — splShuffleBtn cache

| Metric | Before | After | How counted |
|---|---|---|---|
| Full-document `querySelectorAll('button')` scans per `splShuffleBtn()` call in steady state | 1 per call (called from 10 Hz `splUpdate` + `readTrackState`) | 0 — one `isConnected` check per call, re-resolve only if `!cached.isConnected` | code inspection of `PlaybackControls.kt` |

## C5 — Event-driven conversions

| Interval | Signal used / why it stays polling |
|---|---|
| SearchOverlay 2s `bindSearchIcon` | Converted: nav-bar MutationObserver (already in file) is the primary rebind signal; 5s warden check is fallback for icon swaps with no childList mutation on navBar |
| DownloadButton 5s scan | Converted: new MutationObserver on `aside[data-testid="now-playing-bar"]` (the actual container of the lyrics/queue anchor buttons — not `div[data-testid="action-bar-row"]`, which is the playlist-header bar) watching for added anchor buttons; 5s warden check is fallback |
| CollectionDownload 2s tick | `hijack()` already event-driven (body MutationObserver, 150ms debounce); warden covers `ensureButtons()`/`syncButtons()` — no DOM signal exists (they react to JS bridge flags `__splDlActive`/`__splDlBatch` and SPA route changes) |
| PlaylistSort 5s fallback | Stays polling (documented in code): observer covers DOM signals, but `decorate()` also depends on non-DOM state (`window.splPlaylistSort.set()` fires no mutation); bg-guarded |
| MainLoop bootIv 300ms | Stays (documented): one-shot boot loop, self-terminates ≤100 tries; no event signal for "page just loaded, wait for player mount" |
| MainLoop uIv 1s | Stays (documented): one-shot unlock-confirmation poll, ≤6 tries, self-clearing |
| PlaybackControls rfint 1s | Stays (documented): one-shot context-menu watch, self-clears on hit + 5s hard backstop |
| CollectionDownload loadAll iv 200ms | Stays (documented): one-shot per-download scroller, always `clearInterval`'d in `finish()` |
| AndroidTracker fallback 1s | Stays (documented): catch-path only, if `MutationObserver` construction throws |

## C8 — Search debounce

| Metric | Before | After | How counted |
|---|---|---|---|
| Debounce before Spotify API hit | 220 ms | 250 ms | `grep -n "doSearch(v); }, " SearchOverlay.kt` |

## B5-JS — background guards audit

"Guarded" = callback checks `window.__splBg` (or `visibilityState`) before work,
or is a self-terminating one-shot documented as covered by the native
`onPause()`/`pauseTimers()` path (stream B1, `MainActivity.kt` — not this
worker's files).

| Metric | Before | After | How counted |
|---|---|---|---|
| Recurring intervals **without** a background guard | 4 (MainLoop pfint, AutoFeatures afint, DownloadButton 5s, AndroidTracker fallback) | **0** | classified each recurring `setInterval`/rAF hit in the 10 files |
| `requestAnimationFrame` perpetual loops in the 10 files | 0 | 0 | `grep -n requestAnimationFrame` + context audit |
| `requestAnimationFrame` event-driven uses | 0 | 1 (PlaybackControls virtual-list scroll throttle: fires only on user scroll; rAF never fires when the page is hidden; B1-covered) | same |
| Transient one-shot intervals (documented, B1-covered) | 4 (bootIv, uIv, rfint, loadAll iv) | 4 — all now carry a `B5:` comment stating why no signal/guard | code inspection |

## Concurrent-change note (2026-10-10, during this work)

Another worker edited `PlaybackControls.kt` (and `SpotilolApp.kt`) on the same
branch while this stream ran: `actPlayPause`/`actSkipBack`/`actSkipForward` now
return booleans, and a `/*__SPLUX_START__*/` block (~420 lines: LRU, debounce,
virtual list with the scroll-throttle rAF above, service-worker probe) was
appended. That block adds no `setInterval`; its `setTimeout`s are one-shot
(debounce factory, dwell prefetch). None of it touches this stream's regions
(C3 cache, B5 rfint comment verified intact). `node --check` was re-run on the
final merged file: PASS.

## Interaction notes for other workers

- `MainActivity.onStop`/`onResume` (`__splWasPfint/Afint/Cssint` clear/restore)
  is now redundant but harmless: `pfint`/`afint`/`cssint` stay `null`, so the
  `typeof x !== 'undefined' && x` guards skip, and the warden resumes on its own
  when `__splBg=false`. Stream B1 may simplify that block.
- `window.__splWarden` is created once (`if(!window.__splWarden)`) and each part
  falls back to its old private interval if `window.__splWardenAdd` is missing,
  so injection-order changes can't break a part.
- `app/src/test/` did not exist; created with 5 test classes (JUnit4, added
  `testImplementation(libs.junit4)` + catalog entry — build wiring only).
  `.github/workflows/build-apk.yml` currently has no unit-test step; add
  `./gradlew testDebugUnitTest` to run them on Actions.
