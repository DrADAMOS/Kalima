package com.adamos.kalima.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.Badge
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.data.Rewards
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager

/** شاشة المكافآت — الشارات والكائن المتطور والإحصائيات مع حركة دخول خفيفة */
@Composable
fun RewardsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val currentTheme by prefs.themeState.collectAsState(initial = prefs.gameTheme)
    val colors: ThemeColors = ThemeManager.of(currentTheme)
    val unlocked = remember { prefs.unlockedBadges() }
    val pet = Rewards.currentPetStage(prefs.gamesWon)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader("المكافآت والإنجازات", colors, onBack)

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 15 },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                // الكائن
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.card)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(pet.emoji, fontSize = 64.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(pet.name, color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text("${prefs.gamesWon} فوز", color = colors.textDim, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "🏅 الشارات (${unlocked.size}/${Rewards.allBadges.size})",
                    color = colors.textDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))

                Rewards.allBadges.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { badge ->
                            BadgeTile(badge, badge.id in unlocked, colors, Modifier.weight(1f))
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "📊 الإحصائيات",
                    color = colors.textDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))

                StatRow("🎮", "جولات", "${prefs.gamesPlayed}", colors)
                StatRow("🏆", "انتصارات", "${prefs.gamesWon}", colors)
                StatRow("🔥", "السلسلة الحالية", "${prefs.currentStreak}", colors)
                StatRow("⭐", "أفضل سلسلة", "${prefs.bestStreak}", colors)

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: Badge, unlocked: Boolean, colors: ThemeColors, modifier: Modifier) {
    val hidden = badge.isSecret && !unlocked
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.card)
            .border(1.dp, if (unlocked) colors.accent2 else Color.White.copy(alpha = 0.08f), shape)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (hidden) "❓" else badge.emoji,
            fontSize = 28.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hidden) "؟؟؟" else badge.name,
            color = if (unlocked) colors.text else colors.textDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (hidden) "اكتشفه بنفسك" else badge.description,
            color = colors.textDim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatRow(icon: String, label: String, value: String, colors: ThemeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Text(label, color = colors.text, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = colors.accent2, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

/** شريط علوي مشترك لكل الشاشات المساندة */
@Composable
fun ScreenHeader(title: String, colors: ThemeColors, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "رجوع",
            tint = colors.text,
            modifier = Modifier.size(22.dp).clickable(onClick = onBack),
        )
        Spacer(Modifier.width(14.dp))
        Text(title, color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
