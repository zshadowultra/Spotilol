package com.project.lol.webview.injections

import org.junit.Assert.*
import org.junit.Test

/**
 * B5-JS: every setInterval/rAF loop in these files must either stop under
 * window.__splBg / document.visibilityState==='hidden' or be covered by the
 * native onPause()/pauseTimers() path (stream B1, MainActivity.kt).
 *
 * Recurring intervals: guarded (asserted below). One-shot intervals
 * (self-terminating): documented with a B5: comment naming the native path.
 */
class BackgroundGuardAuditTest {

    @Test
    fun `warden skips all checks when backgrounded`() {
        val warden = PlayerCore.CONTENT.substringAfter("if(!window.__splWarden)")
        assertTrue(warden.contains("if(window.__splBg) return;"))
    }

    @Test
    fun `floaters interval keeps its bg guard`() {
        val floaters = PlayerCore.CONTENT.substringAfter("window.__splFloaters=[];")
        assertTrue(floaters.contains("if(window.__splBg) return;"))
    }

    @Test
    fun `css warden check keeps its own bg guard`() {
        // Belt and braces: the warden already skips on __splBg; the check keeps
        // its historical early-return too.
        val check = CssHack.CONTENT.substringAfter("function splCssCheck(){")
        assertTrue(check.contains("if(window.__splBg) return;"))
    }

    @Test
    fun `tracker fallback poll is bg-guarded`() {
        assertTrue(
            AndroidTracker.CONTENT.contains("if(!window.__splBg) readTrackState();")
        )
    }

    @Test
    fun `playlist sort fallback is bg-guarded`() {
        assertTrue(
            PlaylistSort.CONTENT.contains("if (!window.__splBg) decorate();")
        )
    }

    @Test
    fun `collection tick keeps its bg guard`() {
        val tick = CollectionDownload.CONTENT.substringAfter("function tick(){")
        assertTrue(tick.contains("if (window.__splBg) return;"))
    }

    @Test
    fun `search warden fallback is bg-guarded`() {
        val js = SearchOverlay.CONTENT
        assertTrue(
            js.contains("else setInterval(function(){ if(window.__splBg) return; splSearchWarden(); }, 2000);")
        )
    }

    @Test
    fun `download button observer skips when backgrounded`() {
        val obs = DownloadButton.CONTENT.substringAfter("window.__splDlBtnObs = new MutationObserver")
        assertTrue(obs.contains("if(window.__splBg) return;"))
    }

    @Test
    fun `transient one-shot intervals document their background story`() {
        // bootIv: one-shot boot loop; uIv: one-shot unlock poll.
        assertTrue("bootIv must be documented", MainLoop.CONTENT.contains("B5:"))
        // rfint: one-shot context-menu watch with 5s backstop.
        assertTrue("rfint must be documented", PlaybackControls.CONTENT.contains("B5:"))
        // loadAll iv: one-shot per-download scroller, finish()-cleared.
        assertTrue("loadAll iv must be documented", CollectionDownload.CONTENT.contains("B5:"))
        // PlaylistSort fallback: documented why polling remains.
        assertTrue("decorate fallback must be documented", PlaylistSort.CONTENT.contains("C5:"))
    }

    @Test
    fun `requestAnimationFrame is only the scroll throttle, never a perpetual loop`() {
        for ((name, js) in files()) {
            var idx = js.indexOf("requestAnimationFrame")
            while (idx >= 0) {
                val context = js.substring(maxOf(0, idx - 300), idx)
                // The only rAF in these files throttles scroll-driven list renders:
                // event-driven (fires only on user scroll), never self-perpetuating,
                // and rAF does not fire while the page is hidden. Covered by the
                // native onPause()/pauseTimers() path (B1) like everything else.
                assertTrue(
                    "$name: rAF must only appear in the scroll-throttle onScroll()",
                    context.contains("function onScroll(){")
                )
                idx = js.indexOf("requestAnimationFrame", idx + 1)
            }
        }
    }

    private fun files(): List<Pair<String, String>> = listOf(
        "PlayerCore" to PlayerCore.CONTENT,
        "MainLoop" to MainLoop.CONTENT,
        "AutoFeatures" to AutoFeatures.CONTENT,
        "CssHack" to CssHack.CONTENT,
        "DownloadButton" to DownloadButton.CONTENT,
        "SearchOverlay" to SearchOverlay.CONTENT,
        "CollectionDownload" to CollectionDownload.CONTENT,
        "AndroidTracker" to AndroidTracker.CONTENT,
        "PlaybackControls" to PlaybackControls.CONTENT,
        "PlaylistSort" to PlaylistSort.CONTENT
    )
}
