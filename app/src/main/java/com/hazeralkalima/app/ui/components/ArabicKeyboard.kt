package com.hazeralkalima.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazeralkalima.app.data.Dictionary
import com.hazeralkalima.app.data.LetterStatus
import com.hazeralkalima.app.ui.theme.ThemeColors

/** لوحة المفاتيح العربية — تخطيط لوحة المفاتيح العربية القياسي (3 صفوف × 11 مفتاحاً) */
@Composable
fun ArabicKeyboard(
    usedLetters: Map<String, LetterStatus>,
    colors: ThemeColors,
    onKey: (String) -> Unit,
    onEnter: () -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean = true,
    revealedLetters: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val keyHeight = if (configuration.screenHeightDp < 700) 46.dp else 52.dp
    val fontSize = if (configuration.screenWidthDp < 340) 18.sp else 20.sp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .alpha(if (enabled) 1.0f else 0.4f),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        KeyboardRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            ) {
                row.forEach { letter ->
                    val normalized = Dictionary.normalize(letter)
                    KeyButton(
                        label = letter,
                        status = usedLetters[normalized],
                        isHintRevealed = revealedLetters.contains(normalized),
                        colors = colors,
                        height = keyHeight,
                        fontSize = fontSize,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        onClick = {
                            if (enabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onKey(letter)
                            }
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ActionKey(
                label = "إرسال",
                color = colors.accent,
                height = 56.dp,
                modifier = Modifier.weight(2f),
                enabled = enabled,
                onClick = {
                    if (enabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onEnter()
                    }
                },
            )
            BackspaceActionKey(
                label = "مسح",
                color = colors.accent2,
                height = 56.dp,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                onClick = {
                    if (enabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onBackspace()
                    }
                },
            )
        }
    }
}

/** 3 صفوف × 11 مفتاحاً (33 مفتاحاً إجمالاً) بتخطيط لوحة المفاتيح العربية القياسي */
internal val KeyboardRows: List<List<String>> = listOf(
    listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"),
    listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"),
    listOf("ذ", "ء", "ؤ", "ر", "ئ", "ى", "ة", "و", "ز", "ظ", "د"),
)

/**
 * مفتاح حرف.
 * isHintRevealed: الحرف كشفه التلميح في الشبكة — يُميَّز بحدود بلون التمييز
 * حتى لا يوهم اللاعب أنه حرف غير مستخدم.
 */
@Composable
private fun KeyButton(
    label: String,
    status: LetterStatus?,
    colors: ThemeColors,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    isHintRevealed: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = when (status) {
        LetterStatus.CORRECT -> colors.correct
        LetterStatus.PRESENT -> colors.present
        LetterStatus.ABSENT -> colors.absent
        else -> colors.surface
    }
    val fg = when (status) {
        LetterStatus.PRESENT -> Color(0xFF1A1A1A)
        else -> colors.text
    }
    // الحدود تظهر فقط للحرف المكشوف بتلميح ولم تُسجَّل له حالة بعد
    val showHintBorder = isHintRevealed && status == null

    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bg)
            .then(
                if (showHintBorder) {
                    Modifier.border(2.dp, colors.accent, shape)
                } else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActionKey(
    label: String,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.22f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BackspaceActionKey(
    label: String,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.22f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
