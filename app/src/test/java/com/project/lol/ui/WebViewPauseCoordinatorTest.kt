package com.project.lol.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * B1: native pause/resume symmetry — every paused thing is resumed.
 *
 * pauseAll():  webView.onPause() then WebView.pauseTimers()
 * resumeAll(): WebView.resumeTimers() then webView.onResume()
 */
class WebViewPauseCoordinatorTest {

    private class RecordingOps :
        WebViewPauseCoordinator.InstanceOps,
        WebViewPauseCoordinator.GlobalOps {
        val calls = mutableListOf<String>()
        override fun pause() { calls += "instancePause" }
        override fun resume() { calls += "instanceResume" }
        override fun pauseTimers() { calls += "pauseTimers" }
        override fun resumeTimers() { calls += "resumeTimers" }
    }

    @Test
    fun `pause stops the instance then the global timers`() {
        val ops = RecordingOps()
        WebViewPauseCoordinator(ops, ops).pauseAll()
        assertEquals(listOf("instancePause", "pauseTimers"), ops.calls)
    }

    @Test
    fun `resume restarts global timers then the instance`() {
        val ops = RecordingOps()
        WebViewPauseCoordinator(ops, ops).resumeAll()
        assertEquals(listOf("resumeTimers", "instanceResume"), ops.calls)
    }

    @Test
    fun `every paused thing is resumed`() {
        val ops = RecordingOps()
        val coordinator = WebViewPauseCoordinator(ops, ops)
        coordinator.pauseAll()
        coordinator.resumeAll()
        assertTrue("instance resumed", ops.calls.contains("instanceResume"))
        assertTrue("timers resumed", ops.calls.contains("resumeTimers"))
        assertTrue(
            "instance pause precedes its resume",
            ops.calls.indexOf("instancePause") < ops.calls.indexOf("instanceResume")
        )
        assertTrue(
            "timer pause precedes its resume",
            ops.calls.indexOf("pauseTimers") < ops.calls.indexOf("resumeTimers")
        )
    }

    @Test
    fun `resume without pause is a balanced no-op`() {
        val ops = RecordingOps()
        WebViewPauseCoordinator(ops, ops).resumeAll()
        // Resuming an unpaused webview must not leave anything paused: the
        // resume set is complete on its own.
        assertEquals(listOf("resumeTimers", "instanceResume"), ops.calls)
    }
}
