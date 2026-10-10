package com.project.lol.webview

import com.project.lol.ui.RendererRecoveryCoordinator
import org.junit.Assert.*
import org.junit.Test

/**
 * B4: onRenderProcessGone recovery runs as an explicit ordered sequence —
 * snapshot playback state + URL, destroy the dead view, create a new one with
 * identical settings, re-attach it (and the MediaNotificationService WebView
 * reference), then arm the one-shot playback restore fired by
 * onPlayerInjected.
 *
 * The fake view stands in for android.webkit.WebView (not instantiable on
 * the JVM); what is asserted is the order and completeness of the steps.
 */
class RendererRecoveryTest {

    private class FakeView(val id: String)

    private class RecordingSteps : RendererRecoveryCoordinator.Steps<FakeView> {
        val calls = mutableListOf<String>()
        var wasPlaying = true
        var url: String? = "https://open.spotify.com/playlist/abc"

        override fun snapshotPlayback(): Boolean {
            calls += "snapshotPlayback"
            return wasPlaying
        }

        override fun snapshotUrl(): String? {
            calls += "snapshotUrl"
            return url
        }

        override fun destroyOld() {
            calls += "destroyOld"
        }

        override fun createNew(url: String): FakeView {
            calls += "createNew:$url"
            return FakeView("new")
        }

        override fun attachNew(view: FakeView) {
            calls += "attachNew:${view.id}"
        }

        override fun bindService(view: FakeView) {
            calls += "bindService:${view.id}"
        }

        override fun armPlaybackRestore(wasPlaying: Boolean) {
            calls += "armPlaybackRestore:$wasPlaying"
        }
    }

    @Test
    fun `recovery runs the exact ordered sequence`() {
        val steps = RecordingSteps()
        RendererRecoveryCoordinator(steps).recover()
        assertEquals(
            listOf(
                "snapshotPlayback",
                "snapshotUrl",
                "destroyOld",
                "createNew:https://open.spotify.com/playlist/abc",
                "attachNew:new",
                "bindService:new",
                "armPlaybackRestore:true"
            ),
            steps.calls
        )
    }

    @Test
    fun `destroy happens before create - never two renderers alive`() {
        val steps = RecordingSteps()
        RendererRecoveryCoordinator(steps).recover()
        val destroyAt = steps.calls.indexOf("destroyOld")
        val createAt = steps.calls.indexOfFirst { it.startsWith("createNew") }
        assertTrue("destroy must precede create", destroyAt >= 0 && createAt > destroyAt)
    }

    @Test
    fun `null URL falls back to the web player root`() {
        val steps = RecordingSteps().apply { url = null }
        RendererRecoveryCoordinator(steps).recover()
        assertTrue(steps.calls.any { it == "createNew:https://open.spotify.com/" })
    }

    @Test
    fun `playback restore is armed with the snapshot value`() {
        val playing = RecordingSteps().apply { wasPlaying = true }
        RendererRecoveryCoordinator(playing).recover()
        assertTrue(playing.calls.contains("armPlaybackRestore:true"))

        val paused = RecordingSteps().apply { wasPlaying = false }
        RendererRecoveryCoordinator(paused).recover()
        assertTrue(paused.calls.contains("armPlaybackRestore:false"))
    }

    @Test
    fun `service rebind targets the NEW view, not the dead one`() {
        val steps = RecordingSteps()
        RendererRecoveryCoordinator(steps).recover()
        assertTrue(steps.calls.contains("bindService:new"))
        assertFalse(steps.calls.any { it == "bindService:dead" })
    }
}
