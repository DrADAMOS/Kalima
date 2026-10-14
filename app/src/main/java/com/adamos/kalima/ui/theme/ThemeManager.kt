package com.adamos.kalima.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** الثيمات المتاحة في اللعبة */
enum class GameTheme { ORIGINAL, SPACE, ANIMALS, CANDY, OCEAN, SUPERHERO }

/**
 * لوحة ألوان كاملة لكل ثيم.
 * كل ثيم يعرّف خلفيته وهويته وألوان التصحيح ورموز خلفيته المتحركة.
 */
data class ThemeColors(
    val bg: Color,
    val card: Color,
    val cellEmpty: Color,
    val cellBorder: Color,
    val accent: Color,
    val accent2: Color,
    val text: Color,
    val textDim: Color,
    val correct: Color,
    val present: Color,
    val absent: Color,
    val surface: Color,
    val bgGlyphs: List<String>,
    val icon: String,
    val label: String,
)

object ThemeManager {

    private val themes: Map<GameTheme, ThemeColors> = mapOf(

        // مغامرة قصصية للأطفال: سماء زرقاء، ذهب دافئ، ودرجات تشبه لوحة الكنز.
        GameTheme.ORIGINAL to ThemeColors(
            bg = Color(0xFF123B78),
            card = Color(0xFF174A91),
            cellEmpty = Color(0xFF245AA0),
            cellBorder = Color(0xFF3976B8),
            accent = Color(0xFFFFC928),
            accent2 = Color(0xFFFF8A1F),
            text = Color(0xFFFFF8E8),
            textDim = Color(0xFFCFDFF7),
            correct = Color(0xFF2E9E58),
            present = Color(0xFFFFD52A),
            absent = Color(0xFF875044),
            surface = Color(0xFF0B2D60),
            bgGlyphs = listOf("✨", "⭐", "🏜️", "🏆", "💡", "🧩", "أ", "ب"),
            icon = "🏜️",
            label = "مغامرة الكلمات",
        ),

        GameTheme.SPACE to ThemeColors(
            bg = Color(0xFF0D0B2E),
            card = Color(0xFF1A1650),
            cellEmpty = Color(0xFF1E1A5E),
            cellBorder = Color(0xFF302D78),
            accent = Color(0xFF7C4DFF),
            accent2 = Color(0xFF00E5FF),
            text = Color(0xFFF5F0FF),
            textDim = Color(0xFFB0A0E0),
            correct = Color(0xFF76FF03),
            present = Color(0xFFFFE57F),
            absent = Color(0xFF3E3A70),
            surface = Color(0xFF252168),
            bgGlyphs = listOf("⭐", "🌍", "🚀", "🛸", "☄️", "🌙", "🪐", "💫"),
            icon = "🚀",
            label = "الفضاء",
        ),

        GameTheme.ANIMALS to ThemeColors(
            bg = Color(0xFF1B5E20),
            card = Color(0xFF2E7D32),
            cellEmpty = Color(0xFF388E3C),
            cellBorder = Color(0xFF4CAF50),
            accent = Color(0xFFFF8F00),
            accent2 = Color(0xFFFFC107),
            text = Color(0xFFF1F8E9),
            textDim = Color(0xFFAED581),
            correct = Color(0xFF00C853),
            present = Color(0xFFFFD600),
            absent = Color(0xFF4E342E),
            surface = Color(0xFF33691E),
            bgGlyphs = listOf("🐾", "🦁", "🐘", "🦒", "🐒", "🐰", "🐸", "🦋"),
            icon = "🦁",
            label = "الحيوانات",
        ),

        GameTheme.CANDY to ThemeColors(
            bg = Color(0xFFFCE4EC),
            card = Color(0xFFF8BBD0),
            cellEmpty = Color(0xFFF48FB1),
            cellBorder = Color(0xFFEC407A),
            accent = Color(0xFFE91E63),
            accent2 = Color(0xFF00BCD4),
            text = Color(0xFF4A0024),
            textDim = Color(0xFFAD1457),
            correct = Color(0xFF00C853),
            present = Color(0xFFFFC107),
            absent = Color(0xFFE0E0E0),
            surface = Color(0xFFFCE4EC),
            bgGlyphs = listOf("🍭", "🍬", "🍩", "🧁", "🎂", "🍪", "🍫", "🍦"),
            icon = "🍭",
            label = "الحلويات",
        ),

        GameTheme.OCEAN to ThemeColors(
            bg = Color(0xFF006064),
            card = Color(0xFF00838F),
            cellEmpty = Color(0xFF0097A7),
            cellBorder = Color(0xFF00BCD4),
            accent = Color(0xFF00E5FF),
            accent2 = Color(0xFF7C4DFF),
            text = Color(0xFFE0F7FA),
            textDim = Color(0xFF80DEEA),
            correct = Color(0xFF69F0AE),
            present = Color(0xFFFFE57F),
            absent = Color(0xFF3E5C76),
            surface = Color(0xFF005B67),
            bgGlyphs = listOf("🐠", "🐙", "🦀", "🐬", "🐳", "🦈", "🐚", "🫧"),
            icon = "🐠",
            label = "المحيط",
        ),

        GameTheme.SUPERHERO to ThemeColors(
            bg = Color(0xFFB71C1C),
            card = Color(0xFFC62828),
            cellEmpty = Color(0xFFD32F2F),
            cellBorder = Color(0xFFE53935),
            accent = Color(0xFFFFD600),
            accent2 = Color(0xFF2196F3),
            text = Color(0xFFFFFDE7),
            textDim = Color(0xFFFFCDD2),
            correct = Color(0xFF00E676),
            present = Color(0xFFFFEA00),
            absent = Color(0xFF546E7A),
            surface = Color(0xFF8E0000),
            bgGlyphs = listOf("💥", "⚡", "🦸", "🛡️", "⭐", "🔥", "💪", "🎯"),
            icon = "🦸",
            label = "الأبطال",
        ),
    )

    fun of(theme: GameTheme): ThemeColors = themes[theme] ?: themes[GameTheme.ORIGINAL]!!

    fun fromIndex(index: Int): GameTheme =
        GameTheme.entries.getOrElse(index) { GameTheme.ORIGINAL }

    val all: List<GameTheme> get() = GameTheme.entries.toList()
}

/** أبعاد مشتركة */
object Dimens {
    val cardRadius = 18.dp
    val cellRadius = 8.dp
    val keyRadius = 6.dp
}

