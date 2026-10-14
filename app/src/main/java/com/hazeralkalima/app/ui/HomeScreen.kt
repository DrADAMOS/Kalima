package com.hazeralkalima.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazeralkalima.app.data.Dictionary
import com.hazeralkalima.app.data.GameMode
import com.hazeralkalima.app.data.Preferences
import com.hazeralkalima.app.ui.theme.ThemeColors
import com.hazeralkalima.app.ui.theme.ThemeManager

/** الشاشة الرئيسية — اختيار الوضع والدخول للعبة */
@Composable
fun HomeScreen(
    onPlay: (GameMode, Int) -> Unit,
    onRewards: () -> Unit,
    onLearn: () -> Unit,
    onSettings: () -> Unit,
    onLeaderboard: () -> Unit,
    onHowToPlay: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val colors: ThemeColors = ThemeManager.of(prefs.gameTheme)

    var hardMode by remember { mutableStateOf(prefs.hardMode) }
    val totalDaily = Dictionary.lengths.intersect(3..7).size
    var dailyDone by remember { mutableStateOf(prefs.dailyCompletedCount()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(6.dp))

            // شريط الأيقونات العلوي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                HeaderIcon(Icons.AutoMirrored.Filled.HelpOutline, "كيف تلعب", colors, onHowToPlay)
                HeaderIcon(Icons.Filled.EmojiEvents, "المتصدرون", colors, onLeaderboard)
                HeaderIcon(Icons.Filled.School, "التعلّم", colors, onLearn)
                HeaderIcon(Icons.Filled.Settings, "الإعدادات", colors, onSettings)
            }

            Spacer(Modifier.height(16.dp))

            // العنوان
            Text(
                text = "احزر الكلمة",
                color = colors.text,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "خمّن الكلمة!",
                color = colors.textDim,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            // تحدي اليوم
            ModeCard(
                title = "تحدي اليوم",
                subtitle = "$totalDaily ألغاز · $dailyDone/$totalDaily مكتمل",
                color = colors.accent,
                icon = Icons.Filled.WbSunny,
                colors = colors,
                onClick = {
                    val nextLen = (3..7).firstOrNull { !prefs.isDailyFinished(it) } ?: 5
                    onPlay(GameMode.DAILY, nextLen)
                },
            )

            Spacer(Modifier.height(8.dp))

            // بلا نهاية
            ModeCard(
                title = "بلا نهاية",
                subtitle = "واصل التخمين، وابنِ سلسلتك",
                color = colors.correct,
                icon = Icons.Filled.AllInclusive,
                colors = colors,
                onClick = { onPlay(GameMode.ENDLESS, 5) },
            )

            Spacer(Modifier.height(8.dp))

            // عشوائي
            ModeCard(
                title = "عشوائي",
                subtitle = "طول كلمة مفاجئ",
                color = colors.accent2,
                icon = Icons.Filled.Casino,
                colors = colors,
                onClick = { onPlay(GameMode.RANDOM, Dictionary.lengths.random()) },
            )

            Spacer(Modifier.height(8.dp))

            // وضع التعلّم
            ModeCard(
                title = "وضع التعلّم 📚",
                subtitle = "بطاقات وكلمات جديدة بدون ضغط",
                color = Color(0xFF9C27B0),
                icon = Icons.Filled.School,
                colors = colors,
                onClick = onLearn,
            )

            Spacer(Modifier.height(8.dp))

            // الوضع الصعب
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.card)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔥", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "الوضع الصعب",
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = hardMode,
                    onCheckedChange = {
                        hardMode = it
                        prefs.hardMode = it
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.accent),
                )
            }

            Spacer(Modifier.height(16.dp))

            // اختيار سريع لطول الكلمة متمركز
            Text(
                text = "اختيار سريع لطول الكلمة",
                color = colors.textDim,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Dictionary.lengths.take(8).forEach { len ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                            .border(2.dp, colors.accent.copy(alpha = 0.4f), CircleShape)
                            .clickable { onPlay(GameMode.RANDOM, len) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$len",
                            color = colors.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeaderIcon(
    icon: ImageVector,
    label: String,
    colors: ThemeColors,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.textDim,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = colors.textDim, fontSize = 10.sp)
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    colors: ThemeColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, color = colors.textDim, fontSize = 12.sp)
        }
    }
}
