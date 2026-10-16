package com.adamos.kalima.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.data.LetterStatus
import com.adamos.kalima.ui.theme.LocalKalimaTheme
import com.adamos.kalima.ui.theme.ThemeColors

@Composable
fun ArabicKeyboard(
    usedLetters: Map<String, LetterStatus>,
    onKey: (String) -> Unit,
    onEnter: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ThemeColors = LocalKalimaTheme.current,
    enabled: Boolean = true,
    revealedLetters: Set<String> = emptySet(),
    onNextRound: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val cfg = LocalConfiguration.current
    val keyHeight = when {
        cfg.screenHeightDp < 600 -> 34.dp
        cfg.screenHeightDp < 680 -> 38.dp
        else -> 48.dp
    }
    val letterFontSize: TextUnit = when {
        cfg.screenHeightDp < 600 -> 15.sp
        cfg.screenHeightDp < 680 -> 17.sp
        cfg.screenWidthDp < 360 -> 18.sp
        else -> 21.sp
    }
    val rowSpacing = if (cfg.screenHeightDp < 680) 3.dp else 4.dp
    val outerPadding = if (cfg.screenHeightDp < 680) 3.dp else 5.dp

    val boardBg = colors.chalkboardBg
    val frameShape = RoundedCornerShape(18.dp)
    val boardShape = RoundedCornerShape(14.dp)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .shadow(8.dp, frameShape)
                .background(Brush.verticalGradient(colors.keyboardWoodGrad), frameShape)
                .border(1.5.dp, colors.brassAccent.copy(alpha = 0.6f), frameShape)
                .padding(outerPadding)
                .background(boardBg, boardShape)
                .navigationBarsPadding()
                .padding(horizontal = outerPadding, vertical = outerPadding)
                .alpha(if (enabled) 1f else 0.50f),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
        ) {
            // 3 صفوف × 11 حرفاً = 33 حرفاً
            KeyboardRows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                ) {
                    row.forEachIndexed { colIndex, letter ->
                        val normalized = Dictionary.normalize(letter)
                        // النمط الملون التناوبي للمفاتيح الفارغة
                        val pastelIndex = (rowIndex * 11 + colIndex) % colors.keyPastelColors.size
                        val pastelBg = colors.keyPastelColors[pastelIndex]

                        LetterTile(
                            letter = letter,
                            status = usedLetters[normalized],
                            defaultBg = pastelBg,
                            isHinted = revealedLetters.contains(normalized),
                            colors = colors,
                            height = keyHeight,
                            fontSize = letterFontSize,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            onPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onKey(letter)
                            },
                        )
                    }
                }
            }

            // صف أزرار التحكم والعمليات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                ActionTile(
                    label = "مسح",
                    iconType = ActionIconType.BACKSPACE,
                    colors = colors,
                    height = keyHeight,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onBackspace()
                    },
                )

                ActionTile(
                    label = "إرسال / تحقق",
                    iconType = ActionIconType.NONE,
                    colors = colors,
                    height = keyHeight,
                    modifier = Modifier.weight(2f),
                    enabled = enabled,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onEnter()
                    },
                )

                if (onNextRound != null) {
                    ActionTile(
                        label = "التالي",
                        iconType = ActionIconType.STAR,
                        colors = colors,
                        height = keyHeight,
                        modifier = Modifier.weight(1.2f),
                        enabled = true,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onNextRound()
                        },
                    )
                }
            }
        }
    }
}

/** تخطيط 3×11 = 33 حرفاً كامل. */
internal val KeyboardRows: List<List<String>> = listOf(
    listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"),
    listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"),
    listOf("ذ", "ء", "ؤ", "ر", "ئ", "ى", "ة", "و", "ز", "ظ", "د"),
)

enum class ActionIconType { NONE, BACKSPACE, STAR }

/** بلاط حرف ملون مجسم مع نقطة تركوازية إرشادية أعلى الزاوية */
@Composable
private fun LetterTile(
    letter: String,
    status: LetterStatus?,
    defaultBg: Color,
    colors: ThemeColors,
    height: Dp,
    fontSize: TextUnit,
    isHinted: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onPress: () -> Unit,
) {
    val bg = when (status) {
        LetterStatus.CORRECT -> colors.correct
        LetterStatus.PRESENT -> colors.present
        LetterStatus.ABSENT -> colors.absent
        else -> defaultBg
    }
    val fg = when (status) {
        LetterStatus.CORRECT -> colors.onCorrect
        LetterStatus.PRESENT -> colors.onPresent
        LetterStatus.ABSENT -> colors.onAbsent
        else -> colors.text
    }
    val borderColor = when {
        isHinted && status == null -> colors.accent
        status != null -> fg.copy(alpha = 0.25f)
        else -> colors.cellBorder.copy(alpha = 0.6f)
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
        label = "letterPressScale",
    )

    val chunkyStyle = remember(fontSize) {
        TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.25f),
                offset = Offset(0f, 1.2f),
                blurRadius = 1.5f,
            ),
        )
    }

    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .height(height)
            .clip(shape)
            .background(bg)
            .drawBehind {
                if (!pressed) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.22f),
                        topLeft = Offset(0f, size.height - 2.5.dp.toPx()),
                        size = Size(size.width, 2.5.dp.toPx()),
                    )
                }
            }
            .border(
                width = if (isHinted && status == null) 1.8.dp else 0.8.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onPress,
            )
            .semantics {
                contentDescription = letter
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = letter, color = fg, style = chunkyStyle, maxLines = 1)

        // النقطة التركوازية الإرشادية أعلى زاوية كل مفتاح حرف
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.5.dp)
                .size(4.dp)
                .background(
                    if (isHinted && status == null) colors.accent else Color(0xFF00BCD4),
                    CircleShape,
                )
        )
    }
}

/** مفتاح وظيفي ملون وبارز */
@Composable
private fun ActionTile(
    label: String,
    iconType: ActionIconType,
    colors: ThemeColors,
    height: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = when (iconType) {
        ActionIconType.STAR -> Color(0xFFEF5350) // أحمر دافئ
        ActionIconType.BACKSPACE -> Color(0xFF00BCD4) // تركواز
        ActionIconType.NONE -> colors.accent // عنبري / لون أكسنت الثيم
    }
    val fg = Color.White

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
        label = "actionPressScale",
    )

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .height(height)
            .clip(shape)
            .background(bg)
            .drawBehind {
                if (!pressed) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.28f),
                        topLeft = Offset(0f, size.height - 3.dp.toPx()),
                        size = Size(size.width, 3.dp.toPx()),
                    )
                }
            }
            .border(1.2.dp, Color.White.copy(alpha = 0.45f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            when (iconType) {
                ActionIconType.BACKSPACE -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(16.dp),
                )
                ActionIconType.STAR -> Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = colors.brassAccent,
                    modifier = Modifier.size(16.dp),
                )
                ActionIconType.NONE -> Unit
            }
            Text(
                text = label,
                color = fg,
                fontSize = if (iconType == ActionIconType.NONE) 14.sp else 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}
