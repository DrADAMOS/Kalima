package com.adamos.kalima.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.adamos.kalima.ui.theme.GameTheme
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * تخزين محلي لكل إعدادات اللعبة والتقدّم — فوق SharedPreferences.
 */
class Preferences(context: Context) {

    private val prefs = context.getSharedPreferences("kalima_prefs", Context.MODE_PRIVATE)

    // ---------- الثيم ----------
    var gameTheme: GameTheme
        get() = GameTheme.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { GameTheme.ORIGINAL }
        set(value) = prefs.edit { putInt(KEY_THEME, value.ordinal) }

    // ---------- الوضع الصعب ----------
    var hardMode: Boolean
        get() = prefs.getBoolean(KEY_HARD, false)
        set(value) = prefs.edit { putBoolean(KEY_HARD, value) }

    // ---------- شاشة التعليمات للمرة الأولى ----------
    var howToSeen: Boolean
        get() = prefs.getBoolean(KEY_HOW_TO_SEEN, false)
        set(value) = prefs.edit { putBoolean(KEY_HOW_TO_SEEN, value) }

    // ---------- المستوى العمري (0 مبتدئ، 1 متوسط، 2 متقدم) ----------
    var ageLevel: Int
        get() = prefs.getInt(KEY_AGE, 2)
        set(value) = prefs.edit { putInt(KEY_AGE, value.coerceIn(0, 2)) }

    // ---------- الإحصائيات ----------
    var gamesPlayed: Int
        get() = prefs.getInt(KEY_PLAYED, 0)
        private set(value) = prefs.edit { putInt(KEY_PLAYED, value) }

    var gamesWon: Int
        get() = prefs.getInt(KEY_WON, 0)
        private set(value) = prefs.edit { putInt(KEY_WON, value) }

    var currentStreak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        private set(value) = prefs.edit { putInt(KEY_STREAK, value) }

    var bestStreak: Int
        get() = prefs.getInt(KEY_BEST_STREAK, 0)
        private set(value) = prefs.edit { putInt(KEY_BEST_STREAK, value) }

    /** تحديث الإحصائيات بعد جولة (تجنب التكرار في نفس الجولة) */
    fun recordGameOnce(gameId: String, won: Boolean): Boolean {
        val recordedKey = "recorded_$gameId"
        if (prefs.getBoolean(recordedKey, false)) return false
        prefs.edit { putBoolean(recordedKey, true) }

        gamesPlayed += 1
        if (won) {
            gamesWon += 1
            currentStreak += 1
            if (currentStreak > bestStreak) bestStreak = currentStreak
        } else {
            currentStreak = 0
        }
        return true
    }

    /** تسجيل جولة غير يومية (أو بلا نهاية/عشوائي) */
    fun recordNonDailyGame(won: Boolean) {
        gamesPlayed += 1
        if (won) {
            gamesWon += 1
            currentStreak += 1
            if (currentStreak > bestStreak) bestStreak = currentStreak
        } else {
            currentStreak = 0
        }
    }

    // ---------- الشارات ----------
    fun unlockedBadges(): Set<String> =
        prefs.getStringSet(KEY_BADGES, emptySet())?.toSet() ?: emptySet()

    /** يرجع true إذا كانت الشارة جديدة */
    fun unlockBadge(id: String): Boolean {
        val current = unlockedBadges()
        if (id in current) return false
        prefs.edit { putStringSet(KEY_BADGES, current + id) }
        return true
    }

    val badgeNames = mapOf(
        "first_win" to "بطل البداية 🌱",
        "ten_wins" to "جامع النجوم 🌟",
        "no_hints" to "العبقري الصغير 🧠",
        "first_try" to "الهدّاف 🎯",
        "thirty_wins" to "محترف 💎",
        "pet_evolved" to "مربي الحيوانات 🐉",
        "daily_champ" to "بطل التحدي 🏆",
        "streak_3" to "شعلة الأسبوع 🔥",
        "all_lengths" to "ملك الكلمات 👑",
        "speed_five" to "البرق ⚡",
    )

    fun checkAndUnlockBadges(won: Boolean, attemptsCount: Int, hintsUsed: Int, dailyAllDone: Boolean): String? {
        var unlockedName: String? = null
        if (won) {
            if (unlockBadge("first_win")) unlockedName = badgeNames["first_win"]
            if (gamesWon >= 10 && unlockBadge("ten_wins")) unlockedName = badgeNames["ten_wins"]
            if (gamesWon >= 30 && unlockBadge("thirty_wins")) unlockedName = badgeNames["thirty_wins"]
            if (attemptsCount == 1 && unlockBadge("first_try")) unlockedName = badgeNames["first_try"]
            if (hintsUsed == 0 && unlockBadge("no_hints")) unlockedName = badgeNames["no_hints"]
            if (gamesWon >= 50 && unlockBadge("pet_evolved")) unlockedName = badgeNames["pet_evolved"]
            if (dailyAllDone && unlockBadge("daily_champ")) unlockedName = badgeNames["daily_champ"]
            if (currentStreak >= 3 && unlockBadge("streak_3")) unlockedName = badgeNames["streak_3"]
            if (dailyCompletedLengths().size >= 5 && unlockBadge("all_lengths")) unlockedName = badgeNames["all_lengths"]
            if (attemptsCount <= 2 && unlockBadge("speed_five")) unlockedName = badgeNames["speed_five"]
        }
        return unlockedName
    }

    // ---------- صندوق الكنز اليومي ----------
    fun canOpenChestToday(): Boolean =
        prefs.getString(KEY_CHEST_DATE, null) != todayKey()

    fun markChestOpened() {
        prefs.edit { putString(KEY_CHEST_DATE, todayKey()) }
    }

    // ---------- تحدي اليوم ----------
    private fun dailyKey(): String = "daily_${todayKey()}_"

    fun dailyCompletedLengths(): Set<Int> =
        prefs.getStringSet(dailyKey(), emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

    fun dailyCompletedCount(): Int = dailyCompletedLengths().size

    fun markDailyCompleted(length: Int) {
        val current = dailyCompletedLengths()
        prefs.edit {
            putStringSet(dailyKey(), (current + length).map { it.toString() }.toSet())
        }
    }

    fun dailyResultKey(length: Int): String = "daily_res_${todayKey()}_$length"

    fun isDailyFinished(length: Int): Boolean =
        prefs.getBoolean("daily_done_${todayKey()}_$length", false)

    fun getDailyResult(length: Int): Pair<Boolean, List<String>>? {
        val json = prefs.getString(dailyResultKey(length), null) ?: return null
        val parts = json.split("::")
        val won = parts.getOrNull(0)?.toBoolean() ?: false
        val attempts = parts.getOrNull(1)?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        return won to attempts
    }

    fun saveDailyResult(length: Int, won: Boolean, attempts: List<String>) {
        prefs.edit {
            putBoolean("daily_done_${todayKey()}_$length", true)
            putString(dailyResultKey(length), "$won::${attempts.joinToString(",")}")
        }
    }

    // ---------- الرقابة الأبوية ----------
    var kidMode: Boolean
        get() = prefs.getBoolean(KEY_KID_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_KID_MODE, value) }

    var safeDictionaryOnly: Boolean
        get() = prefs.getBoolean(KEY_SAFE_DICT, true)
        set(value) = prefs.edit { putBoolean(KEY_SAFE_DICT, value) }

    var dailyLimitMinutes: Int
        get() = prefs.getInt(KEY_DAILY_LIMIT, 0)
        set(value) = prefs.edit { putInt(KEY_DAILY_LIMIT, value) }

    /** هل تم ضبط PIN للرقابة الأبوية؟ */
    val hasParentalPin: Boolean get() = prefs.getString(KEY_PIN_HASH, null) != null

    /**
     * يضبط PIN جديد. يُخزَّن hash مع salt عشوائي، لا النص الصريح.
     * يرجع false إذا كان الـ PIN أقصر من 4 أرقام.
     */
    fun setParentalPin(pin: String): Boolean {
        val cleaned = pin.trim()
        if (cleaned.length < 4 || cleaned.any { !it.isDigit() }) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPinForTest(cleaned, salt)
        prefs.edit {
            putString(KEY_PIN_SALT, salt.joinToString(",") { it.toString() })
            putString(KEY_PIN_HASH, hash)
        }
        return true
    }

    /** يتحقق من PIN مُدخل. يقارن بزمن ثابت لتقليل تسريب التوقيت. */
    fun verifyParentalPin(pin: String): Boolean {
        val saltStr = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = saltStr.split(",").mapNotNull { it.toByteOrNull() }.toByteArray()
        if (salt.isEmpty()) return false
        return MessageDigest.isEqual(
            hashPinForTest(pin.trim(), salt).toByteArray(),
            expected.toByteArray(),
        )
    }

    /** يحذف PIN (يُستخدم عند تعطيل الرقابة الأبوية نهائيًا) */
    fun clearParentalPin() {
        prefs.edit {
            remove(KEY_PIN_HASH)
            remove(KEY_PIN_SALT)
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): String = hashPinForTest(pin, salt)

    /** دقائق اللعب اليوم */
    fun playedMinutesToday(): Int =
        prefs.getInt("play_${todayKey()}", 0)

    fun addPlayMinutes(minutes: Int) {
        val current = playedMinutesToday()
        prefs.edit { putInt("play_${todayKey()}", current + minutes) }
    }

    /** هل يمكن اللعب الآن؟ */
    fun canPlayNow(): Boolean {
        if (!kidMode) return true
        val limit = dailyLimitMinutes
        if (limit <= 0) return true
        return playedMinutesToday() < limit
    }

    companion object {
        fun todayKey(): String {
            val cal = java.util.Calendar.getInstance()
            val y = cal.get(java.util.Calendar.YEAR)
            val m = cal.get(java.util.Calendar.MONTH) + 1
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
            return "%04d-%02d-%02d".format(y, m, d)
        }

        private const val KEY_THEME = "game_theme"
        private const val KEY_HARD = "hard_mode"
        private const val KEY_HOW_TO_SEEN = "how_to_seen"
        private const val KEY_AGE = "age_level"
        private const val KEY_PLAYED = "games_played"
        private const val KEY_WON = "games_won"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_BADGES = "unlocked_badges"
        private const val KEY_CHEST_DATE = "last_chest_date"
        private const val KEY_KID_MODE = "kid_mode"
        private const val KEY_SAFE_DICT = "safe_dictionary"
        private const val KEY_DAILY_LIMIT = "daily_limit_minutes"
        private const val KEY_PIN_HASH = "parental_pin_hash"
        private const val KEY_PIN_SALT = "parental_pin_salt"
    }
}

/** مكشوف للاختبارات الوحدية فقط — لا يُستخدم في كود الإنتاج */
@VisibleForTesting
internal fun hashPinForTest(pin: String, salt: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(salt)
    // 10,000 تكرار لإبطاء التخمين بالقوة الغاشمة على PIN قصير
    var bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
    repeat(10_000) {
        digest.reset()
        bytes = digest.digest(bytes + salt)
    }
    return bytes.joinToString("") { "%02x".format(it) }
}

