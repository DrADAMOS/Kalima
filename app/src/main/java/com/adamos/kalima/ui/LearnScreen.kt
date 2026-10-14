package com.adamos.kalima.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager

import androidx.compose.foundation.layout.navigationBarsPadding

/** شاشة وضع التعلّم — بطاقات تعليمية وتحدي إملاء */
@Composable
fun LearnScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val colors: ThemeColors = ThemeManager.of(prefs.gameTheme)

    // بطاقات من كلمات 3 و4 حروف
    val cards = remember {
        (Dictionary.lengths.filter { it <= 4 })
            .flatMap { len -> (1..6).map { Dictionary.randomWord(len) } }
            .distinct()
    }

    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var guess by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }

    val word = cards.getOrElse(index) { "بيت" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader("📚 وضع التعلّم", colors, onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))

            // البطاقة
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.card)
                    .border(3.dp, colors.accent2.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!revealed) {
                        Text("🃏", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "كلمة من ${word.length} حروف",
                            color = colors.text,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "اكتبها في المربع أدناه 👇",
                            color = colors.textDim,
                            fontSize = 14.sp,
                        )
                    } else {
                        Text("💡", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = word,
                            color = colors.accent2,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // تحدث الإملاء
            OutlinedTextField(
                value = guess,
                onValueChange = {
                    guess = it
                    feedback = ""
                },
                label = { Text("اكتب الكلمة", color = colors.textDim) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryAction(
                    label = "تحقق",
                    color = colors.accent,
                    modifier = Modifier.weight(1f),
                ) {
                    val ok = Dictionary.normalize(guess) == Dictionary.normalize(word)
                    if (ok) {
                        feedback = "✅ أحسنت!"
                        score++
                        revealed = true
                    } else {
                        feedback = "❌ حاول مجددًا"
                    }
                }
                PrimaryAction(
                    label = "التالي",
                    color = colors.accent2,
                    modifier = Modifier.weight(1f),
                ) {
                    index = (index + 1) % cards.size.coerceAtLeast(1)
                    guess = ""
                    feedback = ""
                    revealed = false
                }
            }

            if (feedback.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = feedback,
                    color = if (feedback.startsWith("✅")) colors.correct else colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "النتيجة: $score",
                color = colors.accent2,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PrimaryAction(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

