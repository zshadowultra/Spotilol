# MEASUREMENTS-E — Honest speed audit + timing instrumentation (round 2)

**Date:** 2026-10-11 · **Branch:** `opt/timing-audit` (from `opt/webview-perf` @ `461d32d`)
**Question asked:** "is the speed really increased"
**Short answer:** the structural wins are real and counted (fewer intervals,
smaller critical payload, virtualized rows, byte counts). **No wall-clock
speedup was ever measured** — every timing number in round 1 (the "<16 ms",
"200-600 ms", "-30%", "wakeups to 0") is inferred or device-only. This doc
classifies every headline claim honestly, corrects the overstatements, and
ships the instrumentation that lets the user capture real numbers on their
phone.

---

## Part 1 — Claim-by-claim audit

Classification key:

- **PROVEN** — verified by a test, a node driver, or a stated counting method
  (grep counts, byte sums, behavioral assertions). Static counts are still
  PROVEN when the counting method is deterministic (e.g. byte sums); they
  prove structure, not speed.
- **INFERRED** — follows from code-path reasoning but was never timed or
  measured. The mechanism may be real; the number is not.
- **DEVICE-ONLY** — can only be established on a real phone (INP, PSS,
  cold-start ms, battery drain, background-audio behavior).

### PLAN.md targets x round-1 reports

| # | Headline claim (as written) | Class | What was actually shown | Honest restatement |
|---|---|---|---|---|
| A1 | cold-start -30% on warm-engine path | **DEVICE-ONLY** | Warmup call exists in `SpotilolApp.onCreate` (code inspection). | Warmup is wired; the -30% was a target, never measured. Needs logcat timestamps (`chromium engine warmed` vs first `onPageFinished`) on device. |
| A2 | peak live WebViews 2 -> 1 during cache-clear flows | **PROVEN** (static) | 3 `WebView(` sites audited; both transient sites guarantee `destroy()` via try/finally; `WebViewBudget` counter + `>1` warning; 7 unit tests. | Logic correct by code-path inspection + budget counter exists. No runtime peak ever observed. |
| A3 | scroll-jank improvement via `setOffscreenPreRaster` | n/a (no claim made) | Flag present, pref-gated (`WebViewTuning/offscreen_pre_raster`). | No speed claim was made; the flag is an experiment with a kill switch. Scroll jank needs a device trace. |
| B1 | background JS timer wakeups -> 0 | **INFERRED** | `onPause`/`pauseTimers` + symmetric resume; 4 unit tests on ordering/symmetry. | `pauseTimers()` pausing all timers is documented Chromium behavior, so "0 wakeups" is a sound inference — but never verified (needs battery stats / trace on device). Foreground restore unit-tested with fakes, not observed. |
| B1 | audio keeps playing under `onPause`/`pauseTimers` | **DEVICE-ONLY** | Documented risk + `WebViewNativePause` escape hatch. | Explicitly unverified. If audio stalls on the user's WebView version, flip `WebViewNativePause=false`. |
| B2 | renderer priority transitions; no functional change | **PROVEN** (static) | Transitions in `onStop`/`onResume`, API-26-guarded. | Wiring exists; "no functional change" is code reasoning (fair). Transitions never observed in logcat. |
| B3 | trim levels 0 -> 4+; JS hook wired | **PROVEN** | `TrimMemoryPolicyTest` (5 tests); stub in document-start bootstrap. | PROVEN. |
| B4 | renderer kill recovers to playing state without restart | **INFERRED** | 6-step `RendererRecoveryCoordinator`, 5 tests with a fake view. | Sequence correct against fakes; a real renderer death was never triggered and audio/queue re-attach is unobserved. |
| B5 | intervals without a background guard -> 0 | **PROVEN** (static) | Per-interval classification table, 4 -> 0. | PROVEN as a code audit. "No wakeups while backgrounded" inherits B1's INFERRED status. |
| C1 | independently-scheduled intervals 7 -> 2 | **PROVEN** (counts; corrected) | `__splWardenAdd` 7 registrations; raw `setInterval(` 14 -> 15 (the +1 is the warden itself). | **Correction:** PLAN said 7 -> 2, but the real baseline was 9, so the honest number is **9 -> 3** (warden + floaters 250 ms + PlaylistSort fallback 5 s; watchdog untouched by design). "Idle gaps appear between 5 s marks (verifiable in a trace)" — no trace taken: **DEVICE-ONLY**. |
| C2 | observer callback reduction (60 s scroll) | **PROVEN** (static) / **DEVICE-ONLY** (count) | `attributeFilter` present on AndroidTracker. | Filter exists (PROVEN); the callback-count reduction never measured (DEVICE-ONLY). |
| C3 | full-document button scans -> 0/min steady state | **PROVEN** (static) | Cache + `isConnected` re-resolve; code inspection. | PROVEN as a code path. |
| C4 | payload ~275 KB -> ~150 KB before TTI | **PROVEN** (bytes; target **not met**) | Core **~231 KB**, deferred **~99 KB (30.1%)** by UTF-8 byte sums; lossless partition + order unit-tested (10 tests). | **Correction:** baseline re-measured at **~330 KB**, not 275 KB, and the deferred set is fixed by scope at 99 KB — real split is 330 KB -> 231 KB immediate, not -> 150 KB. Byte counts PROVEN; the win is 99 KB of parse+compile off the critical path. Any TTI *time* implication is **DEVICE-ONLY**. |
| C5 | polling intervals with an available event signal -> 0 | **PROVEN** (static) | Per-interval table; conversions + documented keep-polling reasons. | PROVEN as a code audit. |
| C6 | offscreen image decodes -> ~0 during fast scroll | **PROVEN** (static) / **DEVICE-ONLY** (decodes) | `loading="lazy" decoding="async"` on 10/10 injected `<img>` sites; IntersectionObserver pagination. | Attributes present (PROVEN); decode behavior unmeasured (DEVICE-ONLY). |
| C7 | 2,000-track playlist: ~2,000 rows -> ~40 live rows | **PROVEN** | Node driver with fake DOM: **23 rows at top, 33 mid-list**; recycling + scroll preserved + click rebinding asserted. | PROVEN — a real, counted win. |
| C8 | API requests per 10-char query ~10 -> 1-2 | **INFERRED** (behavior PROVEN) | Driver: **5 rapid keystrokes -> exactly 1 call ~250 ms after the last**. | **Correction:** the driver exercised 5 keystrokes -> 1 call, not a 10-char query. "1-2 per 10-char query" is extrapolated from the same mechanism. Also note the debounce got *longer* (220 -> 250 ms) — the win is coalescing, not the 30 ms. |
| D1 | tap->icon <50 ms (next frame, <16 ms) vs 200-600 ms round-trip; worst case ~1.1 s | **INFERRED** | Icon DOM mutation happens synchronously in the tap handler (`splPaintPlayIcon`); node driver asserts same-frame DOM paint + `__splPh` sync; rollback lease/confirm/revert behaviorally tested. | **Correction:** the mechanism is real and the rollback logic is PROVEN, but **none of the numbers were measured**: "<16 ms"/"next-frame"/"<50 ms" are frame-budget assumptions (paint never timed), the "200-600 ms" baseline round-trip is a typical-value estimate (never measured on this app), and "~1.1 s worst case" is arithmetic on unmeasured components. The new `tap-play-icon` mark (Part 2) exists precisely to replace this inference with a measurement. |
| D2 | INP < 200 ms on nav/play controls | **DEVICE-ONLY** | `touch-action: manipulation` present; 0 non-passive touch/scroll/wheel listeners in owned files (1 documented keeper). | Static wiring PROVEN; the INP number is a target, never measured. |
| D3 | playlist-open data wait: cache hit -> 0 ms network | **PROVEN** (branch) / **INFERRED** (open latency) | Prefetch LRU cap 20, dwell gating, abort-on-navigate, hit/miss stats — all driver-tested; hit path issues zero fetches. | **Correction:** "0 ms network" is literally true of the cache-hit branch (no fetch issued — PROVEN), but user-perceived playlist-open latency (render + paint) was never measured. The `screen-playlist-paint` mark (Part 2) closes this. |
| D4 | repeat artwork downloads -> ~0 across 3 visits | **DEVICE-ONLY** | SW registration proven impossible (2 independent blockers); in-memory + IndexedDB LRU fallback wired into 10/10 artwork sites. | Fallback exists (PROVEN); the download-reduction claim needs a device network log. |
| D5 | CLS -> ~0 on Home/Library/playlist | **PROVEN** (static) / **DEVICE-ONLY** (CLS) | Skeleton vs final builders produce identical normalized class structures x3 lists; shimmer CSS asserted box-model-free. | Geometry equality PROVEN; the actual CLS number needs a device trace. |

### Overstatements corrected (explicit)

1. **C4 "-> ~150 KB"** — not achieved. Baseline was ~330 KB (not 275 KB);
   the scope-fixed deferred set is 99 KB, so immediate = ~231 KB. The honest
   win: 99 KB / 30.1% of parse+compile off the critical path, lossless,
   order-tested.
2. **C1 "7 -> 2"** — baseline miscount; honest number is **9 -> 3**
   (already corrected in MEASUREMENTS-C; repeated here so the headline
   doesn't travel without the correction).
3. **D1 "<16 ms" / "<50 ms" / "200-600 ms" / "~1.1 s"** — none measured.
   Honest: icon paints synchronously in the tap handler; paint latency
   unmeasured until the user runs the new instrumentation below.
4. **C8 "10-char query -> 1-2"** — driver proved 5 keystrokes -> 1 call; the
   10-char phrasing is extrapolation.
5. **B1 "wakeups -> 0 by construction"** — sound inference from a documented
   API, but unverified; audio-continuation is the open risk.
6. **B4 "recovers without restart"** — true against fakes; no real renderer
   kill was ever exercised.
7. **D3 "0 ms"** — true of the network branch only; open latency unmeasured.

### What is genuinely PROVEN (don't let the audit bury the wins)

- Byte counts: 231 KB core / 99 KB deferred (30.1%), lossless partition,
  core-before-deferred order — unit-tested.
- Interval counts: 9 -> 3 recurring; 7 warden registrations; 4 -> 0 unguarded.
- Row counts: 23/33 live rows for a 2,000-track playlist (fake-DOM driver,
  exact).
- 10/10 artwork sites lazy + cache-first; LRU cap-20 eviction order;
  debounce 5 -> 1; prefetch dwell/cancel/hit-miss stats.
- Guard/filter/attribute wiring across C2, C3, C5, D2, D5 (grep-counted).
- Optimistic rollback state machine (keep/confirm/revert + toast) —
  behaviorally tested.

---

## Part 2 — Timing instrumentation (so the user can measure)

`window.__splPerf`: a tiny mark/measure helper (new
`webview/injections/PerfMarks.kt`), defined by the document-start payload so
it exists before every other injected script. Marks live in memory; only the
key events emit one line to logcat. Default ON (user-requested), near-zero
cost: a `performance.now()` call and an array push per mark. Every use site
is guarded, so feature files also run without the helper (node drivers).

### Bridge -> logcat (ONE consistent tag)

Injected JS calls `AndBridge.perfMark(line)`. The existing `AndBridge.dbg`
route was deliberately **not** reused as-is: it goes through `Logger.js()`
(tag `js`), and — critically — `dbg()` returns early when the `Logging`
pref is off, and `Logger.log()` drops anything below WARN when disabled.
Timing marks must reach logcat even with the in-app log viewer off, so:

- `SpotifyBridge.perfMark(msg)` (`@JavascriptInterface`) ->
  `Logger.perf(msg)` -> `android.util.Log.i("spotilol.perf", msg)`
  unconditionally (never touches the 2000-entry ring buffer, so no buffer
  churn from measurement). The lines are fixed-format `name <n>ms`
  numerics — no user content — so bypassing the Logging gate is safe.

**Exact logcat tag: `spotilol.perf`** (INFO). Capture with:

```
adb logcat -s spotilol.perf:I
```

(Useful companion: `adb logcat -s spotilol.perf:I | tee perf.txt` to save
a session.)

### Events emitted

Each line is `[spl-perf] <event> <n>ms` (or with `rows=`/`virt=` extras).
Typical session emits ~15 lines, not per-frame spam.

| Event | Where | Meaning |
|---|---|---|
| `docstart-eval` | document-start payload begin/end (`SpotifyWebViewClient.buildEarlyJs`) | JS parse+eval time of the whole document-start injection |
| `core-eval` | post-login core chunk (`injectSplitPayload`) | eval time of the immediate (time-to-interactive) chunk |
| `deferred-eval` | deferred scheduler (`buildDeferredSchedulerJs`) | eval time of the idle-deferred chunk (SearchOverlay/PlaylistSort/downloads) |
| `customui-first-paint` | `assets/custom-ui/app.js` end (rAF) | script eval -> first paint of the custom UI |
| `tap-play-icon` | `splOptPlay()` in `SpotilolPlayer.kt` | tap-handler entry -> rAF after the optimistic icon paint |
| `renderPlaylistTracks rows=<n> virt=<0/1>` | `app.js renderPlaylistTracks` (try/finally, so early returns count too) | DOM build time for the track list |
| `screen-Home-paint` / `screen-Search-paint` / `screen-Library-paint` | `app.js switchTab` (rAF) | tab tap -> content painted |
| `screen-playlist-paint` | `app.js openPlaylistDetail` -> `renderDetail` (rAF) | playlist open -> tracks painted |
| `screen-home-content` | `CustomUI.kt loadRealHomeContent` | home open -> real (non-skeleton) content built |
| `screen-library-content` | `CustomUI.kt loadRealLibraryContent` | library open -> real rows built |

### How to use (on the phone)

1. Install the round-2 APK, open the app, log in.
2. Run `adb logcat -s spotilol.perf:I` on the connected machine.
3. Use the app normally: open Home/Library/Search tabs, open a playlist,
   tap play/pause a few times.
4. Each line is self-explanatory, e.g.
   `I/spotilol.perf: [spl-perf] tap-play-icon 8.4ms`.

### Honest limitations of the instrumentation itself

- `rAF` callbacks run **before** the frame's paint, not after — so
  `tap-play-icon` / `*-paint` measure tap -> *the frame that will show the
  change*, excluding rasterization/composition. It is the closest cheap
  proxy, not a photodiode.
- `performance.now()` origin is navigation start; deltas within one page
  load are comparable, absolute values across navigations are not.
- The bridge is async (`@JavascriptInterface` posts to the Java side), so
  logcat ordering between two marks <1 ms apart can jitter; deltas are
  computed in JS before bridging and are exact.

---

## Part 3 — Verification (this branch)

- `node app/src/test/node/run-local.js` — **8/8 drivers PASS** (7 existing +
  new `perf.js`: marks fire in order, deltas are numbers >= 0, emit format
  `[spl-perf] <name> <n>ms`, unknown mark -> -1 with no line, tap path via
  rAF, `performance.now` fallback to `Date.now()`, every emit bridged
  exactly once).
- `node --check` on JS extracted from every modified file — **all pass**:
  `PerfMarks.kt`, `SpotilolPlayer.kt`, `CustomUI.kt` (CONTENT extraction),
  `buildDeferredSchedulerJs` template (with `$quoted` stubbed),
  the docstart/core one-liners, and `assets/custom-ui/app.js`.
- JUnit: `UxPerfTest.perfMarks_fireInOrderWithNonNegativeDeltas` added
  (runs `perf.js` through `NodeRunner`; `NodeRunner`/`run-local.js` now
  extract `PerfMarks.kt` and substitute `__PERF__`; `__SPLUX__`
  substitution made conditional so drivers without it don't fail).
- Existing `PayloadChunkingTest` assertions re-checked against the new
  core-wrap: still 2 evaluate calls, core first with `/*CORE*/`+`/*THEME*/`,
  no `requestIdleCallback` in the first call, `spl-np-show` preserved in
  original mode; scheduler-shape assertions (`requestIdleCallback`,
  `{timeout: 3000}`, `setTimeout(run, 3000)`, `(0,eval)(src)`) unaffected.
- Kotlin compile: no local JDK — same gate as round 1 (GitHub Actions).
  Balance-checked all touched files; the one brace/paren imbalance in
  `NodeRunner.kt` is pre-existing (string literals), verified identical
  before/after.

### Files changed (this branch)

- NEW `app/src/main/java/com/project/lol/webview/injections/PerfMarks.kt`
- `app/src/main/java/com/project/lol/util/Logger.kt` — `Logger.perf()`
- `app/src/main/java/com/project/lol/bridge/SpotifyBridge.kt` — `perfMark()`
- `app/src/main/java/com/project/lol/webview/SpotifyWebViewClient.kt` —
  docstart begin/end parts, core-chunk wrap, deferred-scheduler marks
- `app/src/main/java/com/project/lol/webview/injections/SpotilolPlayer.kt` —
  `splOptPlay` tap -> icon-paint delta
- `app/src/main/assets/custom-ui/app.js` — js-begin mark, first-paint rAF,
  `__splPaintEmit` helper, switchTab / openPlaylistDetail / renderDetail /
  renderPlaylistTracks timing
- `app/src/main/java/com/project/lol/webview/injections/CustomUI.kt` —
  home/library content timing
- `app/src/test/node/drivers/perf.js` (NEW), `run-local.js`,
  `app/src/test/java/com/project/lol/webview/NodeRunner.kt`,
  `app/src/test/java/com/project/lol/webview/UxPerfTest.kt`
- `MEASUREMENTS-E.md` (this file), `PLAN.md` (Round 2 addendum)
