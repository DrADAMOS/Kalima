package com.adamos.kalima.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeManagerContrastTest {
    @Test
    fun semanticForegroundsHaveReadableContrastAcrossAllThemes() {
        ThemeManager.all.forEach { theme ->
            val colors = ThemeManager.of(theme)
            assertContrast(theme, "accent", colors.accent, colors.onAccent)
            assertContrast(theme, "accent2", colors.accent2, colors.onAccent2)
            assertContrast(theme, "correct", colors.correct, colors.onCorrect)
            assertContrast(theme, "present", colors.present, colors.onPresent)
            assertContrast(theme, "absent", colors.absent, colors.onAbsent)
            assertContrast(theme, "surface", colors.surface, colors.onSurface)
            assertContrast(theme, "accent text on background", colors.bg, colors.accentText)
            assertContrast(theme, "accent text on surface", colors.surface, colors.accentText)
            assertContrast(theme, "muted text on background", colors.bg, colors.textDim)
            assertContrast(theme, "muted text on surface", colors.surface, colors.textDim)
        }
    }

    private fun assertContrast(theme: GameTheme, token: String, background: Color, foreground: Color) {
        val lighter = maxOf(background.luminance(), foreground.luminance())
        val darker = minOf(background.luminance(), foreground.luminance())
        val ratio = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue("$theme $token contrast was $ratio", ratio >= 4.5f)
    }
}
