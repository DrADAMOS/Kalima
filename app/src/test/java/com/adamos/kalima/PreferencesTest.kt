package com.adamos.kalima

import android.content.SharedPreferences
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.theme.GameTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesTest {

    @Test
    fun settingGameThemeUpdatesThemeStateFlow() {
        val fakePrefs = FakeSharedPreferences()
        val preferences = Preferences(fakePrefs)

        preferences.gameTheme = GameTheme.SPACE
        assertEquals(GameTheme.SPACE, preferences.gameTheme)
        assertEquals(GameTheme.SPACE, preferences.themeState.value)

        preferences.gameTheme = GameTheme.CANDY
        assertEquals(GameTheme.CANDY, preferences.gameTheme)
        assertEquals(GameTheme.CANDY, preferences.themeState.value)
    }
}

/** In-memory fake SharedPreferences for JVM unit testing */
private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values
    override fun getString(key: String, defValue: String?): String? = (values[key] as? String) ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (values[key] as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: defValues
    override fun getInt(key: String, defValue: Int): Int = (values[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long): Long = (values[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (values[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = (values[key] as? Boolean) ?: defValue
    override fun contains(key: String): Boolean = key in values

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class Editor : SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { changes[key] = value }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = apply { changes[key] = values }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { changes[key] = value }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { changes[key] = value }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { changes[key] = value }
        override fun remove(key: String): SharedPreferences.Editor = apply { changes[key] = null }
        override fun clear(): SharedPreferences.Editor = apply { clearAll = true }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) values.clear()
            for ((key, value) in changes) {
                if (value == null) values.remove(key) else values[key] = value
            }
        }
    }
}
