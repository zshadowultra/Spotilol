package com.project.lol.webview.injections

import org.junit.Assert.*
import org.junit.Test

/**
 * C8: search-as-you-type waits 250 ms after the last keystroke before hitting
 * the Spotify API.
 */
class SearchDebounceTest {

    @Test
    fun `search input is debounced at 250ms`() {
        val js = SearchOverlay.CONTENT
        assertTrue(
            "onInput must debounce doSearch at 250ms",
            js.contains("debTimer = setTimeout(function(){ doSearch(v); }, 250);")
        )
    }

    @Test
    fun `old 220ms debounce is gone`() {
        assertFalse(
            "stale 220ms debounce must not remain",
            SearchOverlay.CONTENT.contains("}, 220);")
        )
    }

    @Test
    fun `pending debounce is cancelled on each keystroke`() {
        val js = SearchOverlay.CONTENT
        val onInput = js.substringAfter("function onInput(){")
        val clearIdx = onInput.indexOf("clearTimeout(debTimer);")
        val setIdx = onInput.indexOf("debTimer = setTimeout(function(){ doSearch(v); }, 250);")
        assertTrue("onInput must clear the pending timer", clearIdx >= 0)
        assertTrue("onInput must arm a fresh timer", setIdx >= 0)
        // Cancel-before-arm: a fast typist never stacks API calls.
        assertTrue("clearTimeout must precede setTimeout", clearIdx < setIdx)
    }

    @Test
    fun `empty query cancels the debounce and shows recents`() {
        val js = SearchOverlay.CONTENT
        val onInput = js.substringAfter("function onInput(){")
        val emptyBranch = onInput.substringAfter("if(!v){")
        assertTrue("empty query must cancel pending search", emptyBranch.contains("clearTimeout(debTimer)"))
        assertTrue("empty query must fall back to recent searches", emptyBranch.contains("doRecent()"))
    }
}
