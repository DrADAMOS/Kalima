package com.adamos.kalima.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.data.GameMode
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager

/** الشاشة الرئيسية — اختيار الوضع والدخول للعبة (تظهر بالكامل بدون تمرير) */
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
    val totalDaily = Dictionary.lengths.size
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // شريط الأيقونات العلوي (يتضمن زر كيف تلعب)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                HeaderIcon(Icons.AutoMirrored.Filled.HelpOutline, "كيف تلعب", colors, onHowToPlay)
                HeaderIcon(Icons.Filled.EmojiEvents, "السجلات", colors, onLeaderboard)
                HeaderIcon(Icons.Filled.School, "التعلّم", colors, onLearn)
                HeaderIcon(Icons.Filled.Settings, "الإعدادات", colors, onSettings)
            }

            // العنوان
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "احزر الكلمة",
                    color = colors.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "خمّن الكلمة!",
                    color = colors.textDim,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            // لوحة ترحيبية مصغرة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.5.dp, colors.accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "رحلة الكلمات تبدأ هنا!",
                        color = colors.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "اكتشف مفردات واجمع النجوم ⭐",
                        color = colors.text,
                        fontSize = 11.sp,
                    )
                }
                Text("🏆", fontSize = 24.sp)
            }

            // بطاقات أوضاع اللعب
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // تحدي اليوم
                ModeCard(
                    title = "تحدي اليوم",
                    subtitle = "$totalDaily ألغاز · $dailyDone/$totalDaily مكتمل",
                    color = colors.accent,
                    icon = Icons.Filled.WbSunny,
                    colors = colors,
                    onClick = {
                        val nextLen = Dictionary.lengths.firstOrNull { !prefs.isDailyFinished(it) } ?: 5
                        onPlay(GameMode.DAILY, nextLen)
                    },
                )

                // بلا نهاية
                ModeCard(
                    title = "بلا نهاية",
                    subtitle = "واصل التخمين، وابنِ سلسلتك",
                    color = colors.correct,
                    icon = Icons.Filled.AllInclusive,
                    colors = colors,
                    onClick = { onPlay(GameMode.ENDLESS, 5) },
                )

                // عشوائي
                ModeCard(
                    title = "عشوائي",
                    subtitle = "طول كلمة مفاجئ",
                    color = colors.accent2,
                    icon = Icons.Filled.Casino,
                    colors = colors,
                    onClick = { onPlay(GameMode.RANDOM, Dictionary.lengths.random()) },
                )

                // وضع التعلّم
                ModeCard(
                    title = "وضع التعلّم 📚",
                    subtitle = "بطاقات وكلمات جديدة بدون ضغط",
                    color = Color(0xFF9C27B0),
                    icon = Icons.Filled.School,
                    colors = colors,
                    onClick = onLearn,
                )
            }

            // الوضع الصعب
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.card)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔥", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "الوضع الصعب",
                    color = colors.text,
                    fontSize = 13.sp,
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

            // اختيار سريع لطول الكلمة متمركز (يشمل أطوال 3 حتى 8)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "اختيار سريع لطول الكلمة",
                    color = colors.textDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                ) {
                    Dictionary.lengths.forEach { len ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(1.5.dp, colors.accent.copy(alpha = 0.4f), CircleShape)
                                .clickable { onPlay(GameMode.RANDOM, len) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$len",
                                color = colors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
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
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(1.dp))
        Text(text = label, color = colors.textDim, fontSize = 9.sp)
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
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = subtitle, color = colors.textDim, fontSize = 10.sp)
        }
    }
}
