package com.adamos.kalima.data

/** شارة إنجاز */
data class Badge(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val isSecret: Boolean = false,
)

/** مرحلة تطور الكائن */
data class PetStage(
    val emoji: String,
    val name: String,
    val winsRequired: Int,
)

/**
 * نظام المكافآت — الشارات والكائن المتطور وصندوق الكنز اليومي.
 */
object Rewards {

    val allBadges: List<Badge> = listOf(
        Badge("first_win", "بطل البداية", "🌱", "أول كلمة تُحل"),
        Badge("ten_wins", "جامع النجوم", "🌟", "حل 10 كلمات"),
        Badge("no_hints", "العبقري الصغير", "🧠", "فوز بدون تلميحات"),
        Badge("first_try", "الهدّاف", "🎯", "فوز من أول محاولة"),
        Badge("streak_3", "شعلة الأسبوع", "🔥", "3 أيام متتالية"),
        Badge("all_lengths", "ملك الكلمات", "👑", "حل جميع الأطوال"),
        Badge("thirty_wins", "محترف", "💎", "30 فوزًا"),
        Badge("speed_five", "البرق", "⚡", "5 انتصارات سريعة"),
        Badge("pet_evolved", "مربي الحيوانات", "🐉", "طوّر الكائن لآخره", isSecret = true),
        Badge("daily_champ", "بطل التحدي", "🏆", "إكمال تحدي اليوم كاملًا"),
    )

    /** مراحل الكائن — تتقدم مع عدد الانتصارات */
    val petStages: List<PetStage> = listOf(
        PetStage("🥚", "البيضة", 0),
        PetStage("🐣", "الفرخ", 5),
        PetStage("🐥", "الكتكوت", 10),
        PetStage("🐓", "الديك", 25),
        PetStage("🦅", "النسر", 50),
    )

    /** مرحلة الكائن حسب عدد الانتصارات */
    fun currentPetStage(wins: Int): PetStage =
        petStages.lastOrNull { wins >= it.winsRequired } ?: petStages.first()

    /** مكافأة عشوائية لصندوق الكنز اليومي */
    fun openChest(seed: Long): ChestReward {
        val rewards = listOf(
            ChestReward("نجمة إضافية", "⭐"),
            ChestReward("تلميحان إضافيان", "💡"),
            ChestReward("ثيم جديد", "🎨"),
            ChestReward("ملصق جديد", "🦄"),
            ChestReward("نقاط مضاعفة", "🔥"),
        )
        return rewards[(seed % rewards.size).toInt()]
    }
}

/** جائزة صندوق الكنز */
data class ChestReward(val name: String, val emoji: String)

