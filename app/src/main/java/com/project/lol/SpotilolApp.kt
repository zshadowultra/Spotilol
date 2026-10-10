package com.project.lol

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.webkit.WebView
import com.project.lol.util.CrashHandler
import com.project.lol.util.Logger

class SpotilolApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.init(this)
        CrashHandler.install(this)
        Logger.s("app", "started")
        // A1: warm the Chromium engine now so the player WebView starts warm.
        warmChromiumEngine()
    }

    /**
     * A1 — Chromium engine warmup.
     *
     * Creating the first WebView in a process pays for the renderer-process
     * spawn, native library load and GPU init. Doing it here (once, on a
     * background thread with its own Looper) moves that cost off the
     * MainActivity cold-start path; the throwaway instance is destroyed
     * immediately so no renderer is left alive.
     *
     * Best-effort by design: any failure is logged and swallowed — a missed
     * warmup must never break app start (correctness first).
     */
    private fun warmChromiumEngine() {
        try {
            val thread = HandlerThread("wv-warmup").apply { start() }
            Handler(thread.looper).post {
                try {
                    val wv = WebView(this@SpotilolApp)
                    wv.destroy()
                    Logger.d("app", "chromium engine warmed (throwaway webview created+destroyed)")
                } catch (e: Exception) {
                    Logger.w("app", "webview warmup failed: ${e.message}")
                } finally {
                    thread.quitSafely()
                }
            }
        } catch (e: Exception) {
            Logger.w("app", "webview warmup skipped: ${e.message}")
        }
    }
}
