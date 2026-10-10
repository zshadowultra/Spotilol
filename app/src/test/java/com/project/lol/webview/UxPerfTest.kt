package com.project.lol.webview

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX speed + lists (Worker 3, opt/webview-perf).
 *
 * Each test assembles a node driver from the JS actually embedded in the
 * Kotlin injection sources (plus assets/custom-ui/app.js) and runs it under
 * node. Drivers assert real behavior with the stdlib assert module; a
 * non-zero exit or missing PASS line fails the test. Skipped gracefully when
 * node is unavailable (CI without node).
 */
class UxPerfTest {

    @Test
    fun optimisticRollback_commandFails_iconReverts() {
        val out = NodeRunner.runDriver("optimistic.js")
        assertTrue(out.contains("PASS optimistic"))
    }

    @Test
    fun virtualizedWindowing_visibleRangeAndRecycling() {
        val out = NodeRunner.runDriver("virtualize.js")
        assertTrue(out.contains("PASS virtualize"))
    }

    @Test
    fun prefetchLru_evictionAndDwell() {
        val out = NodeRunner.runDriver("lru.js")
        assertTrue(out.contains("PASS lru"))
        val out2 = NodeRunner.runDriver("prefetch.js")
        assertTrue(out2.contains("PASS prefetch"))
    }

    @Test
    fun debounceTiming_coalescesRapidInput() {
        val out = NodeRunner.runDriver("debounce.js")
        assertTrue(out.contains("PASS debounce"))
    }

    @Test
    fun skeletonGeometry_matchesFinalRows() {
        val out = NodeRunner.runDriver("skeleton.js")
        assertTrue(out.contains("PASS skeleton"))
    }

    @Test
    fun onMemoryPressure_levelBehavior() {
        val out = NodeRunner.runDriver("mempressure.js")
        assertTrue(out.contains("PASS mempressure"))
    }

    @Test
    fun perfMarks_fireInOrderWithNonNegativeDeltas() {
        val out = NodeRunner.runDriver("perf.js")
        assertTrue(out.contains("PASS perf"))
    }
}
