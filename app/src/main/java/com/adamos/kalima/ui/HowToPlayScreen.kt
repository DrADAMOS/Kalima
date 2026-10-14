package com.adamos.kalima.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.GameMode
import com.adamos.kalima.data.GameState
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager
import kotlinx.coroutines.launch

/** شاشة شرح قواعد اللعبة (متحركة عبر صفحات) */
@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val colors: ThemeColors = ThemeManager.of(prefs.gameTheme)
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Bar with Skip / Back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "كيف تلعب",
                color = colors.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            if (pagerState.currentPage < 4) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("تخطّي", color = colors.textDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (page) {
                    0 -> PageGoal(colors)
                    1 -> PageColors(colors)
                    2 -> PageExample(colors)
                    3 -> PageNotes(colors)
                    4 -> PageAidsAndModes(colors)
                }
            }
        }

        // Bottom Indicators & Next / Start Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(5) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.accent else colors.textDim.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .clickable {
                        coroutineScope.launch {
                            if (pagerState.currentPage < 4) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onBack()
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (pagerState.currentPage < 4) "التالي" else "ابدأ اللعب",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PageGoal(colors: ThemeColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(20.dp))
        Text("🎯", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "هدف اللعبة",
            color = colors.accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
                .padding(20.dp),
        ) {
            Text(
                text = "خمّن الكلمة المخفية في ٦ محاولات. اكتب أي كلمة عربية بنفس عدد الحروف ثم اضغط «إرسال». يتم اختيار طول الكلمة من الشاشة الرئيسية (من ٣ إلى ٧ حروف).",
                color = colors.text,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PageColors(colors: ThemeColors) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "معاني الألوان",
            color = colors.accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(20.dp))
        ColorGuide("أ", colors.correct, "أخضر: الحرف صحيح وفي مكانه", colors, "حرف أ، صحيح وفي مكانه الصحيح")
        Spacer(Modifier.height(16.dp))
        ColorGuide("ب", colors.present, "أصفر: الحرف موجود لكن في مكان آخر", colors, "حرف ب، موجود ولكن في مكان خاطئ")
        Spacer(Modifier.height(16.dp))
        ColorGuide("ج", colors.absent, "رمادي: الحرف غير موجود في الكلمة", colors, "حرف ج، غير موجود في الكلمة")
    }
}

@Composable
private fun ColorGuide(letter: String, color: Color, label: String, colors: ThemeColors, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color)
                .semantics { contentDescription = desc },
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PageExample(colors: ThemeColors) {
    val targetWord = "كتاب"
    val guess1 = "كلاب"
    val guess2 = "بيتك"

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "مثال توضيحي",
            color = colors.accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "الكلمة السرية مؤلفة من 4 حروف (مثال: كتاب)",
            color = colors.textDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        // Guess 1
        ExampleAttemptCard(guess = guess1, target = targetWord, description = "التخمين الأول: كلاب", colors = colors)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "حرف (ك) و(ا) و(ب) في مكانها الصحيح (أخضر)، بينما (ل) غير موجود (رمادي).",
            color = colors.textDim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        // Guess 2
        ExampleAttemptCard(guess = guess2, target = targetWord, description = "التخمين الثاني: بيتك", colors = colors)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "حرف (ب) و(ت) و(ك) موجودة لكن في أماكن خاطئة (أصفر)، و(ي) غير موجود.",
            color = colors.textDim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExampleAttemptCard(guess: String, target: String, description: String, colors: ThemeColors) {
    val tempGame = remember(target) { GameState(GameMode.RANDOM, target.length, target) }
    val evaluation = tempGame.evaluate(guess)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(description, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            guess.forEachIndexed { i, char ->
                val status = evaluation.getOrNull(i) ?: 0
                val bg = when (status) {
                    2 -> colors.correct
                    1 -> colors.present
                    else -> colors.absent
                }
                val desc = when (status) {
                    2 -> "حرف $char، صحيح وفي مكانه"
                    1 -> "حرف $char، موجود في مكان آخر"
                    else -> "حرف $char، غير موجود"
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .semantics { contentDescription = desc },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = char.toString(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageNotes(colors: ThemeColors) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "ملاحظات هامة",
            color = colors.accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(16.dp))

        NoteItem("الألف والهمزة سواء", "أ إ آ = ا", colors)
        Spacer(Modifier.height(12.dp))
        NoteItem("التاء المربوطة والهاء", "ة = ه", colors)
        Spacer(Modifier.height(12.dp))
        NoteItem("الحروف المكررة", "الحرف المكرر يظهر رماديًا إذا لم يتكرر بالعدد نفسه في الكلمة.", colors)
    }
}

@Composable
private fun NoteItem(title: String, desc: String, colors: ThemeColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .padding(16.dp),
    ) {
        Column {
            Text(title, color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(desc, color = colors.textDim, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun PageAidsAndModes(colors: ThemeColors) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "المساعدات والأوضاع",
            color = colors.accent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(14.dp))

        ModeItem("+١ صف", "محاولة إضافية (مرتان فقط في الجولة)", colors)
        Spacer(Modifier.height(10.dp))
        ModeItem("تلميح", "يكشف حرفًا صحيحًا في مكانه (تلميحان)", colors)
        Spacer(Modifier.height(10.dp))
        ModeItem("تحدي اليوم", "كلمة ثابتة وموحدة لكل اللاعبين كل يوم", colors)
        Spacer(Modifier.height(10.dp))
        ModeItem("بلا نهاية", "لعب متواصل وبدون توقف", colors)
        Spacer(Modifier.height(10.dp))
        ModeItem("عشوائي", "كلمة جديدة عشوائية في كل جولة", colors)
        Spacer(Modifier.height(10.dp))
        ModeItem("الوضع الصعب", "إلزام الحفاظ على الحروف الصفراء والخضراء", colors)
    }
}

@Composable
private fun ModeItem(title: String, desc: String, colors: ThemeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("•", color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = colors.textDim, fontSize = 12.sp)
        }
    }
}

