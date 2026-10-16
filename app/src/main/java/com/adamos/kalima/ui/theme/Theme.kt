package com.adamos.kalima.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/** دالة مساعدة لتحديد ما إذا كان الثيم فاتحًا */
fun ThemeColors.isLight(): Boolean = bg.luminance() > 0.5f

/** دالة مساعدة لاستخراج لون شريط النظام Argb */
fun ThemeColors.systemBarArgb(): Int = bg.toArgb()

/** CompositionLocal لسهولة الوصول لألوان الثيم الحالي في أي مكان في شجرة الـ Compose */
val LocalKalimaTheme = staticCompositionLocalOf { ThemeManager.of(GameTheme.ORIGINAL) }

/**
 * ثيم Compose الأساسي للتطبيق (فاتح بالكامل ومناسب للأطفال).
 * الألوان الفعلية لكل شاشة تأتي من ThemeManager حسب الثيم المختار.
 */
@Composable
fun KalimaTheme(
    theme: GameTheme,
    content: @Composable () -> Unit,
) {
    val colors = ThemeManager.of(theme)
    val lightScheme = lightColorScheme(
        primary = colors.accent,
        secondary = colors.accent2,
        background = colors.bg,
        surface = colors.surface,
        onPrimary = colors.onAccent,
        onSecondary = colors.onAccent2,
        onBackground = colors.text,
        onSurface = colors.onSurface,
        surfaceVariant = colors.card,
        onSurfaceVariant = colors.textDim,
    )

    CompositionLocalProvider(LocalKalimaTheme provides colors) {
        MaterialTheme(
            colorScheme = lightScheme,
            content = content,
        )
    }
}
