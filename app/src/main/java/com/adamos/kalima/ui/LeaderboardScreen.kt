package com.adamos.kalima.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager

import androidx.compose.foundation.layout.navigationBarsPadding

private data class LeaderEntry(val rank: Int, val name: String, val score: Int, val tag: String? = null)

/** لوحة المتصدرين — عرض محلي (لاحقاً يمكن توصيله بخدمة) */
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val colors: ThemeColors = ThemeManager.of(prefs.gameTheme)

    val leaders = listOf(
        LeaderEntry(1, "فارس الكلمة", 23, "البطل"),
        LeaderEntry(2, "أمير الحروف", 13, "الوصيف"),
        LeaderEntry(3, "سارة", 9, "المركز الثالث"),
        LeaderEntry(4, "عبدالرحمن", 7),
        LeaderEntry(5, "ليلى", 6),
        LeaderEntry(6, "نور", 5),
        LeaderEntry(7, "محمد", 4),
        LeaderEntry(8, "يوسف", 3),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader("لوحة المتصدرين", colors, onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "سلسلة الانتصارات اليومية — الأيام المتتالية التي أكملت فيها تحديات اليوم.",
                color = colors.textDim,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )

            Spacer(Modifier.height(18.dp))

            // منصة التتويج
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom,
            ) {
                PodiumCard(leaders[1], Color(0xFFC0C0C0), colors)
                PodiumCard(leaders[0], Color(0xFFFFD700), colors)
                PodiumCard(leaders[2], Color(0xFFCD7F32), colors)
            }

            Spacer(Modifier.height(20.dp))

            // بقية القائمة
            leaders.drop(3).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${entry.rank}",
                        color = colors.textDim,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(entry.name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("${entry.score}", color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ترتيب اللاعب الحالي
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(1.dp, colors.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("أنت", color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${prefs.gamesWon} فوز · أفضل سلسلة ${prefs.bestStreak}",
                    color = colors.textDim,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PodiumCard(entry: LeaderEntry, medalColor: Color, colors: ThemeColors) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(medalColor.copy(alpha = 0.2f))
                .border(3.dp, medalColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.name.first().toString(),
                color = medalColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.name,
            color = colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        if (entry.tag != null) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(medalColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(entry.tag, color = medalColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${entry.score}",
            color = colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

