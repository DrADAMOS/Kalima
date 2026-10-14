package com.adamos.kalima.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Optional online dictionary updater.
 *
 * Built-in words remain available offline. A validated remote snapshot is cached
 * locally and reused on later launches; network failures never block gameplay.
 */
object DictionarySync {
    private const val PREFS_NAME = "kalima_dictionary_cache"
    private const val CACHE_KEY = "dictionary_json_v1"
    private const val DICTIONARY_URL =
        "https://raw.githubusercontent.com/DrADAMOS/Kalima/main/dictionary/remote-dictionary-v1.json"

    private val started = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Kalima-Dictionary-Sync").apply { isDaemon = true }
    }

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load the last known good snapshot immediately; this path is offline-safe.
        prefs.getString(CACHE_KEY, null)?.let { cached ->
            parseSnapshot(cached)?.let { (answers, guesses) ->
                Dictionary.applyRemoteWords(answers, guesses)
            }
        }

        if (!started.compareAndSet(false, true)) return
        executor.execute {
            try {
                val connection = (URL(DICTIONARY_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 12_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Kalima-Android")
                }
                try {
                    if (connection.responseCode !in 200..299) return@execute
                    val payload = connection.inputStream
                        .bufferedReader(StandardCharsets.UTF_8)
                        .use { it.readText() }
                    val parsed = parseSnapshot(payload) ?: return@execute
                    // Cache only a structurally valid snapshot, then atomically apply it.
                    prefs.edit().putString(CACHE_KEY, payload).apply()
                    Dictionary.applyRemoteWords(parsed.first, parsed.second)
                } finally {
                    connection.disconnect()
                }
            } catch (_: Exception) {
                // Deliberately silent: offline gameplay must not be interrupted.
            }
        }
    }

    private fun parseSnapshot(payload: String): Pair<List<String>, List<String>>? {
        return try {
            val root = JSONObject(payload)
            if (root.optInt("schemaVersion", -1) != 1) return null
            val answers = readWords(root.optJSONArray("answers"))
            val guesses = readWords(root.optJSONArray("guesses"))
            if (answers.isEmpty() && guesses.isEmpty()) return null
            answers to guesses
        } catch (_: Exception) {
            null
        }
    }

    private fun readWords(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val result = LinkedHashSet<String>()
        val limit = minOf(array.length(), 50_000)
        for (index in 0 until limit) {
            val word = array.optString(index, "").trim()
            if (Dictionary.isValidGuess(word)) result.add(word)
        }
        return result.toList()
    }
}
