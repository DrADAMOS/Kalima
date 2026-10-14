package com.hazeralkalima.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KalimaDarkColors = darkColorScheme(
    primary = Color(0xFF3BA9F4),
    secondary = Color(0xFFFFA726),
    background = Color(0xFF12203A),
    surface = Color(0xFF1A2C4A),
    onPrimary = Color.White,
    onBackground = Color(0xFFF0F4FA),
    onSurface = Color(0xFFF0F4FA),
)

/**
 * ثيم Compose الأساسي للتطبيق.
 * الألوان الفعلية لكل شاشة تأتي من ThemeManager حسب الثيم المختار.
 */
@Composable
fun KalimaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KalimaDarkColors,
        content = content,
    )
}
