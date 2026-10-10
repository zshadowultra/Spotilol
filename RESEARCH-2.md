# Spotilol Pre-render + Cache Research — Round 2 Findings (Worker 2)

**Date:** 2026-10-11 · **Method:** agent-reach CLI (`agent-reach doctor`; Exa/mcporter
backend not installed in this session — same gap as phase 1, fell back to web
search) + targeted web search on public sources. Social (X/Reddit) backends
unauthenticated — discussion coverage gap, flagged not fatal.

**Architecture facts that decide everything below** (verified in code, not assumed):
- Spotilol keeps ONE persistent WebView on `https://open.spotify.com` for the whole
  session. There are no document navigations to "the next screen".
- The custom UI (`assets/custom-ui/app.js`, 642 lines, injected post-login) is a
  SPA-ish shell: Home/Search/Library are `.tab-view` divs toggled by class
  (`switchTab`), their content built once and kept in the DOM. The ONLY view
  rebuilt per visit is the playlist/album detail (`openPlaylistDetail` clears
  `.song-list` and re-renders every time).
- Service Workers are deliberately neutered (`WorkerNeutralize.kt` overrides
  `navigator.serviceWorker.register` to reject; MEASUREMENTS-D) — skip SW entirely.
- All artwork flows through `__splUx.artCache` (memory LRU 60 → IndexedDB LRU 100),
  wired into all 10 injected `<img>` sites (MEASUREMENTS-D C6).

---

## 1. AndroidX WebKit `prerenderUrlAsync` — status 2026

**What:** engine-level off-screen page preparation without a visible WebView:
`WebViewCompat.prerenderUrlAsync(url, options, callback)` → callback fires
ready/timeout/error (~6–10 s timeout); cancel if unused. Mental model from the
in-the-wild writeup: request → readiness (ready ≠ fully loaded) → activation by
binding the prepared render to the visible WebView.

**Status:** graduated from experimental to stable in **AndroidX WebKit 1.15.0**
(commit 2025-07-18: "can now be used without the @OptIn annotation"). This repo
pins **webkit 1.13.0** (`gradle/libs.versions.toml`), so the API is not even on
the compile classpath today.

**Spotilol applicability:** NONE — architecture mismatch, two independent reasons:
1. There is no next-URL to prerender. The WebView sits on open.spotify.com for
   the whole session; "screens" are in-DOM tab toggles, not navigations.
2. A prerendered open.spotify.com would be a *fresh* page: no session cookies
   guaranteed, no two-phase injection, no audio element — activation would still
   need the full login + injection pipeline. The writeup itself warns: if the
   auth cookie changed, don't reuse the prerender.

**Decision: REJECT.** Not a version bump for its own sake; would add dependency
risk for zero applicable surface.

Sources:
- https://medium.com/@timkabor/speeding-up-webview-on-android-and-proving-it-with-numbers-37f6aa97dd06
- https://github.com/androidx/androidx/commit/e00185443dee9cd3a0b0027671554c16e6da6e5f

## 2. Speculation Rules API in WebView

**What:** `<script type="speculationrules">` with `prefetch` / `prerender` rules,
`eagerness: moderate` etc. On a prerendered navigation the next page feels
instant (rendered in a hidden tab).

**Status:** Chromium-only, not Baseline. Blink-dev intent threads confirm the
prerender experiments target all six Blink platforms *including Android
WebView* — but the API is explicitly a **multi-page-application technique**:
it targets full document navigations, not SPA route changes (2026 frontend-perf
roundup states this outright).

**Spotilol applicability:** NONE for navigation — same reason as §1: no document
navigations exist in the custom UI shell. The *subresource* prefetch variant
adds nothing over the existing dwell-prefetch LRU (which is gated on real user
intent, not link heuristics).

**Decision: REJECT** for prerender/prefetch rules. The *concept* (warm the
likely-next on idle, cancel on navigation away) is implemented manually instead:
dwell-prefetch LRU (§5) + the detail-view DOM snapshot (this round).

Sources:
- https://dev.to/shreysaraswatweb/frontend-performance-in-2026-the-techniques-that-actually-move-the-needle-now-445j
- https://groups.google.com/a/chromium.org/g/blink-dev/c/jG8vcT9bDGc/m/GP2J9mDtCQAJ

## 3. WebView HTTP cache tuning (cache modes)

**What:** `WebSettings.setCacheMode(...)`. The 2025-2026 tuning writeups converge:
`LOAD_DEFAULT` (honour HTTP cache headers) is the correct setting; the
measurable wins people report come from *server* cache headers
(`Cache-Control: immutable` on versioned assets), not from the mode flag —
one perf review measured the settings-only delta at **0–3% TTI** ("mostly
already-defaulted settings").

**Spotilol settings audit** (`MainActivity.createPlayerWebView`):
| Setting | Value | Verdict |
|---|---|---|
| `cacheMode` | `LOAD_DEFAULT` (explicit) | optimal — keep |
| `domStorageEnabled` | `true` | required (IndexedDB/localStorage) — keep |
| `mixedContentMode` | `MIXED_CONTENT_NEVER_ALLOW` | correct for https — keep |
| `allowFileAccess` / `allowContentAccess` | `false` | correct hardening — keep |
| `mediaPlaybackRequiresUserGesture` | `false` | required for autoplay — keep |
| `setOffscreenPreRaster` | pref-gated, default on | round-1 A3 — keep |
| Back-forward cache | enabled via `WebSettingsCompat` | keep |
| `setAppCacheEnabled` / `setDatabaseEnabled` | absent | correct — both deprecated/no-op; IndexedDB works without them |

**Decision: NO CODE CHANGE.** The audit is the deliverable — every tunable is
already at its recommended value, and the alternatives (`LOAD_CACHE_ELSE_NETWORK`
etc.) trade correctness for no measurable win. Documented, not implemented.

Sources:
- https://github.com/phoenix12383/spartancrm/blob/HEAD/perf-review/T5_webview_perf_tuning.md
- https://dev.to/kkibet/building-a-fully-featured-custom-webview-app-in-android-complete-guide-4lof

## 4. preconnect / dns-prefetch for the image CDN

**What:** `<link rel="preconnect" href="https://host">` does DNS + TCP + TLS
upfront; `<link rel="dns-prefetch">` does DNS only. Saves "hundreds of
milliseconds on third-party requests" (Jan 2026 resource-hints writeup).
Supported in WebView (Chromium engine). Two subtleties from the field:
- Pair every preconnect with a dns-prefetch fallback.
- CORS and non-CORS fetches use **separate connections** — artwork here is
  loaded both ways (`<img src>` = non-CORS; `artCache`'s `fetch(url,
  {mode:'cors'})` = CORS), so both variants of the hint are needed.

**Spotilol applicability:** HIGH. Artwork hosts are fixed and known:
`i.scdn.co` (API artwork, every row), `api.spotify.com` (prefetch/API JSON).
One-time `<link>` injection into the document head at custom-UI bootstrap,
idempotent, ~zero risk.

**Decision: IMPLEMENT** as `Ux.netHints()` in the `__splUx` block, called once
from `CustomUI.loadAndInject`. 5 links: i.scdn.co preconnect (+crossorigin) +
dns-prefetch; api.spotify.com preconnect + dns-prefetch.

Sources:
- https://medium.com/@linz07m/speed-up-web-page-using-resource-hints-9d169bb11412
- https://medium.com/expedia-group-tech/dns-prefetch-preconnect-7-tips-tricks-and-pitfalls-82d633c7f210

## 5. Image caching: deepening `__splUx.artCache`

**State of the art (2025-2026):** LRU is the consensus default for
temporal-locality workloads (artwork: same images recur across Home/Library/
playlist). IndexedDB blob caches in the wild add: (a) eviction on
`QuotaExceededError` (not just count caps), (b) true LRU via access-timestamp
refresh, (c) `hydrate()` before first paint so the first render takes the
cache path.

**Gaps in the current artCache** (code inspection):
1. `IDB_CAP=100` — small for a library with hundreds of distinct artworks.
   Timestamp-trim eviction exists and works; raising the cap keeps the same
   policy with more headroom (~250 entries × ~20 KB ≈ 5 MB, well within origin quota).
2. No **pre-warm**: the cache only fills for rendered images. The
   dwell-prefetch already establishes intent for a playlist's *track data*;
   its *artwork* is fetched again on open. Pre-warming the first N track
   artworks when the prefetch data lands closes the loop.
3. IDB eviction is insert-time LRU, not access-time: a hit doesn't refresh `ts`.
4. `idbPut` has no quota-error path — a full store silently stops caching.

**Decision: IMPLEMENT all four** (cap 100→250; `artCache.prime(url)` +
priming first 8 track artworks on prefetch-data arrival, idle-scheduled;
ts-refresh on IDB hit; quota-error → trim to half cap → retry once).
Memory cap stays 60 (object URLs are RAM; 60 is the right order).

Sources:
- https://github.com/capsize-games/uwuchat-oss/blob/HEAD/wiki/Client-Side-Caching.md
- https://github.com/lankajs/lanka/blob/HEAD/modules/blob-cache/GUIDE.md
- https://medium.com/@kishanhimself/caching-becomes-powerful-on-a-sudden

## 6. DOM snapshot / screenshot-cache for instant revisit

**What:** two variants in the literature. (a) *Screenshot cache*: capture the
WebView to a bitmap on a screen, show it in an ImageView on next visit while
the live page loads ("removes waiting from the user's mind"). (b) *DOM
snapshot*: keep the last-visited tab's rendered DOM (or its HTML) and restore
it on revisit instead of re-rendering.

**Spotilol applicability:** variant (a) does NOT fit — the WebView is never
hidden and screens are in-DOM tabs, not pages; there is no "next visit" to a
WebView. Variant (b) fits exactly one view: the playlist/album detail, the only
view rebuilt per visit (`openPlaylistDetail` clears `.song-list` → skeleton →
fetch/prefetch-hit → re-render). Home/Search/Library are class-toggled with
content kept in the DOM — snapshotting them buys nothing.

**Design (implemented):** after a successful non-virtualized detail render
(≤40 rows, no next-page), store `{uri, html: list.innerHTML, n}` in
`Ux.detailSnap*`. On `openPlaylistDetail` for the same uri with the prefetch
LRU still holding its data: restore `innerHTML` + rebind row taps by index
(the per-row tap closure is factored into `bindDetailTap`), skip skeleton +
re-render entirely. Single snapshot (last-visited), invalidated on memory
pressure (moderate+) and overwritten on any new successful render.
Event bindings don't survive `innerHTML` restore — hence the index rebind pass,
which is O(rows) listener attachments vs O(rows) full DOM construction.

**Decision: IMPLEMENT** (helpers in `__splUx` for testability; thin glue in
app.js).

Sources:
- https://medium.com/@surajkrj.2207/caching-android-webviews-the-smart-way-near-zero-load-time-ux-7d5c6ba07ebb
- RESEARCH.md §D2 (screenshot-cache variant analysis)

## 7. Critical-path deferral in the custom-ui bootstrap

**What:** 2026 guidance keeps converging on the same rule — defer or idle-schedule
everything not needed for first paint (`requestIdleCallback` with a
`setTimeout` fallback; Partytown-style worker offload for third-party scripts).

**app.js bootstrap audit** (642-line synchronous eval; what's before first paint):
| Item | Verdict |
|---|---|
| `syncInitialPlayer()` IIFE (player DOM sync) | KEEP sync — it IS first paint |
| `rebindNavTabs()` IIFE | KEEP sync — one querySelectorAll, needed before first tap |
| `bindAvatarDiagnostic()` IIFE + its 500 ms × 10 s attach-poll interval | DEFER — pure device-debug tooling, zero user value on the critical path |
| function definitions (`bindTap`, `switchTab`, builders) | cheap — keep |

**Decision: IMPLEMENT** — convert the diagnostic IIFE to a named function +
idle-scheduled invocation. CustomUI.kt's home/library `setTimeout` deferrals
(3–9 s) already cover the heavy content; this removes the last recurring timer
started synchronously at bootstrap.

## 8. Pre-rendering the next tab off-DOM — considered, not built

The task suggested pre-rendering the next tab's skeleton + data off-DOM on idle.
Against the actual architecture: Home/Library contents are built once and kept
in the DOM (class-toggled); the only per-visit build is the detail view, which
the DOM snapshot (§6) covers more cheaply than off-DOM pre-rendering (no
second layout tree, no node-adoption edge cases, bindings preserved by design).
**Rejected as redundant** — §6 is the same win with less machinery.

---

## What ships this round

| # | Item | Files |
|---|---|---|
| 1 | `Ux.netHints()`: preconnect (+crossorigin) / dns-prefetch for `i.scdn.co` + `api.spotify.com` | PlaybackControls.kt, CustomUI.kt (call) |
| 2 | artCache: IDB cap 100→250; `prime(url)`; idle-scheduled artwork pre-warm of first 8 tracks on prefetch-data arrival; ts-refresh on IDB hit; quota-error trim-and-retry | PlaybackControls.kt |
| 3 | Detail-view DOM snapshot: `Ux.detailSnap{Save,Get,Clear}` + app.js restore/rebind glue + `bindDetailTap` refactor | PlaybackControls.kt, app.js |
| 4 | Defer `bindAvatarDiagnostic` to idle | app.js |
| 5 | WebView settings audit | this doc only — no change (all optimal) |

## Deliberately not built

- `prerenderUrlAsync` (§1): wrong architecture + not on the pinned webkit version.
- Speculation Rules (§2): MPA-only; no document navigations exist.
- WebView cache-mode changes (§3): already optimal.
- Service Worker image cache: blocked by design (WorkerNeutralize) — per task, skipped.
- Off-DOM tab pre-render (§8): redundant with the DOM snapshot.
- Raising the *memory* LRU cap: object URLs are RAM; 60 stays.
