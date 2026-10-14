package com.hazeralkalima.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

private val KalimaDarkColors = darkColorScheme(
    primary = Color(0xFF3BA9F4),
    secondary = Color(0xFFFFA726),
    background = Color(0xFF12203A),
    surface = Color(0xFF1A2C4A),
    onPrimary = Color.White,
    onBackground = Color(0xFFF0F4FA),
    onSurface = Color(0xFFF0F4FA),
)

/** دالة مساعدة لتحديد ما إذا كان الثيم فاتحًا */
fun ThemeColors.isLight(): Boolean = bg.luminance() > 0.5f

/** دالة مساعدة لاستخراج لون شريط النظام Argb */
fun ThemeColors.systemBarArgb(): Int = bg.toArgb()

/**
 * ثيم Compose الأساسي للتطبيق.
 * الألوان الفعلية لكل شاشة تأتي من ThemeManager حسب الثيم المختار.
 */
@Composable
fun KalimaTheme(
    theme: GameTheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KalimaDarkColors,
        content = content,
    )
}
