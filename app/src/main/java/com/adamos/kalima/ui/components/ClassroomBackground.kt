package com.adamos.kalima.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.ui.theme.LocalKalimaTheme
import com.adamos.kalima.ui.theme.ThemeColors

/**
 * خلفية الفصل الدافئة — جدران ناعمة بنمط خشب وفصل دافئ،
 * خالية من الأيقونات المزيفة أعلى الشاشة لعدم التشويش على أشرطة النظام.
 */
@Composable
fun ClassroomBackground(
    modifier: Modifier = Modifier,
    colors: ThemeColors = LocalKalimaTheme.current,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // رسم خط الحائط والإفريز العلوي الناعم عبر Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // خط شريط الإفريز العلوي بالجدار
            drawLine(
                color = colors.accent.copy(alpha = 0.20f),
                start = Offset(0f, height * 0.10f),
                end = Offset(width, height * 0.10f),
                strokeWidth = 2.5f,
            )

            // إضاءة خفيفة نازلة من أعلى الجدار
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height * 0.30f
                ),
                size = Size(width, height * 0.30f)
            )
        }

        // رموز الثيم المنتشرة بلطف في خلفية الجدار
        colors.bgGlyphs.take(4).forEachIndexed { index, glyph ->
            val alignment = when (index) {
                0 -> Alignment.CenterStart
                1 -> Alignment.CenterEnd
                2 -> Alignment.BottomStart
                else -> Alignment.BottomEnd
            }
            Text(
                text = glyph,
                fontSize = 20.sp,
                modifier = Modifier
                    .align(alignment)
                    .padding(16.dp)
                    .alpha(0.12f),
            )
        }
    }
}
