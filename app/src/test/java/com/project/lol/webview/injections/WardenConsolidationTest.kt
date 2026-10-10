package com.project.lol.webview.injections

import org.junit.Assert.*
import org.junit.Test

/**
 * C1: the seven warden intervals are consolidated into window.__splWarden.
 *
 * The injected code runs inside a WebView, so these tests assert structural
 * properties of the injected JS (registry shape, registration names, timer
 * cadence, idempotency guards) — a regression here means a part silently went
 * back to owning a private interval.
 */
class WardenConsolidationTest {

    private fun registrationsIn(content: String): Set<String> =
        Regex("""__splWardenAdd\('([A-Za-z]+)'""").findAll(content).map { it.groupValues[1] }.toSet()

    @Test
    fun `warden defines register-by-name registry`() {
        val js = PlayerCore.CONTENT
        assertTrue("__splWardenAdd missing", js.contains("window.__splWardenAdd = function(name, fn)"))
        // Overwrite-by-name: re-registration (e.g. on re-inject) replaces, never duplicates.
        assertTrue("registry must overwrite by name", js.contains("window.__splWardenReg[name] = fn"))
    }

    @Test
    fun `warden timer is created once and ticks every 5s`() {
        val js = PlayerCore.CONTENT
        assertTrue("warden must be created once (idempotent re-inject)", js.contains("if(!window.__splWarden)"))
        assertTrue("warden must tick every 5s", js.contains("},5000);"))
    }

    @Test
    fun `warden skips when backgrounded and calls every check`() {
        val js = PlayerCore.CONTENT
        val wardenBlock = js.substringAfter("if(!window.__splWarden)")
        assertTrue("warden must skip on __splBg", wardenBlock.contains("if(window.__splBg) return;"))
        assertTrue("warden must invoke registered checks", wardenBlock.contains("reg[name]()"))
        // One throwing check must not starve the others.
        assertTrue("check invocation must be exception-isolated", wardenBlock.contains("try{ reg[name](); }catch(e){}"))
    }

    @Test
    fun `all seven checks are registered`() {
        val names = registrationsIn(PlayerCore.CONTENT) +
            registrationsIn(MainLoop.CONTENT) +
            registrationsIn(AutoFeatures.CONTENT) +
            registrationsIn(CssHack.CONTENT) +
            registrationsIn(DownloadButton.CONTENT) +
            registrationsIn(SearchOverlay.CONTENT) +
            registrationsIn(CollectionDownload.CONTENT)
        assertEquals(
            setOf("splPf", "splAf", "splCss", "splCssMark", "splDlBtn", "splSearch", "splColDl"),
            names
        )
    }

    @Test
    fun `no part keeps its old private interval as the primary path`() {
        // The old unconditional creations must be gone; a private interval may
        // only survive inside the `else` fallback (warden missing) or as a
        // documented one-shot.
        assertPrivateIntervalsAreFallbackOrOneShot(
            "MainLoop", MainLoop.CONTENT,
            oneShots = setOf("var bootIv =", "var uIv=")
        )
        assertPrivateIntervalsAreFallbackOrOneShot("AutoFeatures", AutoFeatures.CONTENT)
        assertPrivateIntervalsAreFallbackOrOneShot("CssHack", CssHack.CONTENT)
        assertPrivateIntervalsAreFallbackOrOneShot("DownloadButton", DownloadButton.CONTENT)
        assertPrivateIntervalsAreFallbackOrOneShot("SearchOverlay", SearchOverlay.CONTENT)
        assertPrivateIntervalsAreFallbackOrOneShot(
            "CollectionDownload", CollectionDownload.CONTENT,
            oneShots = setOf("var iv =")
        )
        // The warden itself is the only new recurring interval.
        assertTrue("warden must exist", PlayerCore.CONTENT.contains("window.__splWarden = setInterval"))
    }

    private fun assertPrivateIntervalsAreFallbackOrOneShot(
        name: String,
        js: String,
        oneShots: Set<String> = emptySet()
    ) {
        var idx = js.indexOf("setInterval(")
        var found = 0
        while (idx >= 0) {
            found++
            val before = js.substring(maxOf(0, idx - 200), idx)
            val isFallback = before.contains("else")
            val isDocumentedOneShot = oneShots.any { before.trimEnd().endsWith(it) }
            assertTrue(
                "$name: private setInterval must be the warden-missing else branch or a documented one-shot",
                isFallback || isDocumentedOneShot
            )
            idx = js.indexOf("setInterval(", idx + 1)
        }
        assertTrue("$name: expected at least the fallback interval", found >= 1)
    }

    @Test
    fun `migrated parts fall back to a private interval only if the warden is missing`() {
        // Defensive: if PlayerCore's warden ever failed to install, each part keeps
        // working exactly as before instead of throwing on __splWardenAdd.
        for ((name, js) in listOf(
            "MainLoop" to MainLoop.CONTENT,
            "AutoFeatures" to AutoFeatures.CONTENT,
            "CssHack" to CssHack.CONTENT,
            "DownloadButton" to DownloadButton.CONTENT,
            "SearchOverlay" to SearchOverlay.CONTENT,
            "CollectionDownload" to CollectionDownload.CONTENT
        )) {
            assertTrue(
                "$name must guard on window.__splWardenAdd",
                js.contains("if(window.__splWardenAdd)")
            )
        }
    }
}
