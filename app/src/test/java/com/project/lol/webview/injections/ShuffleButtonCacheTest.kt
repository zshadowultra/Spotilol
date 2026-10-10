package com.project.lol.webview.injections

import org.junit.Assert.*
import org.junit.Test

/**
 * C3: splShuffleBtn() caches the resolved button and re-resolves only when the
 * cached node left the DOM. Steady-state cost per call drops from a
 * full-document button scan to one isConnected check.
 */
class ShuffleButtonCacheTest {

    @Test
    fun `resolved button is cached on window`() {
        val js = PlaybackControls.CONTENT
        assertTrue("cache slot must exist", js.contains("window.__splShuffleBtn"))
        assertTrue(
            "successful resolve must populate the cache",
            js.contains("if(b) window.__splShuffleBtn = b;")
        )
    }

    @Test
    fun `cache is returned when still connected`() {
        val js = PlaybackControls.CONTENT
        val fn = js.substringAfter("window.splShuffleBtn = function() {")
        assertTrue(
            "must re-resolve only when the cached node left the DOM",
            fn.contains("if(c && c.isConnected) return c;")
        )
    }

    @Test
    fun `cache is consulted before any DOM scan`() {
        val js = PlaybackControls.CONTENT
        val fn = js.substringAfter("window.splShuffleBtn = function() {")
        val cacheRead = fn.indexOf("window.__splShuffleBtn;")
        val firstScan = fn.indexOf("document.querySelector('button[data-testid=\"control-button-shuffle\"]')")
        val fullScan = fn.indexOf("document.querySelectorAll('button')")
        assertTrue("cache read must exist", cacheRead >= 0)
        assertTrue("testid lookup must exist", firstScan >= 0)
        assertTrue("full-document fallback scan must exist", fullScan >= 0)
        assertTrue("cache must be consulted before the testid scan", cacheRead < firstScan)
        assertTrue("testid scan must precede the full-document scan", firstScan < fullScan)
    }

    @Test
    fun `miss is not cached and returns null`() {
        val js = PlaybackControls.CONTENT
        val fn = js.substringAfter("window.splShuffleBtn = function() {")
        // A miss must not poison the cache: next call retries the resolve.
        assertTrue("returns null on miss", fn.contains("return b || null;"))
        assertFalse(
            "must not cache a null resolve",
            fn.contains("window.__splShuffleBtn = b || null") ||
                fn.contains("window.__splShuffleBtn=null")
        )
    }

    @Test
    fun `resolution order is unchanged`() {
        val js = PlaybackControls.CONTENT
        val fn = js.substringAfter("window.splShuffleBtn = function() {")
        val testid = fn.indexOf("control-button-shuffle")
        val skipBack = fn.indexOf("control-button-skip-back")
        val svgScan = fn.indexOf("M13.151.922")
        assertTrue(testid < skipBack && skipBack < svgScan)
    }
}
