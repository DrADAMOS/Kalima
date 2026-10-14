package com.adamos.kalima.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.LetterCell
import com.adamos.kalima.data.LetterStatus
import com.adamos.kalima.ui.theme.ThemeColors

/**
 * شبكة الكلمات — صفوف وأعمدة متمركزة ديناميكياً بحسب طول الكلمة وأبعاد الشاشة.
 * الحد الأدنى لحجم الخلية 30.dp (والأقصى 60.dp) لضمان عدم تجاوز العرض أو الارتفاع
 * حتى على الشاشات الصغيرة (320x640dp) مع 3..7 حروف و 8 صفوف.
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
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val spacing = 6.dp
        val horizontalPadding = 16.dp
        val availableWidth = (maxWidth - (horizontalPadding * 2)).coerceAtLeast(120.dp)
        val maxCellWidth = (availableWidth - (spacing * (wordLength - 1))) / wordLength

        val totalRows = rows.size.coerceAtLeast(1)
        val availableHeight = maxHeight.coerceAtLeast(100.dp)
        val maxCellHeight = (availableHeight - (spacing * (totalRows - 1))) / totalRows

        // حجم الخلية مرن ومحكوم بين 30.dp و 60.dp لمنع تجاوز الشاشة
        val cellSize = minOf(maxCellWidth, maxCellHeight).coerceIn(30.dp, 60.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCell(
    letter: String,
    status: LetterStatus,
    isActive: Boolean,
    size: Dp,
    colors: ThemeColors,
) {
    val bg: Color
    val fg: Color
    val borderColor: Color?

    when (status) {
        LetterStatus.CORRECT -> {
            bg = colors.correct
            fg = Color.White
            borderColor = null
        }
        LetterStatus.PRESENT -> {
            bg = colors.present
            fg = Color(0xFF1A1A1A)
            borderColor = null
        }
        LetterStatus.ABSENT -> {
            bg = colors.absent
            fg = Color.White
            borderColor = null
        }
        LetterStatus.EMPTY -> {
            bg = colors.cellEmpty
            fg = colors.text
            borderColor = if (isActive) colors.accent else colors.cellBorder
        }
    }

    val shape = RoundedCornerShape((size.value * 0.18f).dp.coerceIn(6.dp, 12.dp))
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(bg)
            .then(
                if (borderColor != null) {
                    Modifier.border(
                        width = if (isActive) 2.dp else 1.dp,
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
        }
    }
}

