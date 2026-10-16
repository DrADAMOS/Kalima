package com.adamos.kalima.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Offline-first dictionary refresh. The bundled JSON is always available; this
 * updater optionally adds words from the public GitHub snapshot and caches the
 * last valid response for the next launch.
 */
object DictionarySync {
    private const val TAG = "KalimaDictionarySync"
    private const val PREFS_NAME = "kalima_dictionary_sync"
    private const val CACHE_KEY = "snapshot_v1"
    private const val DICTIONARY_URL =
        "https://raw.githubusercontent.com/DrADAMOS/Kalima/main/dictionary/remote-dictionary-v1.json"

    private val started = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Kalima-Dictionary-Sync").apply { isDaemon = true }
    }

    fun initialize(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Use the last successful response immediately, even without connectivity.
        prefs.getString(CACHE_KEY, null)?.let { cached ->
            parseWords(cached)?.let { (_, words) -> Dictionary.applyOnlineWords(words) }
        }

        if (!started.compareAndSet(false, true)) return
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(DICTIONARY_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 12_000
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Kalima-Android")
                }
                val code = connection.responseCode
                if (code !in 200..299) {
                    Log.w(TAG, "Dictionary refresh failed: HTTP $code")
                    return@execute
                }
                val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val parsed = parseWords(body, prefs.getString(KEY_VERSION, null))
                if (parsed == null) {
                    Log.w(TAG, "Dictionary refresh rejected: invalid, stale or empty snapshot")
                    return@execute
                }
                val (version, words) = parsed
                prefs.edit().putString(CACHE_KEY, body).putString(KEY_VERSION, version).apply()
                Dictionary.applyOnlineWords(words)
                Log.i(TAG, "Dictionary refreshed successfully: ${words.size} words (v$version)")
            } catch (e: Exception) {
                Log.w(TAG, "Dictionary refresh failed; bundled/cached words remain available", e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    private const val MAX_ACCEPTED_WORDS = 50_000
    private const val MAX_INVALID_RATIO = 0.05
    private const val KEY_VERSION = "snapshot_version"

    /**
     * يتحقق من اللقطة قبل قبولها: schemaVersion = 1، وحقل version رقمي لا يقل عن
     * المخزَّن، وعدد الكلمات المقبولة محدود، ونسبة المدخلات غير الصالحة لا تتجاوز 5%.
     * الكلمات المحجوبة تُستبعد بصمت. يرجع null عند الرفض.
     */
    private fun parseWords(body: String, minVersion: String? = null): Pair<String, Set<String>>? {
        return try {
            val root = JSONObject(body)
            if (root.optInt("schemaVersion", -1) != 1) return null
            val version = root.optString("version", "")
            if (!Regex("""\d{4}-\d{2}-\d{2}(\.\d+)?""").matches(version)) return null
            if (minVersion != null && compareVersions(version, minVersion) < 0) return null

            val words = LinkedHashSet<String>()
            var total = 0
            var invalid = 0
            for (key in listOf("answers", "guesses")) {
                val array = root.optJSONArray(key) ?: continue
                for (i in 0 until minOf(array.length(), MAX_ACCEPTED_WORDS)) {
                    val word = array.optString(i, "").trim()
                    total++
                    if (!Dictionary.isValidGuess(word) || !Dictionary.isPlausibleExpandedEntry(word)) {
                        invalid++
                    } else if (Dictionary.isSafe(word)) {
                        words.add(word)
                    }
                }
            }
            if (words.isEmpty() || words.size > MAX_ACCEPTED_WORDS) return null
            if (total > 0 && invalid.toDouble() / total > MAX_INVALID_RATIO) return null
            version to words
        } catch (_: Exception) {
            null
        }
    }

    /** مقارنة أرقام النسخة «yyyy-MM-dd.n» رقميًا. */
    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split('-', '.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('-', '.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }
}
