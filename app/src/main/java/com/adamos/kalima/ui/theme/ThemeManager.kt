package com.adamos.kalima.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** الثيمات المتاحة في اللعبة (فاتحة ومبهجة للأطفال) */
enum class GameTheme { ORIGINAL, SPACE, ANIMALS, CANDY, OCEAN, SUPERHERO }

/**
 * لوحة ألوان كاملة لكل ثيم (وضع فاتح فقط) مع ألوان تباين دلالية (Semantic Contrast).
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
    /** ألوان متقدمة لدعم التصميم ثنائي/ثلاثي الأبعاد لكل ثيم */
    val woodFrameGrad: List<Color> = listOf(Color(0xFFD4A373), Color(0xFF8B5A2B), Color(0xFF5C3A1A)),
    val chalkboardBg: Color = Color(0xFF1E3F20),
    val keyboardWoodGrad: List<Color> = listOf(Color(0xFF8B5A2B), Color(0xFF5C3A1A)),
    val keyPastelColors: List<Color> = listOf(
        Color(0xFFFFF3E0), // amber
        Color(0xFFE0F7FA), // turquoise
        Color(0xFFFCE4EC), // pale pink
        Color(0xFFFFFDE7)  // ivory
    ),
    val brassAccent: Color = Color(0xFFFFD700),
    /** Darkened accent variant for text drawn on light surfaces. */
    val accentText: Color = readableAccentText(accent, bg),
    val onAccent: Color = foregroundFor(accent),
    val onAccent2: Color = foregroundFor(accent2),
    val onCorrect: Color = foregroundFor(correct),
    val onPresent: Color = foregroundFor(present),
    val onAbsent: Color = foregroundFor(absent),
    val onSurface: Color = text,
    val bgGlyphs: List<String>,
    val icon: String,
    val label: String,
)


/** Darken an accent without changing its hue until it reaches WCAG AA text contrast. */
private fun readableAccentText(accent: Color, background: Color): Color {
    fun contrast(foreground: Color): Float {
        // Accent text appears on both the theme background and white cards/surfaces.
        val backgroundLuminance = maxOf(background.luminance(), Color.White.luminance())
        val lighter = maxOf(backgroundLuminance, foreground.luminance())
        val darker = minOf(backgroundLuminance, foreground.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    var candidate = accent
    repeat(12) {
        // Use a 5.0 target (not merely 4.5) to leave enough margin for
        // slightly tinted theme backgrounds as well as white cards.
        if (contrast(candidate) >= 5.0f) return candidate
        candidate = Color(candidate.red * 0.82f, candidate.green * 0.82f, candidate.blue * 0.82f, candidate.alpha)
    }
    return Color(0xFF17212B)
}

/** Choose whichever of black/white gives readable foreground contrast. */
private fun foregroundFor(background: Color): Color {
    val dark = Color(0xFF17212B)
    fun contrast(foreground: Color): Float {
        val lighter = maxOf(background.luminance(), foreground.luminance())
        val darker = minOf(background.luminance(), foreground.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    return if (contrast(dark) >= contrast(Color.White)) dark else Color.White
}

object ThemeManager {

    private val themes: Map<GameTheme, ThemeColors> = mapOf(

        GameTheme.ORIGINAL to ThemeColors(
            bg = Color(0xFFFFF9E8),
            card = Color(0xFFFFFFFF),
            cellEmpty = Color(0xFFFFFFFF),
            cellBorder = Color(0xFFFFE082),
            accent = Color(0xFFFFB300),
            accent2 = Color(0xFF42A5F5),
            text = Color(0xFF263238),
            textDim = Color(0xFF526873),
            correct = Color(0xFF66BB6A),
            present = Color(0xFFFFB300),
            absent = Color(0xFF90A4AE),
            surface = Color(0xFFFFFFFF),
            woodFrameGrad = listOf(Color(0xFFD4A373), Color(0xFF8B5A2B), Color(0xFF5C3A1A)),
            chalkboardBg = Color(0xFF1B3B1E), // classic forest green chalkboard
            keyboardWoodGrad = listOf(Color(0xFF8B5A2B), Color(0xFF5C3A1A)),
            keyPastelColors = listOf(Color(0xFFFFF3E0), Color(0xFFE0F7FA), Color(0xFFFCE4EC), Color(0xFFFFFDE7)),
            brassAccent = Color(0xFFFFD700),
            bgGlyphs = listOf("✨", "⭐", "🏜️", "🏆", "💡", "🧩", "أ", "ب"),
            icon = "🏜️",
            label = "السماء المشرقة",
        ),

        GameTheme.SPACE to ThemeColors(
            bg = Color(0xFFF5F0FF),
            card = Color(0xFFFFFFFF),
            cellEmpty = Color(0xFFFFFFFF),
            cellBorder = Color(0xFFD1C4E9),
            accent = Color(0xFF7E57C2),
            accent2 = Color(0xFF42A5F5),
            text = Color(0xFF302744),
            textDim = Color(0xFF655A7C),
            correct = Color(0xFF26A69A),
            present = Color(0xFF7E57C2),
            absent = Color(0xFF958DA5),
            surface = Color(0xFFFFFFFF),
            woodFrameGrad = listOf(Color(0xFF9C27B0), Color(0xFF4A148C), Color(0xFF1A237E)),
            chalkboardBg = Color(0xFF0F172A), // cosmic deep dark slate
            keyboardWoodGrad = listOf(Color(0xFF4A148C), Color(0xFF1A237E)),
            keyPastelColors = listOf(Color(0xFFEDE7F6), Color(0xFFE1F5FE), Color(0xFFF3E5F5), Color(0xFFE8EAF6)),
            brassAccent = Color(0xFFE040FB),
            bgGlyphs = listOf("⭐", "🌍", "🚀", "🛸", "☄️", "🌙", "🪐", "💫"),
            icon = "🚀",
            label = "مغامرة الفضاء",
        ),

        GameTheme.ANIMALS to ThemeColors(
            bg = Color(0xFFF1F8E9),
            card = Color(0xFFFFFFFF),
            cellEmpty = Color(0xFFFFFFFF),
            cellBorder = Color(0xFFC8E6C9),
            accent = Color(0xFF43A047),
            accent2 = Color(0xFFFFA726),
            text = Color(0xFF26352A),
            textDim = Color(0xFF456F26),
            correct = Color(0xFF43A047),
            present = Color(0xFFFFA726),
            absent = Color(0xFF9E9D24),
            surface = Color(0xFFFFFFFF),
            woodFrameGrad = listOf(Color(0xFFAED581), Color(0xFF558B2F), Color(0xFF2E7D32)),
            chalkboardBg = Color(0xFF1C3A27), // jungle green
            keyboardWoodGrad = listOf(Color(0xFF558B2F), Color(0xFF2E7D32)),
            keyPastelColors = listOf(Color(0xFFF1F8E9), Color(0xFFFFF8E1), Color(0xFFE8F5E9), Color(0xFFFFF3E0)),
            brassAccent = Color(0xFFFFC107),
            bgGlyphs = listOf("🐾", "🦁", "🐘", "🦒", "🐒", "🐰", "🐸", "🦋"),
            icon = "🦁",
            label = "عالم الحيوانات",
        ),

        GameTheme.CANDY to ThemeColors(
            bg = Color(0xFFFFF0F5),
            card = Color(0xFFFFFFFF),
            cellEmpty = Color(0xFFFFFFFF),
            cellBorder = Color(0xFFF8BBD0),
            accent = Color(0xFFD81B60),
            accent2 = Color(0xFFAB47BC),
            text = Color(0xFF452637),
            textDim = Color(0xFF7A0C46),
            correct = Color(0xFF66BB6A),
            present = Color(0xFFFFB300),
            absent = Color(0xFFB0BEC5),
            surface = Color(0xFFFFFFFF),
            woodFrameGrad = listOf(Color(0xFFF48FB1), Color(0xFFC2185B), Color(0xFF880E4F)),
            chalkboardBg = Color(0xFF38002C), // deep plum/magenta chalkboard
            keyboardWoodGrad = listOf(Color(0xFFC2185B), Color(0xFF880E4F)),
            keyPastelColors = listOf(Color(0xFFFCE4EC), Color(0xFFF3E5F5), Color(0xFFFFF0F5), Color(0xFFFFF8E1)),
            brassAccent = Color(0xFFFF4081),
            bgGlyphs = listOf("🍭", "🍬", "🍩", "🧁", "🎂", "🍪", "🍫", "🍦"),
            icon = "🍭",
            label = "عالم الحلوى",
        ),

        GameTheme.OCEAN to ThemeColors(
            bg = Color(0xFFE8F8FC),
            card = Color(0xFFFFFFFF),
            cellEmpty = Color(0xFFFFFFFF),
            cellBorder = Color(0xFFB2EBF2),
            accent = Color(0xFF0097A7),
            accent2 = Color(0xFF42A5F5),
            text = Color(0xFF20373D),
            textDim = Color(0xFF006773),
            correct = Color(0xFF26A69A),
            present = Color(0xFF42A5F5),
            absent = Color(0xFF90A4AE),
            surface = Color(0xFFFFFFFF),
            woodFrameGrad = listOf(Color(0xFF80DEEA), Color(0xFF00838F), Color(0xFF004D40)),
            chalkboardBg = Color(0xFF0A2E38), // deep ocean abyss
            keyboardWoodGrad = listOf(Color(0xFF00838F), Color(0xFF004D40)),
            keyPastelColors = listOf(Color(0xFFE0F7FA), Color(0xFFE1F5FE), Color(0xFFE0F2F1), Color(0xFFFFFDE7)),
            brassAccent = Color(0xFF00E5FF),
            bgGlyphs = listOf("🐠", "🐙", "🦀", "🐬", "🐳", "🦈", "🐚", "🫧"),
            icon = "🐠",
            label = "مغامرة المحيط",
        ),

        GameTheme.SUPERHERO to ThemeColors(
            bg = Color(0xFFFFF5E6),
            card = Color(0xFFFFFFFF),
            cellEmpty = Color(0xFFFFFFFF),
            cellBorder = Color(0xFFFFE0B2),
            accent = Color(0xFFEF5350),
            accent2 = Color(0xFF1976D2),
            text = Color(0xFF342B2B),
            textDim = Color(0xFF76584A),
            correct = Color(0xFF66BB6A),
            present = Color(0xFFFFB300),
            absent = Color(0xFF90A4AE),
            surface = Color(0xFFFFFFFF),
            woodFrameGrad = listOf(Color(0xFFFFD54F), Color(0xFFE65100), Color(0xFFB71C1C)),
            chalkboardBg = Color(0xFF1A1A24), // dark hero slate
            keyboardWoodGrad = listOf(Color(0xFFE65100), Color(0xFFB71C1C)),
            keyPastelColors = listOf(Color(0xFFFFF3E0), Color(0xFFFFEBEE), Color(0xFFE3F2FD), Color(0xFFFFFDE7)),
            brassAccent = Color(0xFFFFD700),
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
