package com.project.lol.webview.injections

import org.junit.Assert.*
import org.junit.Test

/**
 * C2: AndroidTracker's MutationObserver filters attribute callbacks to the
 * only attributes readTrackState() reads, so React class toggles and other
 * attribute churn no longer wake the debounced read.
 */
class TrackerAttributeFilterTest {

    @Test
    fun `observer has the exact attribute filter`() {
        assertTrue(
            "attributeFilter must list exactly the attributes readTrackState reads",
            AndroidTracker.CONTENT.contains("attributeFilter: ['aria-checked','aria-label','href','src']")
        )
    }

    @Test
    fun `filter is attached to the now-playing-bar observe call`() {
        val js = AndroidTracker.CONTENT
        val observe = js.substringAfter("obs.observe(npTarget")
        // The filter only takes effect when attributes:true is observed.
        assertTrue("must still observe attributes", observe.contains("attributes: true"))
        assertTrue("filter must be on the same observe call", observe.contains("attributeFilter:"))
        // The observe call must terminate (filter is part of the options object).
        assertTrue("options object must close", observe.contains("});"))
    }

    @Test
    fun `readTrackState reads only filtered attributes`() {
        val read = AndroidTracker.CONTENT.substringAfter("function readTrackState(){")
        // aria-checked (repeat + liked buttons); aria-label via
        // readTrackState -> splIsPlayingSticky() -> PlayerCore splIsPlaying();
        // src (cover art, read as im.src property).
        assertTrue(read.contains("getAttribute('aria-checked')"))
        assertTrue(
            "aria-label read via splIsPlayingSticky -> splIsPlaying",
            PlayerCore.CONTENT.contains("getAttribute('aria-label')")
        )
        assertTrue(read.contains("im.src"))
        // Every attribute read on observed nodes must be in the filter
        // (plus d/max/value, which are read but intentionally not observed:
        // 'd' is read off a freshly queried node, and max/value churn every
        // second with progress — observing them would defeat the filter).
        val attrs = Regex("""getAttribute\('([a-z-]+)'\)""").findAll(read).map { it.groupValues[1] }.toSet()
        val allowed = setOf("aria-checked", "aria-label", "href", "src", "d", "max", "value")
        assertTrue(
            "unexpected attribute reads: ${attrs - allowed}",
            attrs.all { it in allowed }
        )
    }
}
