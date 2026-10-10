package com.project.lol.webview

import org.junit.Assert.*
import org.junit.Test

/**
 * C4: the ~275KB post-login payload is split into an immediate core chunk and
 * a deferred chunk (downloads UI, PlaylistSort, SearchOverlay).
 *
 * These tests assert structural properties of the split: exact deferred
 * membership, no core->deferred load-time dependency, core evaluated before
 * deferred, and the idle scheduler shape. A regression here means a chunk
 * silently moved across the split boundary.
 */
class PayloadChunkingTest {

    private fun chunks(mode: String) =
        assemblePlayerChunks(flagsJs = "/*flags*/", recAccountJs = "/*rec*/", playerMode = mode)

    @Test
    fun `deferred set is exactly the four planned chunks`() {
        val names = chunks("customui").map { it.name }.toSet()
        assertEquals(
            setOf("SearchOverlay", "CollectionDownload", "ContextMenuDownload", "PlaylistSort"),
            names.intersect(DEFERRED_CHUNK_NAMES)
        )
        assertEquals(DEFERRED_CHUNK_NAMES, names.intersect(DEFERRED_CHUNK_NAMES))
    }

    @Test
    fun `core carries the player-critical modules`() {
        val (core, _) = splitPlayerPayload(chunks("customui"))
        assertTrue("MainLoop entry", core.contains("window.firstFuck"))
        assertTrue("AutoFeatures entry", core.contains("window.addAutoFeatures"))
        assertTrue("CssHack entry", core.contains("window.addCSSJSHack"))
        assertTrue("PlayerCore warden", core.contains("__splWarden"))
        assertTrue("TrackObserver", core.contains("track"))
        assertTrue("flags chunk", core.contains("/*flags*/"))
        assertTrue("account reporter", core.contains("/*rec*/"))
    }

    @Test
    fun `no deferred module leaks into the core chunk`() {
        val (core, deferred) = splitPlayerPayload(chunks("customui"))
        assertFalse(core.contains("window.splPlaylistSort ="))
        assertFalse(core.contains("window.splSearchInit"))
        assertFalse(core.contains("__splColDlInit"))
        assertFalse(core.contains("__splCtxDlInit"))
        assertTrue(deferred.contains("window.splPlaylistSort ="))
        assertTrue(deferred.contains("window.splSearchInit"))
    }

    @Test
    fun `split is lossless - every chunk lands in exactly one payload`() {
        val all = chunks("spotilol")
        val (core, deferred) = splitPlayerPayload(all)
        // Each non-empty chunk's stripped content appears in exactly one side.
        for (c in all) {
            if (c.js.isBlank()) continue
            val stripped = com.project.lol.webview.helpers.JsUtils.stripConsoleLogs(c.js)
            val inCore = core.contains(stripped.take(120))
            val inDeferred = deferred.contains(stripped.take(120))
            assertTrue("chunk ${c.name} lost from both payloads", inCore || inDeferred)
            assertFalse("chunk ${c.name} in both payloads", inCore && inDeferred)
        }
    }

    @Test
    fun `player engine chunk follows the player mode`() {
        val (coreSpotilol, _) = splitPlayerPayload(chunks("spotilol"))
        val (coreCustom, _) = splitPlayerPayload(chunks("customui"))
        assertTrue("spotilol engine immediate", coreSpotilol.contains("initSpotilolPlayer"))
        assertFalse("customui engine not in spotilol build", coreSpotilol.contains("__splCustomUiLoaded"))
        assertTrue("customui engine immediate", coreCustom.contains("__splCustomUiLoaded"))
        assertFalse("spotilol engine not in customui build", coreCustom.contains("initSpotilolPlayer"))
        // The engine chunks are never deferred.
        val (_, deferredSpotilol) = splitPlayerPayload(chunks("spotilol"))
        assertFalse(deferredSpotilol.contains("initSpotilolPlayer"))
    }

    @Test
    fun `core is evaluated before deferred`() {
        val calls = mutableListOf<String>()
        injectSplitPayload(
            evaluate = { calls += it },
            coreJs = "/*CORE*/",
            themeJs = "/*THEME*/",
            deferredJs = "/*DEFERRED*/",
            playerMode = "customui"
        )
        assertEquals("two evaluate calls", 2, calls.size)
        assertTrue("first call is the core payload", calls[0].contains("/*CORE*/"))
        assertTrue("first call carries theme", calls[0].contains("/*THEME*/"))
        assertFalse("deferred not in first call", calls[0].contains("requestIdleCallback"))
        assertTrue("second call is the idle scheduler", calls[1].contains("requestIdleCallback"))
        assertFalse("core not re-evaluated second", calls[1].contains("/*CORE*/"))
    }

    @Test
    fun `original player mode appends the now-playing-bar style to core`() {
        val calls = mutableListOf<String>()
        injectSplitPayload(
            evaluate = { calls += it },
            coreJs = "/*CORE*/",
            themeJs = "",
            deferredJs = "",
            playerMode = "original"
        )
        assertTrue(calls[0].contains("spl-np-show"))
    }

    @Test
    fun `scheduler uses requestIdleCallback with a 3s timeout and setTimeout fallback`() {
        val js = buildDeferredSchedulerJs("var x = 1;")
        assertTrue(js.contains("window.requestIdleCallback"))
        assertTrue(js.contains("{timeout: 3000}"))
        assertTrue(js.contains("setTimeout(run, 3000)"))
        assertTrue("one schedule per page", js.contains("if (window.__splDeferredScheduled) return;"))
        assertTrue("runs once", js.contains("if (window.__splDeferredRan) return;"))
        assertTrue("global-scope eval", js.contains("(0,eval)(src)"))
    }

    @Test
    fun `scheduler embeds the payload as an escaped JSON string`() {
        val payload = "var s = \"a'b\\\"c\\\\d\"; var re = /x\\/y/;"
        val js = buildDeferredSchedulerJs(payload)
        // The raw payload must NOT appear unescaped (that would break the
        // string literal the scheduler evals).
        assertFalse("raw payload must not appear verbatim", js.contains(payload))
        assertTrue("escaped content present", js.contains("\\\""))
    }

    @Test
    fun `jsonQuote escapes quotes backslashes and control chars`() {
        assertEquals("\"a\"", jsonQuote("a"))
        assertEquals("\"say \\\"hi\\\"\"", jsonQuote("say \"hi\""))
        assertEquals("\"a\\\\b\"", jsonQuote("a\\b"))
        assertEquals("\"a\\nb\\tc\"", jsonQuote("a\nb\tc"))
        assertEquals("\"\\u0001\"", jsonQuote("\u0001"))
        assertEquals("\"<\\/script>\"", jsonQuote("</script>"))
    }
}
