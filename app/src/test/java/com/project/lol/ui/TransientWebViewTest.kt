package com.project.lol.ui

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * A2: transient WebViews are destroy()ed on ALL paths (happy path, block
 * throws, destroy() itself throws), the budget counter stays consistent, and
 * >1 live WebView is signaled via overBudget.
 */
class TransientWebViewTest {

    private class FakeTransientWebView : TransientWebView {
        var destroyed = false
        var clears = 0
        override fun clearCache(includeDiskFiles: Boolean) { clears++ }
        override fun clearHistory() { clears++ }
        override fun clearFormData() { clears++ }
        override fun destroy() { destroyed = true }
    }

    @Before
    fun resetBudget() {
        WebViewBudget.resetForTests()
    }

    @Test
    fun `destroy runs on the happy path and the budget returns to zero`() {
        val fake = FakeTransientWebView()
        val res = useTransientWebView(create = { fake }) {
            it.clearCache(true)
            it.clearHistory()
            "ok"
        }
        assertEquals("ok", res.value)
        assertFalse("no second webview alive", res.overBudget)
        assertTrue("destroy called", fake.destroyed)
        assertEquals(2, fake.clears)
        assertEquals(0, WebViewBudget.liveCount)
    }

    @Test
    fun `destroy runs when the block throws`() {
        val fake = FakeTransientWebView()
        try {
            useTransientWebView(create = { fake }) {
                throw IllegalStateException("boom")
            }
            fail("block exception must propagate")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }
        assertTrue("destroy called despite block failure", fake.destroyed)
        assertEquals("budget consistent after block failure", 0, WebViewBudget.liveCount)
    }

    @Test
    fun `budget stays consistent when destroy itself throws`() {
        val bad = object : TransientWebView {
            override fun clearCache(includeDiskFiles: Boolean) {}
            override fun clearHistory() {}
            override fun clearFormData() {}
            override fun destroy() = throw RuntimeException("destroy boom")
        }
        try {
            useTransientWebView(create = { bad }) { }
            fail("destroy exception must propagate")
        } catch (e: RuntimeException) {
            assertEquals("destroy boom", e.message)
        }
        assertEquals("budget consistent after destroy failure", 0, WebViewBudget.liveCount)
    }

    @Test
    fun `budget stays consistent when create throws`() {
        try {
            useTransientWebView(create = { throw IllegalStateException("no webview") }) { }
            fail("create exception must propagate")
        } catch (e: IllegalStateException) {
            // expected
        }
        assertEquals(0, WebViewBudget.liveCount)
    }

    @Test
    fun `overBudget signals when a second webview is alive`() {
        WebViewBudget.noteCreated("main")
        val res = useTransientWebView(create = { FakeTransientWebView() }) { 42 }
        assertEquals(42, res.value)
        assertTrue("must flag >1 live webview", res.overBudget)
        assertEquals("main still counted", 1, WebViewBudget.liveCount)
        WebViewBudget.noteDestroyed()
        assertEquals(0, WebViewBudget.liveCount)
    }

    @Test
    fun `budget never goes negative`() {
        WebViewBudget.noteDestroyed()
        WebViewBudget.noteDestroyed()
        assertEquals(0, WebViewBudget.liveCount)
    }

    @Test
    fun `noteCreated flags only above one live webview`() {
        assertFalse(WebViewBudget.noteCreated("first"))
        assertTrue(WebViewBudget.noteCreated("second"))
        WebViewBudget.noteDestroyed()
        WebViewBudget.noteDestroyed()
    }
}
