package com.project.lol.webview.injections

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import org.json.JSONObject

/**
 * TestAuth - seeds a baked-in Spotify session for automated testing.
 *
 * Reads app/src/main/assets/test-cookies.json (ONLY present in test builds):
 *   { "cookies": [ {"name": "...", "value": "...", "domain": ".spotify.com"}, ... ] }
 *
 * Seeds them via CookieManager, flips the LoggedIn pref, and returns true so
 * the caller can reload the page into an authenticated session. Returns false
 * immediately when the asset is absent (production builds) — zero behavior
 * change for real users.
 */
object TestAuth {
    private const val TAG = "TestAuth"

    fun trySeedTestSession(context: Context): Boolean {
        val json = try {
            context.assets.open("test-cookies.json").bufferedReader().readText()
        } catch (e: Exception) {
            return false // no test asset -> production behavior
        }
        return try {
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("cookies")
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            var count = 0
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                val name = c.getString("name")
                val value = c.getString("value")
                val domain = c.optString("domain", ".spotify.com")
                // CookieManager.setCookie(url, "name=value; Domain=...; Path=/")
                cm.setCookie("https://open.spotify.com",
                    "$name=$value; Domain=$domain; Path=/; Secure; SameSite=None")
                count++
            }
            cm.flush()
            context.getSharedPreferences("spotilol_prefs", 0)
                .edit().putBoolean("LoggedIn", true).apply()
            Log.i(TAG, "seeded $count test cookies, LoggedIn=true")
            true
        } catch (e: Exception) {
            Log.w(TAG, "test cookie seeding failed: ${e.message}")
            false
        }
    }
}
