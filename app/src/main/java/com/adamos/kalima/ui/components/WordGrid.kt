package com.adamos.kalima.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.LetterCell
import com.adamos.kalima.data.LetterStatus
import com.adamos.kalima.ui.theme.ThemeColors

/**
 * شبكة الكلمات الطباشورية — صفوف وأعمدة متمركزة ديناميكياً بحجم استجابي يمنع الاقتصاص بالكامل.
 * مع شريط "حروف خاطئة:" أسفل السبورة ببلاطات خشبية مصغرة للحروف المستبعدة.
 */
@Composable
fun WordGrid(
    rows: List<List<LetterCell>>,
    currentAttempt: Int,
    currentColumn: Int,
    wordLength: Int,
    colors: ThemeColors,
    isGameOver: Boolean,
    modifier: Modifier = Modifier,
    incorrectLetters: List<String> = emptyList(),
    shakeToken: Int = 0,
    revealRowIndex: Int = -1,
    revealToken: Int = 0,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeToken) {
        if (shakeToken > 0) {
            shake.snapTo(0f)
            shake.animateTo(1f, animationSpec = tween(360))
            shake.snapTo(0f)
        }
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(translationX = if (shake.value == 0f) 0f else kotlin.math.sin(shake.value * Math.PI * 8).toFloat() * (1f - shake.value) * 18f),
        contentAlignment = Alignment.Center,
    ) {
        val totalRows = rows.size.coerceAtLeast(1)
        val hasIncorrectBadge = incorrectLetters.isNotEmpty()
        val badgeHeight = if (hasIncorrectBadge) (if (this.maxHeight < 280.dp) 20.dp else 26.dp) else 0.dp

        val spacing = when {
            this.maxHeight < 220.dp -> 2.dp
            this.maxHeight < 300.dp -> 3.dp
            else -> 5.dp
        }
        val horizontalPadding = 8.dp
        val availableWidth = (this.maxWidth - (horizontalPadding * 2)).coerceAtLeast(100.dp)
        val maxCellWidth = (availableWidth - (spacing * (wordLength - 1))) / wordLength

        val availableHeight = (this.maxHeight - badgeHeight - spacing).coerceAtLeast(80.dp)
        val maxCellHeight = (availableHeight - (spacing * (totalRows - 1))) / totalRows

        val cellSize = minOf(maxCellWidth, maxCellHeight).coerceIn(18.dp, 56.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // شبكة الصفوف
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                rows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (colIndex in 0 until wordLength) {
                            val cell = row.getOrNull(colIndex)
                            val letter = cell?.letter.orEmpty()
                            val isActive = rowIndex == currentAttempt &&
                                colIndex == currentColumn && !isGameOver
                            WordCell(
                                letter = letter,
                                status = cell?.status ?: LetterStatus.EMPTY,
                                isActive = isActive,
                                size = cellSize,
                                colors = colors,
                                colIndex = colIndex,
                                animateReveal = revealToken > 0 && rowIndex == revealRowIndex,
                            )
                        }
                    }
                }
            }

            // شريط "حروف خاطئة:" أسفل السبورة
            if (hasIncorrectBadge) {
                Spacer(Modifier.height(if (cellSize < 28.dp) 2.dp else 4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = "حروف خاطئة:",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = if (cellSize < 28.dp) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    incorrectLetters.take(8).forEach { letter ->
                        WoodenTileBadge(letter = letter, colors = colors, compact = cellSize < 28.dp)
                    }
                }
            }
        }
    }
}

/** خلية طباشورية في السبورة */
@Composable
private fun WordCell(
    letter: String,
    status: LetterStatus,
    isActive: Boolean,
    size: Dp,
    colors: ThemeColors,
    colIndex: Int,
    animateReveal: Boolean,
) {
    val bg: Color
    val fg: Color
    val borderColor: Color?

    when (status) {
        LetterStatus.CORRECT -> {
            bg = colors.correct
            fg = colors.onCorrect
            borderColor = null
        }
        LetterStatus.PRESENT -> {
            bg = colors.present
            fg = colors.onPresent
            borderColor = null
        }
        LetterStatus.ABSENT -> {
            bg = colors.absent
            fg = colors.onAbsent
            borderColor = null
        }
        LetterStatus.EMPTY -> {
            // خانة طباشورية شفافة على لون السبورة
            bg = Color.White.copy(alpha = 0.12f)
            fg = Color.White
            borderColor = if (isActive) colors.accent else Color.White.copy(alpha = 0.45f)
        }
    }

    val scaleAnim = remember { Animatable(1f) }
    val flipAnim = remember { Animatable(0f) }
    LaunchedEffect(letter) {
        if (letter.isNotEmpty()) {
            scaleAnim.snapTo(0.82f)
            scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 420f))
        } else {
            scaleAnim.snapTo(1f)
        }
    }
    LaunchedEffect(status, animateReveal) {
        if (status != LetterStatus.EMPTY && animateReveal) {
            delay(colIndex * 75L)
            flipAnim.snapTo(90f)
            flipAnim.animateTo(0f, animationSpec = tween(220))
        } else {
            flipAnim.snapTo(0f)
        }
    }

    val shape = RoundedCornerShape((size.value * 0.18f).dp.coerceIn(4.dp, 10.dp))
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer(scaleX = scaleAnim.value, scaleY = scaleAnim.value, rotationY = flipAnim.value)
            .clip(shape)
            .background(bg)
            .then(
                if (borderColor != null) {
                    Modifier.border(
                        width = if (isActive) 1.8.dp else 1.dp,
                        color = borderColor,
                        shape = shape,
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (letter.isNotEmpty()) {
            Text(
                text = letter,
                color = fg,
                fontSize = (size.value * 0.48f).sp,
                fontWeight = FontWeight.ExtraBold,
            )
        } else {
            // خط إرشادي طبشوري أسفل الخانة الفارغة
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (size.value * 0.08f).dp.coerceIn(2.dp, 4.dp))
                    .size(width = (size.value * 0.42f).dp, height = 1.8.dp)
                    .background(Color.White.copy(alpha = 0.6f))
            )
        }
    }
}

/** بلاطة خشبية مصغرة للحروف الخاطئة المستبعدة */
@Composable
private fun WoodenTileBadge(
    letter: String,
    colors: ThemeColors,
    compact: Boolean = false,
) {
    val badgeWidth = if (compact) 18.dp else 22.dp
    val badgeHeight = if (compact) 18.dp else 22.dp
    Box(
        modifier = Modifier
            .size(width = badgeWidth, height = badgeHeight)
            .shadow(1.5.dp, RoundedCornerShape(3.dp))
            .clip(RoundedCornerShape(3.dp))
            .background(colors.absent)
            .border(0.8.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = colors.onAbsent,
            fontSize = if (compact) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
