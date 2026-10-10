package com.project.lol.webview

import android.graphics.Bitmap
import com.project.lol.util.Logger
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.project.lol.webview.helpers.*
import com.project.lol.webview.injections.*
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class SpotifyWebViewClient(
    private val onLoginRequired: () -> Unit,
    private val onNavStateChanged: ((Boolean) -> Unit)? = null,
    private val onRenderProcessGone: ((deadView: WebView) -> Unit)? = null,
    private val onWebViewError: ((errorCode: Int, description: String) -> Unit)? = null,
    private val onPlayerInjected: (() -> Unit)? = null
) : WebViewClient() {

    private var currentWebView: WebView? = null
    private var prefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var boundPrefs: android.content.SharedPreferences? = null
    private var pageStartedAt = 0L
    private var docStartHandler: ScriptHandler? = null
    private var docStartJs: String? = null
    private var docStartView: WebView? = null
    private var docStartJustRegistered = false

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        val canGoBack = view?.canGoBack() == true
        Logger.d(TAG, "history: $url reload=$isReload canGoBack=$canGoBack")
        onNavStateChanged?.invoke(canGoBack)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view == null || url == null) return

        val elapsed = if (pageStartedAt > 0) System.currentTimeMillis() - pageStartedAt else -1
        pageStartedAt = 0
        currentWebView = view
        registerPrefsListener(view)
        Logger.i(TAG, "page finished in ${elapsed}ms: $url")

        if (url.startsWith("https://www.facebook.com/privacy/consent/gdp/")) {
            Logger.s(TAG, "route: facebook gdpr bypass")
            onPageFinishedClean(view, FbGdprBypass.CONTENT)
            return
        }

        if (url.endsWith("/login")) {
            Logger.s(TAG, "route: classic login button")
            onPageFinishedClean(view, ClassicLoginButton.CONTENT)
        }

        var loggedIn = view.context.getSharedPreferences("spotilol_prefs", 0)
            .getBoolean("LoggedIn", false)

        // Test builds: seed a baked-in Spotify session so automated device
        // tests (Appetize/Firebase) don't need interactive login.
        if (!loggedIn && TestAuth.trySeedTestSession(view.context)) {
            Logger.i(TAG, "test session seeded, reloading into authenticated state")
            loggedIn = true
            view.reload()
            return
        }

        // Test builds: force the custom UI even without login, so tap
        // mechanics can be tested on a fresh device. Data calls will fail
        // (no auth) but UI interaction is fully testable.
        val forceTestUi = try {
            view.context.assets.open("test-force-ui.json").close()
            // Ensure CustomUI mode is active for the test
            view.context.getSharedPreferences("spotilol_prefs", 0)
                .edit().putString("PlayerMode", "customui").apply()
            true
        } catch (e: Exception) { false }

        if (!loggedIn && !forceTestUi) {
            Logger.i(TAG, "not logged in, arming login detection")
            onPageFinishedClean(view, LoginDetection.CONTENT)
            return
        }

        Logger.s(TAG, "logged in, injecting player control in 500ms")

        view.postDelayed({
            injectPlayerControl(view)
        }, 500)

        view.evaluateJavascript(LogoutCheck.CONTENT) { result ->
            Logger.d(TAG, "logout check: $result")
            if (result == "\"out\"") {
                Logger.w(TAG, "session expired, back to login")
                view.context.getSharedPreferences("spotilol_prefs", 0)
                    .edit().putBoolean("LoggedIn", false).apply()
                view.loadUrl("https://accounts.spotify.com/login")
            }
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        pageStartedAt = System.currentTimeMillis()
        val prefs = view?.context?.getSharedPreferences("spotilol_prefs", 0)

        val useProxy = prefs?.getString("ConnectionMode", "normal") == "proxy"
        val powerSave = prefs?.getBoolean("PowerSave", false) ?: false
        val blockSW = prefs?.getBoolean("BlockServiceWorker", true) ?: true
        val hideEmptyPlayer = prefs?.getBoolean("HideEmptyPlayer", false) ?: false
        val playlistSort = prefs?.getBoolean("PlaylistSortEnabled", true) ?: true
        val showScrollbar = prefs?.getBoolean("ShowScrollbar", true) ?: true

        Logger.i(
            TAG,
            "page started: $url proxy=$useProxy powerSave=$powerSave blockSW=$blockSW " +
                "hideEmpty=$hideEmptyPlayer playlistSort=$playlistSort scrollbar=$showScrollbar"
        )

        AdIdStore.clear()
        if (view == null || prefs == null) return

        // open.spotify.com: the payload is a document-start script (registered in
        // MainActivity before the first load), so it runs before Spotify's own scripts.
        // If it had to be (re)registered just now - first run or prefs changed - it may
        // not apply to this navigation, so also inject it the old way.
        if (isWebPlayerUrl(url) && installDocumentStartScripts(view) && !docStartJustRegistered) return

        view.evaluateJavascript(buildEarlyJs(prefs, isGoogleAuthUrl(url)), null)
    }

    /**
     * Registers the early payload for open.spotify.com as a document-start script,
     * re-registering if prefs changed the payload. Returns false if the WebView doesn't
     * support DOCUMENT_START_SCRIPT. Sets [docStartJustRegistered] when a (re)registration
     * happened in this call.
     */
    fun installDocumentStartScripts(view: WebView): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return false
        val prefs = view.context.getSharedPreferences("spotilol_prefs", 0)
        val js = buildEarlyJs(prefs, isGoogle = false)
        docStartJustRegistered = false
        if (js == docStartJs && docStartView === view) return true
        docStartHandler?.remove()
        docStartHandler = WebViewCompat.addDocumentStartJavaScript(view, js, setOf(WEB_PLAYER_ORIGIN))
        docStartJs = js
        docStartView = view
        docStartJustRegistered = true
        Logger.i(TAG, "document-start payload registered (${js.length} bytes)")
        return true
    }

    private fun buildEarlyJs(prefs: android.content.SharedPreferences, isGoogle: Boolean): String {
        val useProxy = prefs.getString("ConnectionMode", "normal") == "proxy"
        val powerSave = prefs.getBoolean("PowerSave", false)
        val blockSW = prefs.getBoolean("BlockServiceWorker", true)
        val hideEmptyPlayer = prefs.getBoolean("HideEmptyPlayer", false)
        val playlistSort = prefs.getBoolean("PlaylistSortEnabled", true)
        val showScrollbar = prefs.getBoolean("ShowScrollbar", true)
        // Each payload runs in its own try/catch, matching the old behaviour where each
        // was a separate evaluateJavascript call and one failure didn't stop the rest.
        val parts = buildList {
            // Timing audit (round 2): the __splPerf helper must exist before
            // any part runs, so it is the first part; the end part emits the
            // document-start eval duration to logcat (tag spotilol.perf).
            add(PerfMarks.CONTENT)
            add(PerfMarks.DOCSTART_BEGIN)
            add("window.__splShowScrollbar=$showScrollbar;")
            add("window.__spotilolUseProxy=$useProxy;")
            add("window.__splPowerSavePref=$powerSave;")
            add("window.__splHideEmpty=$hideEmptyPlayer;")
            add("window.__splPlaylistSortEnabled=$playlistSort;")
            add(MEMORY_PRESSURE_STUB_JS)
            add(if (isGoogle) GoogleSpoof.CONTENT else BrowserSpoof.CONTENT)
            add(FetchOverride.CONTENT)
            add(AdStateHook.CONTENT)
            if (blockSW) add(WorkerNeutralize.CONTENT)
            add(GaBlocker.CONTENT)
            add(PowerSave.CONTENT)
            add(SettingsFix.CONTENT)
            add(VideoPark.CONTENT)
            add(CookieBypass.CONTENT)
            add(PerfMarks.DOCSTART_END)
        }
        return parts.joinToString("\n") { "try{\n$it\n}catch(e){}" }
    }

    private fun isWebPlayerUrl(url: String?): Boolean =
        url != null && (url == WEB_PLAYER_ORIGIN || url.startsWith("$WEB_PLAYER_ORIGIN/"))

    /**
     * B4 — renderer-death entry point. Returns true (we handle it).
     *
     * The dead view is NOT destroyed here: the owner (MainActivity) must
     * capture the view's parent / layout params / current URL BEFORE destroy,
     * so the callback receives the dead view and owns the full teardown +
     * rebuild sequence (see MainActivity.handleRendererGone for the exact
     * ordered steps). If nobody handles it, fall back to destroying the view
     * so no zombie renderer is left behind.
     */
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Logger.e(TAG, "renderer process gone: crashed=${detail?.didCrash()}")
        view?.stopLoading()
        val cb = onRenderProcessGone
        if (view != null && cb != null) {
            cb(view)
        } else {
            view?.destroy()
        }
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame != true) return
        val code = try { error?.errorCode ?: -1 } catch (_: Exception) { -1 }
        val desc = try { error?.description?.toString() ?: "" } catch (_: Exception) { "" }
        Logger.e(TAG, "main frame error $code ($desc) ${request.url} method=${request.method}")
        onWebViewError?.invoke(code, desc)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame != true) return
        val status = try { errorResponse?.statusCode ?: 0 } catch (_: Exception) { 0 }
        Logger.e(TAG, "main frame http $status ${request.url}")
        if (status >= 400) {
            onWebViewError?.invoke(status, "HTTP $status")
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()

        if (isAnalyticsDomain(url)) {
            val headers = mapOf("Access-Control-Allow-Origin" to "*")
            return WebResourceResponse("text/plain", "utf-8", 200, "OK", headers,
                ByteArrayInputStream(ByteArray(0)))
        }

        if (AdIdStore.matches(url)) {
            view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
            val silent = view.context.assets?.open("silent.mp3") ?: return null
            return WebResourceResponse("audio/mpeg", null, silent)
        }

        val useProxy = view.context.getSharedPreferences("spotilol_prefs", 0)
            .getString("ConnectionMode", "normal") == "proxy"

        if (!useProxy) {
            // Only ad-audio candidates and Google auth URLs need the native sniff below.
            // Everything else goes straight to the WebView, which would otherwise fetch
            // it a second time after this blocking request returns null.
            if (!isAdAudioUrl(url) && !isGoogleAuthUrl(url)) return null
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = request.method
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val isGoogle = isGoogleAuthUrl(url)
                    for ((k, v) in request.requestHeaders) {
                        val lk = k.lowercase(Locale.ROOT)
                        if (lk != "x-requested-with" && lk != "sec-gpc" && !lk.startsWith("sec-ch-ua") &&
                            !(isGoogle && lk == "user-agent")
                        ) {
                            conn.setRequestProperty(k, v)
                        }
                    }
                    if (isGoogle) {
                        conn.setRequestProperty("User-Agent", DESKTOP_UA)
                        val cookie = CookieManager.getInstance().getCookie(url)
                        if (!cookie.isNullOrEmpty()) conn.setRequestProperty("Cookie", cookie)
                    }
                    conn.setRequestProperty("sec-gpc", "1")
                    conn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"")
                    conn.setRequestProperty("sec-ch-ua-mobile", "?0")
                    conn.setRequestProperty("sec-ch-ua", "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Google Chrome\";v=\"150\"")
                    conn.connect()
                    if (isGoogle) {
                        conn.headerFields.forEach { (key, values) ->
                            if (key != null && key.equals("Set-Cookie", ignoreCase = true)) {
                                values.forEach { CookieManager.getInstance().setCookie(url, it) }
                            }
                        }
                        CookieManager.getInstance().flush()
                    }
                    val contentType = conn.contentType
                    if (contentType == "audio/mpeg" &&
                        !url.contains("podz-content") && !url.contains("gew4-spclient") &&
                        isAdAudioUrl(url)
                    ) {
                        view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
                        val silent = view.context.assets?.open("silent.mp3") ?: return null
                        return WebResourceResponse("audio/mpeg", null, silent)
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                return null
            }
            return null
        }

        val adMatch = matchAdCdn(url)
        if (adMatch != null) {
            view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
            val silent = view.context.assets?.open("silent.mp3") ?: return null
            return WebResourceResponse("audio/mpeg", null, silent)
        }

        return null
    }

    private fun isGoogleAuthUrl(url: String?): Boolean {
        if (url == null) return false
        val host = runCatching { android.net.Uri.parse(url).host }.getOrNull()
            ?.lowercase() ?: return false
        return host == "google.com" ||
            host.endsWith(".google.com") ||
            host.contains(".google.") ||
            host.endsWith(".youtube.com") ||
            host == "youtube.com"
    }

    private fun injectPlayerControl(view: WebView) {
        val prefs = view.context.getSharedPreferences("spotilol_prefs", 0)
        val autoPlayMode = prefs.getString("APlayMode", "disabled") ?: "disabled"
        val closeNowPlay = prefs.getBoolean("CloseNowPlay", true)
        val amoledEnabled = prefs.getBoolean("AmoledTheme", true)
        val customCss = prefs.getString("CustomCss", "") ?: ""
        val playerMode = prefs.getString("PlayerMode", "customui") ?: "spotilol"
        val useProxy = prefs.getString("ConnectionMode", "normal") == "proxy"
        val debugOverlay = Logger.isEnabled()
        val takeControl = prefs.getBoolean("TakeControl", true)
        val hideEmptyPlayer = prefs.getBoolean("HideEmptyPlayer", false)
        val playlistSortEnabled = prefs.getBoolean("PlaylistSortEnabled", true)
        val showScrollbar = prefs.getBoolean("ShowScrollbar", true)
        val lyricsStyle = prefs.getString("LyricsStyle", LyricsTheme.DEFAULT_STYLE) ?: LyricsTheme.DEFAULT_STYLE

        Logger.s(
            TAG,
            "inject: engine=$playerMode autoPlay=$autoPlayMode closeNp=$closeNowPlay amoled=$amoledEnabled " +
                "proxy=$useProxy logging=$debugOverlay takeControl=$takeControl sort=$playlistSortEnabled " +
                "scrollbar=$showScrollbar css=${customCss.length}chars lyrics=$lyricsStyle"
        )

        // C4: the post-login payload is assembled as named chunks, then split
        // into an immediate (time-to-interactive) chunk and a deferred chunk.
        // Only the evaluateJavascript timing changes — the call graph is intact.
        val chunks = assemblePlayerChunks(
            flagsJs = buildString {
                append("window.autoPlayMode='$autoPlayMode';\n")
                append("window.closeNpPref=$closeNowPlay;\n")
                append("window.__spotilolUseProxy=$useProxy;\n")
                append("window.__splTakeControl=$takeControl;\n")
                append("window.__splHideEmpty=$hideEmptyPlayer;\n")
                append("window.__splPlaylistSortEnabled=$playlistSortEnabled;\n")
                append("window.__splShowScrollbar=$showScrollbar;\n")
                if (debugOverlay) {
                    append(DevLogPrelude.js())
                    append("\n")
                }
            },
            recAccountJs = """
                (function(){
                    var recAcc=function(){
                        try{
                            var uw=document.querySelector('[data-testid="user-widget-link"]');
                            if(uw){
                                var txt=(uw.textContent||'').split('\n')[0].trim();
                                if(txt) AndBridge.recAccountName(txt);
                            }
                        }catch(e){}
                    };
                    setTimeout(recAcc,5000);
                    setInterval(recAcc,60000);
                })();
            """.trimIndent(),
            playerMode = playerMode
        )
        val (coreJs, deferredJs) = splitPlayerPayload(chunks)
        val themeJs = buildAmoledJs(amoledEnabled) + "\n" +
                AccentTheme.buildAccentJs(view.context) + "\n" +
                buildCustomCssJs(customCss) + "\n" +
                LyricsTheme.buildLyricsStyleJs(lyricsStyle)

        val coreBytes = coreJs.toByteArray(Charsets.UTF_8).size
        val deferredBytes = deferredJs.toByteArray(Charsets.UTF_8).size
        Logger.d(
            TAG,
            "payload split: core=${coreBytes}B immediate, deferred=${deferredBytes}B on idle " +
                "(engine=$playerMode)"
        )

        injectSplitPayload(
            evaluate = { js -> view.evaluateJavascript(js, null) },
            coreJs = coreJs,
            themeJs = themeJs,
            deferredJs = deferredJs,
            playerMode = playerMode
        )
        onPlayerInjected?.invoke()
    }

    private fun registerPrefsListener(view: WebView) {
        val prefs = view.context.getSharedPreferences("spotilol_prefs", 0)
        // Listener doesn't depend on the WebView instance (it reads currentWebView),
        // so registering once is enough. Re-register only if the prefs instance
        // actually changed (new context after a renderer-crash rebuild).
        if (boundPrefs === prefs && prefsListener != null) return
        prefsListener?.let { boundPrefs?.unregisterOnSharedPreferenceChangeListener(it) }
        boundPrefs = prefs

        prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            val wv = currentWebView ?: return@OnSharedPreferenceChangeListener
            Logger.d(TAG, "pref changed: $key")
            when (key) {
                "PlayerMode" ->
                    switchPlayerMode(wv, prefs.getString("PlayerMode", "customui") ?: "spotilol")
                "PowerSave" -> {
                    val on = prefs.getBoolean("PowerSave", false)
                    wv.evaluateJavascript("if(window.__splApplyPowerSave) window.__splApplyPowerSave($on);", null)
                }
                "CloseNowPlay" -> {
                    val closeNp = prefs.getBoolean("CloseNowPlay", true)
                    wv.evaluateJavascript("window.closeNpPref=$closeNp;", null)
                }
                "APlayMode" -> {
                    val mode = prefs.getString("APlayMode", "disabled") ?: "disabled"
                    wv.evaluateJavascript("window.autoPlayMode='$mode';", null)
                }
                "AmoledTheme", "CustomCss", "LyricsStyle" -> {
                    val js = buildAmoledJs(prefs.getBoolean("AmoledTheme", true)) + ";\n" +
                            buildCustomCssJs(prefs.getString("CustomCss", "") ?: "")
                    wv.evaluateJavascript(js, null)
                    wv.evaluateJavascript(LyricsTheme.buildLyricsStyleJs(prefs.getString("LyricsStyle", LyricsTheme.DEFAULT_STYLE) ?: LyricsTheme.DEFAULT_STYLE), null)
                }
                "PaletteSeed", "MaterialYou" ->
                    wv.evaluateJavascript(AccentTheme.buildAccentJs(wv.context), null)
                "TakeControl" -> {
                    val on = prefs.getBoolean("TakeControl", true)
                    wv.evaluateJavascript("window.__splTakeControl=$on;", null)
                }
                "BlockServiceWorker" -> {
                    if (prefs.getBoolean("BlockServiceWorker", true)) {
                        wv.evaluateJavascript(WorkerNeutralize.CONTENT + ";\n" + SW_UNREGISTER_JS, null)
                    } else {
                        wv.reload()
                    }
                }
                "HideEmptyPlayer" -> {
                    val hideEmpty = prefs.getBoolean("HideEmptyPlayer", false)
                    wv.evaluateJavascript("window.__splHideEmpty=$hideEmpty; if(window.splApplyEmpty) window.splApplyEmpty();", null)
                }
                "PlaylistSortEnabled" -> {
                    val sortOn = prefs.getBoolean("PlaylistSortEnabled", true)
                    wv.evaluateJavascript("window.__splPlaylistSortEnabled=$sortOn; if(window.splPlaylistSort) window.splPlaylistSort.refresh();", null)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun switchPlayerMode(view: WebView, mode: String) {
        Logger.i(TAG, "switch player engine: $mode")
        if (mode == "original") {
            val js = """
                (function(){
                    var pl=document.getElementById('spotilolPlayerControls');
                    if(pl) pl.style.display='none';
                    var s=document.createElement('style');
                    s.id='spl-np-show';
                    s.textContent='aside[data-testid="now-playing-bar"]{display:flex!important}';
                    document.head.appendChild(s);
                })();
            """.trimIndent()
            view.evaluateJavascript(js, null)
        } else {
            view.evaluateJavascript("if(typeof initSpotilolPlayer!=='function'){" + SpotilolPlayer.CONTENT + "}", null)
            val js = """
                (function(){
                    var s=document.getElementById('spl-np-show');
                    if(s) s.remove();
                    var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                    if(npb) npb.style.display='none';
                    var pl=document.getElementById('spotilolPlayerControls');
                    if(pl){pl.style.display='flex';}
                    else if(typeof initSpotilolPlayer==='function'){initSpotilolPlayer();}
                })();
            """.trimIndent()
            view.evaluateJavascript(js, null)
        }
    }

    private fun onPageFinishedClean(view: WebView, js: String) {
        view.evaluateJavascript(JsUtils.stripConsoleLogs(js), null)
    }

    companion object {
        private const val TAG = "wv"
        private const val WEB_PLAYER_ORIGIN = "https://open.spotify.com"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

        private val SW_UNREGISTER_JS = """
            try {
                if(navigator.serviceWorker){
                    navigator.serviceWorker.getRegistrations().then(function(regs){
                        regs.forEach(function(r){ r.unregister(); });
                    });
                }
            } catch(e){}
        """.trimIndent()
    }
}

// ---------------------------------------------------------------------------
// B3 — window.__spotilol.onMemoryPressure hook stub (injection bootstrap).
//
// The real implementation (shrink virtualized-list overscan, drop the
// in-memory artwork cache, cancel prefetch queues) lands with the list
// virtualization work; this stub only guarantees the native call site never
// throws on an undefined function, and never overrides an implementation
// injected later (the || keeps whichever definition wins first).
// ---------------------------------------------------------------------------
internal val MEMORY_PRESSURE_STUB_JS = """
    window.__spotilol = window.__spotilol || {};
    window.__spotilol.onMemoryPressure = window.__spotilol.onMemoryPressure || function(level, name) {
    };
""".trimIndent()

/**
 * B3 — pure mapping of Android onTrimMemory levels to WebView shedding
 * actions. Kept free of Android calls so it is unit-testable; the Activity
 * applies the resulting actions.
 */
internal object TrimMemoryPolicy {
    /** RAM-only cache eviction at TRIM_MEMORY_UI_HIDDEN and above (disk kept). */
    fun shouldClearRamCache(level: Int): Boolean =
        level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN

    /**
     * JS memory-pressure hook at MODERATE and above; null means "no JS call".
     * Returns the severity name handed to window.__spotilol.onMemoryPressure.
     */
    fun pressureName(level: Int): String? = when {
        level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "critical"
        level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "moderate"
        else -> null
    }

    fun buildPressureJs(level: Int, name: String): String =
        "try{if(window.__spotilol&&typeof window.__spotilol.onMemoryPressure==='function')" +
            "{window.__spotilol.onMemoryPressure($level,'$name');}}catch(e){}"
}

// ---------------------------------------------------------------------------
// C4 — post-login payload split: critical (immediate) vs deferred (idle).
//
// Only the evaluateJavascript TIMING changes; the JS call graph is untouched.
// Dependency audit (2026-10-10): no core chunk references a deferred chunk's
// window.* symbols at inject time —
//   * DownloadProgress.kt DEFINES window.splDownloadProgress (core);
//     CollectionDownload.kt only *calls* it behind a typeof guard.
//   * PlaylistSort wraps window.fetch at its own inject time (document-start
//     FetchOverride already installed, so ordering is preserved).
//   * SearchOverlay/ContextMenuDownload expose init-guarded entry points
//     (splSearchInit / __splCtxDlInit / __splColDlInit); nothing in core or in
//     native code (MainActivity, bridge, MediaNotificationService) touches
//     them — the PlaylistSortEnabled pref listener guards with
//     `if(window.splPlaylistSort)`.
// ---------------------------------------------------------------------------

/** Chunk names injected on requestIdleCallback (fallback setTimeout 3000). */
internal val DEFERRED_CHUNK_NAMES = setOf(
    "SearchOverlay",
    "CollectionDownload",
    "ContextMenuDownload",
    "PlaylistSort"
)

internal data class NamedChunk(val name: String, val js: String)
internal data class PayloadChunks(val coreJs: String, val deferredJs: String)

/**
 * Assembles the post-login payload as ordered named chunks. Pure string
 * assembly (no WebView) so chunk membership and ordering are unit-testable.
 */
internal fun assemblePlayerChunks(
    flagsJs: String,
    recAccountJs: String,
    playerMode: String
): List<NamedChunk> = listOf(
    NamedChunk("flags", flagsJs),
    // Core (immediate): everything needed for time-to-interactive.
    NamedChunk("PlayerCore", com.project.lol.webview.injections.PlayerCore.CONTENT),
    NamedChunk("TrackObserver", com.project.lol.webview.injections.TrackObserver.CONTENT),
    NamedChunk("ClassicBridge", com.project.lol.webview.injections.ClassicBridge.CONTENT),
    NamedChunk("MediaUpdater", com.project.lol.webview.injections.MediaUpdater.CONTENT),
    NamedChunk("LibraryFetcher", com.project.lol.webview.injections.LibraryFetcher.CONTENT),
    NamedChunk("LibraryParser", com.project.lol.webview.injections.LibraryParser.CONTENT),
    NamedChunk("PlaybackControls", com.project.lol.webview.injections.PlaybackControls.CONTENT),
    NamedChunk("AndroidAuto", com.project.lol.webview.injections.AndroidAuto.CONTENT),
    NamedChunk("MainLoop", com.project.lol.webview.injections.MainLoop.CONTENT),
    NamedChunk("AutoFeatures", com.project.lol.webview.injections.AutoFeatures.CONTENT),
    NamedChunk("AndroidTracker", com.project.lol.webview.injections.AndroidTracker.CONTENT),
    // Deferred: downloads UI, sort, search overlay.
    NamedChunk("SearchOverlay", com.project.lol.webview.injections.SearchOverlay.CONTENT),
    // Core resumes.
    NamedChunk("DownloadButton", com.project.lol.webview.injections.DownloadButton.CONTENT),
    NamedChunk("DownloadProgress", com.project.lol.webview.injections.DownloadProgress.CONTENT),
    // Deferred.
    NamedChunk("CollectionDownload", com.project.lol.webview.injections.CollectionDownload.CONTENT),
    NamedChunk("ContextMenuDownload", com.project.lol.webview.injections.ContextMenuDownload.CONTENT),
    // Core resumes.
    NamedChunk("recAccount", recAccountJs),
    NamedChunk("CssHack", com.project.lol.webview.injections.CssHack.CONTENT),
    NamedChunk("ModalFix", com.project.lol.webview.injections.ModalFix.CONTENT),
    NamedChunk("ErrorDialogRestyle", com.project.lol.webview.injections.ErrorDialogRestyle.CONTENT),
    NamedChunk("ToastFix", com.project.lol.webview.injections.ToastFix.CONTENT),
    NamedChunk("LyricsSyncFix", com.project.lol.webview.injections.LyricsSyncFix.CONTENT),
    NamedChunk("QueueAutoClose", com.project.lol.webview.injections.QueueAutoClose.CONTENT),
    NamedChunk("LibraryAutoClose", com.project.lol.webview.injections.LibraryAutoClose.CONTENT),
    // Deferred.
    NamedChunk("PlaylistSort", com.project.lol.webview.injections.PlaylistSort.CONTENT),
    // Player engine (immediate — it IS the visible player).
    NamedChunk(
        "SpotilolPlayer",
        if (playerMode == "spotilol") com.project.lol.webview.injections.SpotilolPlayer.CONTENT else ""
    ),
    NamedChunk(
        "CustomUI",
        if (playerMode == "customui") com.project.lol.webview.injections.CustomUI.CONTENT else ""
    )
)

/**
 * Evaluates the split payload: the core chunk immediately (time-to-interactive),
 * then the deferred chunk via the in-page idle scheduler. Extracted so the
 * core-before-deferred order is unit-testable with a recording evaluator.
 */
internal fun injectSplitPayload(
    evaluate: (String) -> Unit,
    coreJs: String,
    themeJs: String,
    deferredJs: String,
    playerMode: String
) {
    // Timing audit (round 2): core-chunk eval duration, one line on logcat
    // (tag spotilol.perf). Only the timing wrapper changes — the call graph
    // and the core-before-deferred order are untouched.
    val coreEval = (if (playerMode == "original") coreJs + "\n" + themeJs + "\n" + NP_SHOW_STYLE_JS
    else coreJs + "\n" + themeJs)
    evaluate(PerfMarks.CORE_BEGIN + "\n" + coreEval + "\n" + PerfMarks.CORE_END)
    // Deferred chunks (downloads UI, PlaylistSort, SearchOverlay) run on
    // requestIdleCallback, setTimeout(3000) fallback.
    evaluate(buildDeferredSchedulerJs(deferredJs))
}

internal const val NP_SHOW_STYLE_JS =
    "(function(){var s=document.createElement('style');s.id='spl-np-show';" +
        "s.textContent='aside[data-testid=\"now-playing-bar\"]{display:flex!important}';" +
        "document.head.appendChild(s);})();"

/** Partitions named chunks into the immediate core payload and the deferred payload. */
internal fun splitPlayerPayload(chunks: List<NamedChunk>): PayloadChunks {
    val core = chunks.filter { it.name !in DEFERRED_CHUNK_NAMES }
    val deferred = chunks.filter { it.name in DEFERRED_CHUNK_NAMES }
    return PayloadChunks(
        coreJs = com.project.lol.webview.helpers.JsUtils.stripConsoleLogs(core.joinToString("\n") { it.js }),
        deferredJs = com.project.lol.webview.helpers.JsUtils.stripConsoleLogs(deferred.joinToString("\n") { it.js })
    )
}

/**
 * Minimal JSON string escaper (pure Kotlin). org.json.JSONObject is an Android
 * stub that throws on JVM unit tests, so the scheduler's quoting cannot rely
 * on it.
 */
internal fun jsonQuote(s: String): String {
    val sb = StringBuilder(s.length + 2).append('"')
    for (c in s) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '/' -> sb.append("\\/")
            else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
    }
    return sb.append('"').toString()
}

/**
 * Builds the tiny in-page scheduler that evals the deferred payload on
 * requestIdleCallback (timeout 3000ms), falling back to setTimeout(3000).
 * The payload rides as a JSON string literal and is eval'd exactly once.
 * open.spotify.com's CSP includes 'unsafe-eval' (verified 2026-10-10), so the
 * eval is not blocked. One schedule per page (guard flag).
 */
internal fun buildDeferredSchedulerJs(deferredJs: String): String {
    val quoted = jsonQuote(deferredJs)
    return """
        (function(){
            try {
                if (window.__splDeferredScheduled) return;
                window.__splDeferredScheduled = true;
                var src = $quoted;
                var run = function(){
                    if (window.__splDeferredRan) return;
                    window.__splDeferredRan = true;
                    // Timing audit (round 2): deferred-chunk eval duration.
                    try{ if(window.__splPerf) window.__splPerf.mark('deferred-begin'); }catch(e){}
                    try { (0,eval)(src); } catch(e) {}
                    try{ if(window.__splPerf) window.__splPerf.emitSince('deferred-begin','deferred-eval'); }catch(e){}
                    src = null;
                };
                if (window.requestIdleCallback) {
                    try { window.requestIdleCallback(function(){ run(); }, {timeout: 3000}); }
                    catch(e) { setTimeout(run, 3000); }
                } else { setTimeout(run, 3000); }
            } catch(e) {}
        })();
    """.trimIndent()
}
