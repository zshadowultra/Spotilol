package com.project.lol

import android.app.Application
import com.project.lol.util.CrashHandler
import com.project.lol.util.Logger

class SpotilolApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.init(this)
        CrashHandler.install(this)
        Logger.s("app", "started")
        // NOTE: no Chromium "warmup" here. A WebView MUST be created on the
        // main thread, and Android pins the whole process to the thread of the
        // first WebView created: a background-thread warmup (previously tried
        // here on a HandlerThread) makes the real WebView in MainActivity throw
        // IllegalStateException("Calling View methods on another thread than
        // the UI thread") at WebViewChromium.init on first launch. The one-time
        // engine init cost is paid by the first real WebView instead.
    }
}
