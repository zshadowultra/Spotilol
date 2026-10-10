package com.project.lol.webview

import android.content.ComponentCallbacks2
import org.junit.Assert.*
import org.junit.Test

/**
 * B3: trim-level -> action mapping.
 *
 * UI_HIDDEN and above -> RAM-only cache eviction (clearCache(false));
 * MODERATE and above -> also the window.__spotilol.onMemoryPressure JS hook.
 */
class TrimMemoryPolicyTest {

    @Test
    fun `UI_HIDDEN and above clears the RAM cache`() {
        assertTrue(TrimMemoryPolicy.shouldClearRamCache(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN))
        assertTrue(TrimMemoryPolicy.shouldClearRamCache(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND))
        assertTrue(TrimMemoryPolicy.shouldClearRamCache(ComponentCallbacks2.TRIM_MEMORY_MODERATE))
        assertTrue(TrimMemoryPolicy.shouldClearRamCache(ComponentCallbacks2.TRIM_MEMORY_COMPLETE))
    }

    @Test
    fun `running levels do not clear the RAM cache`() {
        assertFalse(TrimMemoryPolicy.shouldClearRamCache(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE))
        assertFalse(TrimMemoryPolicy.shouldClearRamCache(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW))
        assertFalse(TrimMemoryPolicy.shouldClearRamCache(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL))
    }

    @Test
    fun `JS pressure hook fires only at MODERATE and above`() {
        assertNull(TrimMemoryPolicy.pressureName(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL))
        assertNull(TrimMemoryPolicy.pressureName(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN))
        assertNull(TrimMemoryPolicy.pressureName(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND))
        assertEquals("moderate", TrimMemoryPolicy.pressureName(ComponentCallbacks2.TRIM_MEMORY_MODERATE))
        assertEquals("critical", TrimMemoryPolicy.pressureName(ComponentCallbacks2.TRIM_MEMORY_COMPLETE))
    }

    @Test
    fun `pressure JS guards on the hook and passes level and name`() {
        val js = TrimMemoryPolicy.buildPressureJs(60, "moderate")
        assertTrue("must type-check the hook", js.contains("typeof window.__spotilol.onMemoryPressure==='function'"))
        assertTrue("must pass level+name", js.contains("window.__spotilol.onMemoryPressure(60,'moderate')"))
        assertTrue("must never throw", js.startsWith("try{"))
    }

    @Test
    fun `pressure stub defines the hook without overriding a later implementation`() {
        assertTrue(MEMORY_PRESSURE_STUB_JS.contains("window.__spotilol = window.__spotilol || {}"))
        assertTrue(
            MEMORY_PRESSURE_STUB_JS.contains(
                "window.__spotilol.onMemoryPressure = window.__spotilol.onMemoryPressure || function(level, name)"
            )
        )
    }
}
