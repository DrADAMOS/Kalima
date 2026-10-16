package com.adamos.kalima.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.ui.theme.LocalKalimaTheme
import com.adamos.kalima.ui.theme.ThemeColors

/**
 * العناصر الديكورية المجسمة بالفصل: الساعة الرملية الخشبية، القلم الممواة،
 * الكشكول، وزر التلميح على شكل المصباح المضيء — متكيفة مع الشاشات الصغيرة.
 */
@Composable
fun DecorativeObjectsBar(
    hintCount: Int,
    extraRowsRemaining: Int,
    onAddRow: () -> Unit,
    onUseHint: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ThemeColors = LocalKalimaTheme.current,
    pulseHintToken: Int = 0,
    isCompact: Boolean = false,
) {
    val verticalPadding = if (isCompact) 1.dp else 2.dp
    val iconFontSize = if (isCompact) 13.sp else 16.sp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. القلم الممواة والكشكول (جهة اليمين)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF3E0))
                    .border(1.2.dp, Color(0xFFFFB300), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = if (isCompact) 2.dp else 3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✏️", fontSize = iconFontSize)
                    Text(text = "📓", fontSize = iconFontSize)
                }
            }

            // زر +1 صف الخشبي المجسم
            RowActionButton(
                label = "+صف ($extraRowsRemaining)",
                color = colors.accent,
                isCompact = isCompact,
                onClick = onAddRow,
            )
        }

        // 2. الساعة الرملية وزر التلميح المصمم كالمصباح المضيء (جهة اليسار)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "⏳", fontSize = if (isCompact) 15.sp else 18.sp)

            // زر التلميح المميز كالمصباح المضيء
            BulbHintButton(
                hintCount = hintCount,
                colors = colors,
                pulseToken = pulseHintToken,
                isCompact = isCompact,
                onClick = onUseHint,
            )
        }
    }
}

@Composable
private fun RowActionButton(
    label: String,
    color: Color,
    isCompact: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "rowBtnScale")

    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(3.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(1.2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (isCompact) 8.dp else 10.dp, vertical = if (isCompact) 3.dp else 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (isCompact) 14.dp else 16.dp))
            Text(text = label, color = Color.White, fontSize = if (isCompact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BulbHintButton(
    hintCount: Int,
    colors: ThemeColors,
    pulseToken: Int,
    isCompact: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.90f else 1f, label = "bulbScale")
    val pulseAnim = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(pulseToken) {
        if (pulseToken > 0) {
            pulseAnim.animateTo(1.15f, animationSpec = spring(dampingRatio = 0.35f, stiffness = 500f))
            pulseAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = pressScale * pulseAnim.value, scaleY = pressScale * pulseAnim.value)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(colors.accent2)
            .border(1.5.dp, colors.brassAccent, CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = hintCount > 0, onClick = onClick)
            .padding(horizontal = if (isCompact) 9.dp else 11.dp, vertical = if (isCompact) 3.dp else 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = "تلميح",
                tint = colors.brassAccent,
                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp),
            )
            Text(
                text = if (hintCount > 90) "∞" else "$hintCount",
                color = colors.onAccent2,
                fontSize = if (isCompact) 12.sp else 13.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
