package com.adamos.kalima.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager

/** لوحة المتصدرين — سجل السجلات الشخصية والإحصائيات المحلية للمستخدم */
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val currentTheme by prefs.themeState.collectAsState(initial = prefs.gameTheme)
    val colors: ThemeColors = ThemeManager.of(currentTheme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader("السجلات والإحصائيات", colors, onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "سجل إنجازاتك الشخصية وسلسلة الانتصارات في لعبة احزر الكلمة.",
                color = colors.textDim,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(24.dp))

            // بطاقة الأداء الأبرز
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.card)
                    .border(2.dp, colors.accent2.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👑", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("أفضل سلسلة انتصارات", color = colors.textDim, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${prefs.bestStreak} أيام متتالية", color = colors.accent2, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(20.dp))

            // الإحصائيات التفصيلية
            StatRecordRow("🎮", "إجمالي الجولات", "${prefs.gamesPlayed}", colors)
            StatRecordRow("🏆", "الانتصارات", "${prefs.gamesWon}", colors)
            StatRecordRow("🔥", "السلسلة الحالية", "${prefs.currentStreak}", colors)
            StatRecordRow("🌟", "التحديات اليومية المكتملة", "${prefs.dailyCompletedCount()} أطوال", colors)

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatRecordRow(icon: String, label: String, value: String, colors: ThemeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(14.dp))
        Text(label, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(value, color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}
