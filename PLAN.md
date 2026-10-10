# Spotilol Optimization Plan — WebView resource usage + near-native UX

**Date:** 2026-10-10 · **Dir:** `~/workspace/spotilol-opt` (fork `zshadowultra/Spotilol`, `main` @ `5f2c632`)
**Inputs:** `RESEARCH.md` (web research), `PERF_AUDIT.md` + `PERF_FIXES.md` (P0+P1 done 2026-10-03),
`~/workspace/research/spotilol/FINDINGS.md`
**Rule:** zero functionality change unless the item says otherwise. Every item ships with a
before/after measurement (counts, not vibes). Device-level numbers (PSS, INP) are
"verify on device" targets — this environment has no device; static instrumentation
+ unit tests + green Actions build are the gates here.

## Baseline inventory (measured 2026-10-10, before this plan)

| Metric | Value | How counted |
|---|---|---|
| Active `setInterval` in injected JS | 12 | grep `setInterval(` in `webview/injections/` |
| MutationObservers | 9 | grep `new MutationObserver` |
| Post-login payload | ~275 KB | PERF_AUDIT.md |
| Document-start payload | ~38 KB | PERF_AUDIT.md |
| WebView instances in MainActivity | 1 main + 2 transient (L722/L731) | grep `WebView(` |
| `:has()` selectors remaining | 0 (replaced w/ classes, P1 #4) | PERF_FIXES.md |

---

## Stream A — Reduce WebView overload (fewer/heavier WebViews, cheaper init)

### A1. Warm the Chromium engine at app start
**What:** create + destroy a throwaway WebView in `SpotilolApp.onCreate` so the
renderer process, native libs, and GPU init happen before the user opens the player.
**Measurable:** cold-start time from `MainActivity.onCreate` to first
`onPageFinished` — target −30% on warm-engine path (measure via logcat timestamps
on device; here: assert the warmup call exists and runs before any UI WebView).
**Applies to:** `SpotilolApp.kt`.

### A2. Audit + budget the two transient WebViews (MainActivity L722, L731)
**What:** identify what L722/L731 WebViews are for; ensure each is removed from its
parent and `destroy()`ed immediately after use; never let a second renderer live
alongside the main one. Add a debug assertion: >1 live WebView logs a warning.
**Measurable:** peak live WebView count during auth/share flows: 2 → 1 after
transient teardown (static: every `WebView(` construction site has a matching
`destroy()` on all paths — verified by code inspection + unit test on the helper).

### A3. `setOffscreenPreRaster(true)` for long lists — experiment with kill switch
**What:** enable on the main WebView settings; measure PSS delta. If memory-bound,
revert — the flag trades GPU memory for scroll smoothness.
**Measurable:** scroll jank on a 1k-track playlist (device); here: flag present,
gated by a `WebViewTuning` pref so it can be A/B'd.

## Stream B — Reduce background WebView usage (the big battery lever)

### B1. `webView.onPause()` + `WebView.pauseTimers()` on background
**What:** in `MainActivity.onPause` (currently only `onStop` clears some intervals),
call `webView.onPause()` and `WebView.pauseTimers()`; resume both in `onResume`.
The existing `__splBg` JS flag stays as the in-page complement.
**Measurable:** background JS timer wakeups → 0 while backgrounded (static: no
`setInterval`/`rAF` can fire — verified by the pause covering all WebViews);
foreground resume restores all loops (existing `__splWas*` restore path extended
to cover every interval in the inventory).
**Risk (must verify on device):** `<audio>` must keep playing while paused —
Chromium exempts audible renderers (RESEARCH.md B2), but System WebView versions
vary. The media-notification service keeps the foreground service alive regardless.
If audio stalls on any target WebView version, fall back to `__splBg`-only mode
behind a pref.

### B2. `setRendererPriorityPolicy(RENDERER_PRIORITY_BOUND, true)` on background
**What:** let the OS reclaim the renderer under memory pressure when backgrounded;
restore to `RENDERER_PRIORITY_IMPORTANT` on foreground.
**Measurable:** renderer process priority transitions logged; no functional change
while foregrounded.

### B3. `onTrimMemory` → staged shedding (native + JS bridge)
**What:** `ComponentActivity.onTrimMemory`: at `TRIM_MEMORY_UI_HIDDEN`+ drop
`webView.clearCache(false)` (RAM only); at MODERATE+ also notify JS via
`window.__spotilol.onMemoryPressure(level)` → injected JS shrinks virtualized-list
overscan, drops in-memory artwork cache, cancels prefetch queues.
**Measurable:** trim levels handled: 0 → 4+ distinct levels; JS hook present and
wired in the injection bootstrap.

### B4. `onRenderProcessGone` recovery
**What:** handle renderer death: return `true`, destroy the dead WebView, create a
new one, re-run the two-phase injection, re-attach audio/queue state from the
native playback state of record.
**Measurable:** simulated renderer kill (device: `adb shell am kill` the renderer)
recovers to playing state without app restart — here: handler exists, re-attach
path unit-tested with a fake WebViewClient.

### B5. Extend `__splBg` guards to every interval/observer missing one
**What:** P2 #9 follow-through — audit all 12 intervals + 9 observers; every one
either stops on `__splBg`/visibilitychange or is on the `onPause`/`pauseTimers`
path (B1). No JS thread wakeups while backgrounded.
**Measurable:** intervals without a background guard: count → 0.

## Stream C — Reduce computation (JS thread)

### C1. Consolidate the warden intervals (PERF_AUDIT P2 #9)
**What:** single `window.__splWarden` 5s interval in `PlayerCore.kt` calling
registered check functions (registry pattern like `__splFloaters`). Keep
TrackObserver's 1s watchdog (remount needs to be snappy). Migrate: MainLoop pfint,
AutoFeatures afint, CssHack cssint, DownloadButton, SearchOverlay, CollectionDownload tick.
**Measurable:** independent waking intervals: 7 → 2 (warden + watchdog). Renderer
idle gaps appear between 5s marks (verifiable in a trace).

### C2. `attributeFilter` on AndroidTracker observer (P2 #10)
**What:** add `attributeFilter: ['aria-checked','aria-label','href','src']` —
the only attributes `readTrackState` reads.
**Measurable:** observer callback fires per React re-render class-toggle → only on
meaningful attribute changes (static: filter present; device: callback count
during a 60s playlist scroll).

### C3. Cache `splShuffleBtn` result (P2 #11)
**What:** cache the found button; re-resolve only if `!cached.isConnected`.
**Measurable:** full-document `querySelectorAll('button')` scans per minute → 0
in steady state.

### C4. Split the 275 KB post-login payload: critical vs deferred (P2 #12)
**What:** inject player-core set immediately; defer downloads UI, PlaylistSort,
SearchOverlay via `requestIdleCallback` (fallback `setTimeout 3000`) or lazily on
first use (PlaylistSort on first playlist nav, SearchOverlay on first search tap).
Keep the call graph intact — only delay the `evaluateJavascript` chunks.
**Measurable:** bytes parsed+compiled before time-to-interactive: ~275 KB → ~150 KB
(counted from the chunked payload sizes); deferred chunks load on idle/first-use.

### C5. Event-driven conversions for remaining polling intervals
**What:** audit each remaining `setInterval`: convert where a DOM/event signal
exists (e.g. SearchOverlay's 2s `bindSearchIcon` → MutationObserver on nav bar;
DownloadButton's 5s scan → observer on action-bar container). Where no signal
exists, document why the interval stays.
**Measurable:** polling intervals remaining with an available event signal: → 0;
each conversion notes the signal used.

### C6. IntersectionObserver + `loading="lazy"` for artwork (RESEARCH.md C3)
**What:** in the injected library/playlist/search list renderers, add
`loading="lazy" decoding="async"` to artwork `<img>`; use IntersectionObserver
for infinite-scroll pagination instead of scroll handlers.
**Measurable:** offscreen image decodes during a fast scroll: → ~0 (device);
static: attributes present on all injected `<img>` creation sites.

### C7. Virtualized lists for 1k+ track lists (RESEARCH.md C4)
**What:** window the injected Liked Songs / playlist / search-result lists:
render viewport + overscan rows only, recycle row nodes, `content-visibility:auto`
as the CSS assist. This is the DOM-node-count lever (memory).
**Measurable:** DOM nodes for a 2,000-track playlist: ~2,000 rows → ~40 live rows
(counted via `document.querySelectorAll` in a test harness page); scroll position
and item click behavior unchanged (unit-tested).

### C8. Debounce search input 250 ms (RESEARCH.md C2)
**What:** search-as-you-type waits 250 ms after last keystroke before hitting the
Spotify API.
**Measurable:** API requests per 10-char query: ~10 → 1–2.

## Stream D — Near-native UX speed

### D1. Optimistic play/pause + skip (RESEARCH.md D1, Doherty ~400 ms)
**What:** toggle the play/pause icon and progress state instantly on tap; reconcile
with the real player state in the background. Design the rollback first: if the
underlying Spotify command fails, revert the icon and flash the error toast.
**Measurable:** perceived tap→icon-change latency: next-frame (<50 ms) vs today
(round-trip to Spotify web player, often 200–600 ms). Rollback path unit-tested.

### D2. `touch-action: manipulation` + passive listeners (RESEARCH.md D3)
**What:** add `touch-action: manipulation` to the injected root (kills the 300 ms
tap delay where it still applies); audit all `addEventListener('touchstart'/
'touchmove'/'wheel')` for `{passive:true}`.
**Measurable:** non-passive touch/scroll listeners in injected JS: → 0;
tap→response on nav/play controls (device INP < 200 ms target).

### D3. Prefetch likely-next screen data (RESEARCH.md D4)
**What:** when the user dwells on a playlist row (>400 ms hover/focus) or opens
an album, prefetch its track list via the Spotify API into an LRU cache (cap 20
entries); cancel on navigation away.
**Measurable:** playlist-open data wait: cache hit → 0 ms network (hit rate
logged); prefetch never fires for items not dwelled on (no wasted requests).

### D4. Service-Worker image cache, stale-while-revalidate (RESEARCH.md D5)
**What:** register a Service Worker in the WebView (verify `navigator.serviceWorker`
is available in this WebView context — SPIKE FIRST); cache-first for artwork,
SWR for API JSON. Same artwork recurs across Home/Library/playlist.
**Measurable:** repeat artwork downloads across 3 screen visits: → ~0 (device
network log); if SW registration fails in the WebView, fall back to an
in-memory + disk LRU in native code and document the outcome.

### D5. Skeleton screens matching final geometry (RESEARCH.md D2)
**What:** for Home and Library, render shimmer skeletons with the exact row
geometry before data arrives; swap to live content without layout shift.
**Measurable:** cumulative layout shift on Home load → ~0 (skeleton geometry ==
final geometry, asserted by a unit test comparing rects).

## Stream E — Measurement + gates (not optional)

### E1. Before/after instrumentation counts
Each stream's "Measurable" is recorded in `MEASUREMENTS.md` (before column filled
from the baseline inventory above; after column filled by the implementing worker
with the counting method used). No item closes without both columns.

### E2. JS validity gate
Every modified `.kt` injection file: JS extracted from the Kotlin raw strings and
`node --check` validated, exactly like PERF_FIXES.md did.

### E3. Unit tests
New behavior gets unit tests: warden registry, payload chunking order, optimistic
rollback, virtualized-list windowing math, `onMemoryPressure` hook, renderer-gone
re-attach path (fakes), prefetch LRU. Tests must exercise functionality, not assert
`true`.

### E4. Build gate
`./gradlew assembleDebug` equivalent via GitHub Actions (no local JDK here).
Workflow `.github/workflows/build-apk.yml` already exists; push the work branch,
`gh workflow run`, poll to green. Firebase already stripped (no google-services
plugin) — no blocker.

## Out of scope (documented, not forgotten)
- WebView-per-screen or multi-process split (A7): too invasive for this pass;
  revisit only if renderer kills prove frequent.
- `WebViewCompat.addDocumentStartJavaScript` already used — no change.
- WhatsApp's rumored Q2-2026 Windows optimization patch: re-check when it ships;
  not applicable to Android WebView anyway.

## Round 2 (2026-10-11)

Three parallel workstreams, each on its own branch from `opt/webview-perf`:

1. **Solar icons** (`opt/solar-icons`) — 99/101 custom-UI inline SVGs
   replaced with official Solar icons (480 Design, CC BY 4.0), Linear style,
   sourced from Iconify `@iconify-json/solar`; 2 brand glyphs (Instagram,
   WhatsApp) deliberately left alone. Mapping + attribution in
   `ICON-MAPPING.md`. Verified: XML parse 101/101, node --check, 9/9 node
   drivers, before/after screenshots + 75-icon gallery inspected.
2. **Pre-render / cache** (`opt/prerender-cache`) — research done
   (`RESEARCH-2.md`, 2026-10-11): `WebViewCompat.prerenderUrlAsync`,
   Speculation Rules, HTTP cache tuning, preconnect/dns-prefetch for the
   image CDN, deepening `__splUx.artCache`, DOM-snapshot revisit cache,
   custom-ui bootstrap deferral. Key architectural finding: the app is a
   single persistent WebView SPA (no document navigations), so only the
   playlist-detail view is rebuilt per visit — that bounds what pre-render
   can buy. Implementation status: branch created, not yet pushed.
3. **Honest speed audit + timing instrumentation** (`opt/timing-audit`) —
   this branch. Two deliverables:
   - `MEASUREMENTS-E.md`: every round-1 headline claim classified as
     PROVEN / INFERRED / DEVICE-ONLY, with explicit corrections
     (C4 "~150 KB" not met — real split 330 KB -> 231 KB; C1 "7 -> 2"
     was 9 -> 3; D1 timing numbers unmeasured; C8 extrapolation;
     B1/B4/D3 qualified). Genuine counted wins preserved (byte counts,
     interval counts, 23/33 virtualized rows, debounce, LRU, rollback).
   - In-app `performance.now()` instrumentation (`window.__splPerf`,
     `PerfMarks.kt`) bridged to logcat with ONE tag — `spotilol.perf`
     (`adb logcat -s spotilol.perf:I`) — covering docstart/core/deferred
     eval, custom-ui first paint, tap->icon-paint delta, renderPlaylistTracks
     duration, and home/library/search/playlist screen-open -> paint.
     Node driver `perf.js` (8/8 green); `node --check` green on all
     touched JS; `UxPerfTest` +1 test.

## Suggested build order
1. C1–C3 (one-file JS wins, same shape as P0/P1) + B5 (guards audit)
2. B1–B3 (native lifecycle — biggest battery lever, needs the most care)
3. C4–C6, C8 (payload + lists + search)
4. D1–D3, D5 (UX speed)
5. B4, D4 (renderer-gone, SW spike — highest risk, last)
6. E1–E4 throughout (measurements as you go, not at the end)
