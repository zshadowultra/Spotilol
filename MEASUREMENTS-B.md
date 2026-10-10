# MEASUREMENTS-B — Worker 2 (native WebView lifecycle), opt/webview-perf

Date: 2026-10-10. Every number below states its counting method (PLAN.md E1).
Device-level numbers (PSS, cold-start ms, battery) are "verify on device" —
this environment has no device; static instrumentation + unit tests are the
gates here.

## A1 — Warm the Chromium engine

| | Before | After |
|---|---|---|
| Warmup call in `SpotilolApp.onCreate` | absent | present: throwaway `WebView` created + destroyed on a dedicated `HandlerThread` (`wv-warmup`), best-effort (all failures logged + swallowed) |

Counting: code inspection of `SpotilolApp.kt`. Runtime proof on device: logcat
`chromium engine warmed (throwaway webview created+destroyed)` before the first
`MainActivity` WebView creation.

## A2 — Transient WebView audit + budget

The two transient `WebView(...)` sites in `MainActivity`:

| Site | Purpose | Before | After |
|---|---|---|---|
| `clearWebViewCache()` (~L722) | Settings "clear cache": evict shared Chromium HTTP cache + history | `destroy()` called, but NOT exception-safe (a throw in `clearCache` skipped `destroy`); always spawned a 2nd renderer | prefers the live main WebView (shared-cache state needs no instance) → peak stays 1; throwaway fallback only when main is gone, `destroy()` guaranteed via `useTransientWebView` (try/finally incl. `create()` exceptions) |
| `clearAllData()` (~L731) | Settings "clear all data": cache + history + form data, then cookies/storage wipe + relaunch | same as above | same pattern as above |

| Metric | Before | After | Counting method |
|---|---|---|---|
| Construction sites with guaranteed `destroy()` on ALL paths (incl. exceptions) | 0 / 2 | 2 / 2 | `grep -n "WebView(" MainActivity.kt` → 3 hits: 1 main (factory, teardown via `teardownWebView`) + 2 transient (both via `useTransientWebView`); inspection of the finally blocks |
| Peak live WebViews during cache-clear flows | 2 (main + transient) | 1 (main reused; transient only when main is null) | code path inspection |
| `>1` live WebView debug warning | absent | present: `WebViewBudget` counter, incremented on every construction site, decremented on every destroy path; `Logger.w` when `noteCreated` sees >1 | `TransientWebViewTest` (7 tests incl. overBudget signal) |

## A3 — setOffscreenPreRaster

| | Before | After |
|---|---|---|
| `settings.setOffscreenPreRaster` | absent | present in `createPlayerWebView` (covers initial + B4-rebuilt WebViews), guarded at API 29 (Q — conservative; docs carry no lower-bound note) + try/catch, pref-gated via `WebViewTuning` / `offscreen_pre_raster` (default ON, user-togglable for A/B; applies on WebView (re)creation) |

Counting: code inspection.

## A8 — hardwareAccelerated

| | Before | After |
|---|---|---|
| `android:hardwareAccelerated` in manifest | absent (platform default `true`, but unverified) | explicit `android:hardwareAccelerated="true"` on `<application>` |

Counting: manifest inspection.

## B1 — webView.onPause() + pauseTimers()

| Metric | Before | After | Counting method |
|---|---|---|---|
| `onPause()` override | absent | present: `webView?.onPause()` + `WebView.pauseTimers()` via `WebViewPauseCoordinator`; skipped in PiP (activity paused-but-visible, video overlay needs the renderer); skipped when `spotilol_prefs` / `WebViewNativePause` = false (default true) → falls back to `__splBg`-only | code inspection |
| `onResume()` restore | `__splBg=false` + restart of the 3 `__splWas*` intervals | + `WebView.resumeTimers()` + `webView?.onResume()` (symmetric pair, order reversed) + `__splWas*` block extended with `typeof` guards | code inspection |
| Intervals cleared in `onStop` | 3 (`pfint`, `afint`, `cssint`) | 3 (unchanged set — inventory re-audited 2026-10-10; Worker 1's C1 warden keeps `firstFuck`/`addAutoFeatures`/`addCSSJSHack` as idempotent re-registration entry points, so the restore calls stay valid) | `grep clearInterval MainActivity.kt` |
| Intervals restarted in `onResume` | 3 / 3 cleared | 3 / 3 cleared; every OTHER injected interval (12 total per PLAN baseline) is covered by the symmetric `pauseTimers`/`resumeTimers` pair, most also honor the `__splBg` in-page guard (kept as the complement) | `grep -c "setInterval(" webview/injections/` = baseline 12; `WebViewPauseCoordinatorTest` (4 tests: pause order, resume order, symmetry, balanced no-op) |

Background JS timer wakeups while backgrounded: → 0 by construction
(`pauseTimers` is global across all WebViews); foreground resume restores all
loops (native pair + `__splWas*` block). Audio-continuation risk is documented
in code: Chromium exempts audible renderers (RESEARCH.md B2), but the
`WebViewNativePause` pref (default ON) is the per-WebView-version fallback —
**must be verified on device**.

## B2 — setRendererPriorityPolicy

| | Before | After |
|---|---|---|
| Renderer priority transitions | none | `onStop` → `RENDERER_PRIORITY_BOUND, true`; `onResume` → `RENDERER_PRIORITY_IMPORTANT, false`; both guarded at API 26 (O, verified: method added in O) + try/catch |

Counting: code inspection. No functional change while foregrounded.

## B3 — onTrimMemory staged shedding

| Metric | Before | After | Counting method |
|---|---|---|---|
| Trim levels handled in `MainActivity` | 0 (`MediaNotificationService` had its own, service-only) | 4 distinct levels: `UI_HIDDEN`+ → `webView.clearCache(false)` (RAM only, disk kept); `MODERATE`+ → additionally `window.__spotilol.onMemoryPressure(level, name)` with `name` ∈ {`moderate`, `critical`} | `TrimMemoryPolicyTest` (5 tests: RAM-cache threshold, hook threshold, JS shape, stub shape) |
| JS hook point | absent | `MEMORY_PRESSURE_STUB_JS` in the document-start bootstrap (`buildEarlyJs`); `\|\|`-guarded so Worker 3's list-virtualization implementation wins when it lands | `node --check` on the extracted stub |

Level mapping: `>= TRIM_MEMORY_COMPLETE (80)` → `critical`; `>= TRIM_MEMORY_MODERATE (60)` → `moderate`; below → no JS call. `RUNNING_*` levels do not evict (foreground).

## B4 — onRenderProcessGone recovery

| | Before | After |
|---|---|---|
| Handler | returned `true`, destroyed the dead view, no rebuild (user saw the error screen / dead player) | 6-step recovery via `RendererRecoveryCoordinator`: 1. snapshot playback state (`pipPlaying`, the activity-local mirror of the service's media status) + dead view's parent/index/layout-params/URL — **before** destroy (the client deliberately no longer destroys; it only `stopLoading()`s and hands the dead view over) · 2. `teardownWebView()` (same hygiene as the normal destroy path) · 3. `createPlayerWebView()` — identical settings/clients/proxy (single canonical config point) · 4. fresh view takes the dead view's slot in the parent; `MediaNotificationService.webView` re-pointed (the service holds the WebView reference — notification actions + `wakeAndRun` flow through it) · 5. two-phase injection re-runs automatically (document-start installed before first `loadUrl`; `onPageFinished → injectPlayerControl` on the new client instance, which re-registers its prefs listener) · 6. `onPlayerInjected` fires → one-shot `actPlayPause(true)` if the snapshot said "was playing" (fresh page restores queue/session from account cookies — no app restart) |

Counting: `RendererRecoveryTest` (5 tests with a fake view: exact call order,
destroy-before-create, null-URL fallback, restore armed with the snapshot
value, service rebind targets the new view).

## C4 — post-login payload split

| Metric | Before | After | Counting method |
|---|---|---|---|
| Immediate (time-to-interactive) chunk | ~330 KB single `evaluateJavascript` | **~231 KB** (`coreJs`, UTF-8 bytes, pre-strip; runtime emits the exact post-strip bytes via `Logger.d "payload split: core=…B immediate, deferred=…B on idle"`) | sum of UTF-8 bytes of the 21 core `CONTENT` strings extracted from `webview/injections/*.kt` |
| Deferred chunk | 0 | **~99 KB (30.1%)** on `requestIdleCallback` (timeout 3000) → `setTimeout(3000)` fallback; payload rides as an escaped JSON string literal, eval'd once in global scope (`(0,eval)`) — open.spotify.com's CSP includes `'unsafe-eval'` (verified via `curl -sI` 2026-10-10), so this is not blocked | same method, 4 deferred `CONTENT` strings: SearchOverlay 41,944 B · CollectionDownload 16,252 B · ContextMenuDownload 10,322 B · PlaylistSort 30,802 B |
| Deferred membership | n/a | exactly {SearchOverlay, CollectionDownload, ContextMenuDownload, PlaylistSort} — the downloads UI, sort, and search overlay | `PayloadChunkingTest` (10 tests: exact membership, core-before-deferred evaluation order via a recording evaluator, lossless partition, engine-follows-mode, scheduler shape, `jsonQuote` escaping) |
| Call-graph changes | n/a | **none** — only `evaluateJavascript` timing changed. Dependency audit 2026-10-10: no core chunk references a deferred chunk's `window.*` symbols at inject time (`DownloadProgress` *defines* `window.splDownloadProgress`; `CollectionDownload` only calls it behind `typeof`; `PlaylistSort` wraps `window.fetch` after document-start `FetchOverride`; nothing in core/native touches `splSearchInit`/`__splCtxDlInit`/`__splColDlInit`; the `PlaylistSortEnabled` pref listener guards with `if(window.splPlaylistSort)`) | `grep` cross-reference audit, recorded in code comment |

Note on the PLAN target ("~275 KB → ~150 KB"): the deferred set is fixed by
the item's scope (downloads UI + PlaylistSort + SearchOverlay) = 99 KB / 30%.
The remaining core (~231 KB) is dominated by the player engines
(`SpotilolPlayer` ~44 KB / `CustomUI` ~29 KB — only one is injected per
`PlayerMode`), `AndroidAuto` (~30 KB) and `CssHack` (~28 KB); further
reductions belong to Worker 3's virtualization / lazy-load work, not this
item. The win here is 99 KB of parse+compile moved off the critical path.

New JS validity (PLAN E2): `node --check` passes on the extracted
`MEMORY_PRESSURE_STUB_JS` and the deferred scheduler template.

## Unit tests added (all under `app/src/test/java/com/project/lol/`)

- `webview/TrimMemoryPolicyTest.kt` — 5 tests (B3)
- `webview/PayloadChunkingTest.kt` — 10 tests (C4)
- `webview/RendererRecoveryTest.kt` — 5 tests (B4)
- `ui/WebViewPauseCoordinatorTest.kt` — 4 tests (B1)
- `ui/TransientWebViewTest.kt` — 7 tests (A2)

31 tests total. `testImplementation(libs.junit4)` was already added by a
sibling worker (build.gradle.kts) — no build-file change needed from me.

## Prefs introduced (all `spotilol_prefs` unless noted)

| Pref | Default | Purpose |
|---|---|---|
| `WebViewNativePause` | `true` | B1 fallback: `false` disables the native `onPause`/`pauseTimers`, relying on `__splBg` only (audio-stall escape hatch per WebView version) |
| `WebViewTuning` / `offscreen_pre_raster` | `true` | A3 kill switch for A/B (applies on WebView (re)creation) |

## Not done / open risks

1. **No device verification** (no device in this environment): B1 audio
   continuation under `onPause`/`pauseTimers`, B2 priority transitions, B3
   trim behavior, B4 kill-recovery (`adb shell am kill` on the renderer),
   A1/A3/C4 timing deltas — all need the on-device pass per PLAN.md.
2. **Kotlin compile**: no local JDK here; code reviewed carefully (API-level
   guards double-checked: `setRendererPriorityPolicy` = API 26/O per task +
   docs; `setOffscreenPreRaster` guarded at Q conservatively + try/catch;
   nullability audited). GitHub Actions (`build-apk.yml`) is the compile
   gate — **not yet run**; the parent should trigger it on this branch.
3. `WebView.pauseTimers()` also pauses the transient warmup/clear-cache
   WebViews if one were alive during backgrounding — by construction none is
   (all are short-lived and main-thread-confined).
4. `MediaNotificationService.wakeAndRun` calls `wv.onResume()` +
   `resumeTimers()` on media-button presses while backgrounded — pre-existing
   behavior, intentionally left (the service needs JS to handle the action);
   it temporarily unpauses timers until the next `onPause`.
