package com.project.lol.webview

import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assume

/**
 * Runs the node-based UX-perf drivers (app/src/test/node/drivers/, one .js per
 * driver) against
 * the JS actually embedded in the Kotlin injection sources. Used by UxPerfTest.
 * Local equivalent without Gradle: node app/src/test/node/run-local.js
 */
object NodeRunner {

    private fun baseDir(): File {
        val candidates = listOf(
            File("app"), // gradle rootDir
            File(".")    // module dir (app/)
        )
        for (c in candidates) {
            if (File(c, "src/main/java/com/project/lol/webview/injections/PlaybackControls.kt").exists()) return c
        }
        throw IllegalStateException("cannot locate app sources from ${File(".").absolutePath}")
    }

    private val injDir: File get() = File(baseDir(), "src/main/java/com/project/lol/webview/injections")
    private val nodeDir: File get() = File(baseDir(), "src/test/node")

    fun assumeNode() {
        val ok = try {
            val p = ProcessBuilder("node", "--version").start()
            p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0
        } catch (e: Exception) { false }
        Assume.assumeTrue("node is required for UX-perf JS tests", ok)
    }

    private fun contentJs(ktName: String): String {
        val kt = File(injDir, ktName).readText()
        val m = Regex("const val CONTENT = \"\"\"\n([\\s\\S]*)\n    \"\"\"").find(kt)
            ?: throw IllegalStateException("CONTENT not found in $ktName")
        return m.groupValues[1]
    }

    /** Brace-aware extraction of a function starting at [marker]. */
    private fun extractFn(src: String, marker: String): String {
        val i = src.indexOf(marker)
        require(i >= 0) { "marker not found: $marker" }
        var j = src.indexOf('{', i)
        var depth = 0
        var inStr: Char? = null
        var esc = false
        var lineComment = false
        var blockComment = false
        while (j < src.length) {
            val c = src[j]
            when {
                lineComment -> if (c == '\n') lineComment = false
                blockComment -> if (c == '*' && j + 1 < src.length && src[j + 1] == '/') { blockComment = false; j++ }
                inStr != null -> when {
                    esc -> esc = false
                    c == '\\' -> esc = true
                    c == inStr -> inStr = null
                }
                c == '/' && j + 1 < src.length && src[j + 1] == '/' -> { lineComment = true; j++ }
                c == '/' && j + 1 < src.length && src[j + 1] == '*' -> { blockComment = true; j++ }
                c == '"' || c == '\'' || c == '`' -> inStr = c
                c == '{' -> depth++
                c == '}' -> {
                    depth--
                    if (depth == 0) return src.substring(i, j + 1)
                }
            }
            j++
        }
        throw IllegalStateException("unbalanced braces for $marker")
    }

    private fun jsUnescape(s: String): String =
        s.replace("\\\\", "\u0000").replace("\\'", "'").replace("\u0000", "\\")

    private fun assemble(driver: String): String {
        val pc = contentJs("PlaybackControls.kt")
        val a = pc.indexOf("/*__SPLUX_START__*/")
        val b = pc.indexOf("/*__SPLUX_END__*/") + "/*__SPLUX_END__*/".length
        val splux = pc.substring(a, b)

        val sp = contentJs("SpotilolPlayer.kt")
        val paintFn = extractFn(sp, "window.splPaintPlayIcon=function(playing)")

        val cui = contentJs("CustomUI.kt")
        val cuiFns = listOf(
            "skelRecentCard", "realRecentCard", "skelReleaseCard", "realReleaseCard",
            "skelSongRow", "libSongRow", "splArtInto", "splDwell"
        ).joinToString("\n") { extractFn(cui, "function $it(") }

        val appjs = File(baseDir(), "src/main/assets/custom-ui/app.js").readText()
        val appSkel = extractFn(appjs, "function skelSongRow(")
            .replace("function skelSongRow(", "function appSkelSongRow(")

        val cssM = Regex("""sk\.textContent=((?:'[^']*'\s*\+\s*)*'[^']*');""").find(cui)
            ?: throw IllegalStateException("skeleton CSS not found")
        val cssParts = Regex("'((?:[^'\\\\]|\\\\.)*)'").findAll(cssM.groupValues[1])
            .map { jsUnescape(it.groupValues[1]) }.toList()
        val skelCss = cssParts.joinToString("")

        var body = File(nodeDir, "drivers/$driver").readText()
        fun sub(token: String, value: String) {
            val re = Regex("^$token$", RegexOption.MULTILINE)
            require(re.containsMatchIn(body)) { "placeholder missing: $token" }
            // quoteReplacement: $ and \ in value must stay literal, not act as
            // group references / escapes in appendReplacement semantics.
            body = re.replaceFirst(body, java.util.regex.Matcher.quoteReplacement(value))
        }
        sub("__SPLUX__", splux)
        if (Regex("^__PAINT__$", RegexOption.MULTILINE).containsMatchIn(body)) sub("__PAINT__", paintFn)
        if (Regex("^__CUSTOMUI_FNS__$", RegexOption.MULTILINE).containsMatchIn(body)) sub("__CUSTOMUI_FNS__", cuiFns)
        if (Regex("^__APPSKELETON__$", RegexOption.MULTILINE).containsMatchIn(body)) sub("__APPSKELETON__", appSkel)
        body = body.replace("__SKELCSS__", jsQuote(skelCss))
        return File(nodeDir, "prelude.js").readText() + "\n" + body
    }

    /** Minimal JSON.stringify(s) equivalent: a double-quoted JS string literal. */
    private fun jsQuote(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    /** Assembles and runs a driver; returns stdout. Throws on failure. */
    fun runDriver(driver: String): String {
        assumeNode()
        val js = assemble(driver)
        val tmp = File.createTempFile("uxdrv-", ".js")
        try {
            tmp.writeText(js)
            val syntax = ProcessBuilder("node", "--check", tmp.absolutePath).start()
            require(syntax.waitFor(30, TimeUnit.SECONDS) && syntax.exitValue() == 0) {
                "node --check failed for $driver"
            }
            val p = ProcessBuilder("node", tmp.absolutePath).redirectErrorStream(true).start()
            val out = p.inputStream.bufferedReader().readText()
            require(p.waitFor(30, TimeUnit.SECONDS)) { "node timed out for $driver" }
            require(p.exitValue() == 0) { "driver $driver FAILED:\n$out" }
            require("PASS" in out) { "driver $driver produced no PASS:\n$out" }
            return out
        } finally {
            tmp.delete()
        }
    }
}
