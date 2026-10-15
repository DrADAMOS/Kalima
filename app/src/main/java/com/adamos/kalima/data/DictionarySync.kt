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
            parseWords(cached)?.let(Dictionary::applyOnlineWords)
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
                val words = parseWords(body)
                if (words == null || words.isEmpty()) {
                    Log.w(TAG, "Dictionary refresh rejected: invalid or empty snapshot")
                    return@execute
                }
                prefs.edit().putString(CACHE_KEY, body).apply()
                Dictionary.applyOnlineWords(words)
                Log.i(TAG, "Dictionary refreshed successfully: ${words.size} words")
            } catch (e: Exception) {
                Log.w(TAG, "Dictionary refresh failed; bundled/cached words remain available", e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun parseWords(body: String): Set<String>? {
        return try {
            val root = JSONObject(body)
            if (root.optInt("schemaVersion", -1) != 1) return null
            val words = LinkedHashSet<String>()
            addWords(root.optJSONArray("answers"), words)
            addWords(root.optJSONArray("guesses"), words)
            words.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun addWords(array: JSONArray?, destination: MutableSet<String>) {
        if (array == null) return
        for (i in 0 until minOf(array.length(), 50_000)) {
            val word = array.optString(i, "").trim()
            if (Dictionary.isValidGuess(word)) destination.add(word)
        }
    }
}
