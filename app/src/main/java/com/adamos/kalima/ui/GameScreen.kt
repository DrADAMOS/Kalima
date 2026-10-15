package com.adamos.kalima.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.data.GameMode
import com.adamos.kalima.data.GameState
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.data.SubmitResult
import com.adamos.kalima.ui.components.ArabicKeyboard
import com.adamos.kalima.ui.components.WordGrid
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager
import kotlinx.coroutines.delay

/** شاشة اللعب — الشبكة ولوحة المفاتيح ومنطق التخمين والنتائج */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    mode: GameMode,
    initialLength: Int,
    onBack: () -> Unit,
    onPlayAnother: (GameMode, Int) -> Unit,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val colors: ThemeColors = ThemeManager.of(prefs.gameTheme)

    var roundKey by remember { mutableStateOf(0) }
    var length by remember { mutableStateOf(initialLength) }
    val safeOnly = prefs.kidMode && prefs.safeDictionaryOnly

    val dailyFinished = mode == GameMode.DAILY && prefs.isDailyFinished(length)
    val dailyResult = if (dailyFinished) prefs.getDailyResult(length) else null

    val game = remember(mode, length, roundKey) {
        val target = when (mode) {
            GameMode.DAILY -> Dictionary.dailyWord(length, Preferences.todayKey(), safeOnly)
            else -> Dictionary.randomWord(length, safeOnly)
        }
        val g = GameState(
            mode = mode,
            wordLength = length,
            targetWord = target,
            hardMode = if (mode == GameMode.DAILY) false else prefs.hardMode,
        )
        if (dailyFinished && dailyResult != null) {
            val neededExtraRows = maxOf(0, dailyResult.second.size - 6)
            repeat(neededExtraRows) {
                g.addExtraRow()
            }
            dailyResult.second.forEach { guess ->
                g.submitGuess(guess)
            }
        }
        g
    }

    var message by remember { mutableStateOf<String?>(null) }
    var messageVersion by remember { mutableStateOf(0) }
    var tick by remember { mutableStateOf(0) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showResultSheet by remember { mutableStateOf(dailyFinished) }
    var unlockedBadgeName by remember(roundKey) { mutableStateOf<String?>(null) }
    var recordedInMem by remember(game) { mutableStateOf(false) }

    LaunchedEffect(message, messageVersion) {
        if (message != null) {
            delay(2600)
            message = null
        }
    }

    LaunchedEffect(game.isGameOver) {
        if (game.isGameOver) {
            val isDaily = mode == GameMode.DAILY
            val shouldRecord = if (isDaily) !dailyFinished else !recordedInMem
            if (shouldRecord) {
                if (!isDaily) recordedInMem = true
                if (isDaily) {
                    if (game.isWon) {
                        prefs.markDailyCompleted(length)
                    }
                    prefs.saveDailyResult(length, game.isWon, game.attempts)
                    prefs.recordGameOnce("daily_${length}_${Preferences.todayKey()}", game.isWon)
                } else {
                    prefs.recordNonDailyGame(game.isWon)
                }
                val dailyAllDone = (3..7).all { prefs.isDailyFinished(it) && prefs.getDailyResult(it)?.first == true }
                val badge = prefs.checkAndUnlockBadges(game.isWon, game.attemptsUsed, 2 - game.hintCount, dailyAllDone)
                unlockedBadgeName = badge
            }
            delay(700)
            showResultSheet = true
        }
    }

    val modeTitle = when (mode) {
        GameMode.DAILY -> "تحدي اليوم"
        GameMode.ENDLESS -> "بلا نهاية"
        GameMode.RANDOM -> "عشوائي"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header (56dp height with statusBarsPadding + displayCutoutPadding)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent.copy(alpha = 0.15f))
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = colors.text,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$modeTitle • $length حروف",
                    color = colors.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "كيف تلعب",
                    tint = colors.text,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = { showHelpDialog = true }),
                )
            }

            // Counter (28dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "المحاولات: ${game.attemptsUsed}/${game.totalRows}",
                    color = colors.textDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Message strip (32dp fixed height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(
                        visible = message != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        message?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.card)
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(msg, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Action buttons (44dp height)
            if (!game.isGameOver) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameActionButton(
                        label = "+1 صف (متبقي ${game.extraRows - game.extraRowsUsed})",
                        icon = Icons.Filled.Add,
                        color = colors.accent,
                        enabled = true,
                        colors = colors,
                        onClick = {
                            if (!game.addExtraRow()) {
                                message = "لا توجد صفوف إضافية"
                                messageVersion++
                            }
                            tick++
                        },
                    )
                    GameActionButton(
                        label = if (game.hintCount > 90) "تلميح ∞" else "تلميح (${game.hintCount})",
                        icon = Icons.Filled.Lightbulb,
                        color = colors.accent2,
                        enabled = game.hintCount > 0,
                        colors = colors,
                        onClick = {
                            if (!game.useHint()) {
                                message = "لا توجد تلميحات"
                                messageVersion++
                            }
                            tick++
                        },
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accent)
                            .clickable { showResultSheet = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "عرض النتيجة وتفاصيل الجولة",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Grid takes remaining space (weight 1f) and is centered
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                WordGrid(
                    rows = game.rowsSnapshot(),
                    currentAttempt = game.currentAttempt,
                    currentColumn = game.currentColumn,
                    wordLength = length,
                    colors = colors,
                    isGameOver = game.isGameOver,
                )
                tick.toString()
            }

            // Keyboard
            ArabicKeyboard(
                usedLetters = game.usedLetters(),
                colors = colors,
                enabled = !game.isGameOver && !dailyFinished,
                revealedLetters = game.hintRevealedLetters(),
                onKey = {
                    game.addLetter(it)
                    tick++
                },
                onEnter = {
                    if (game.isGameOver) return@ArabicKeyboard
                    val guess = game.currentRowLetters()
                    when (game.submitGuess(guess)) {
                        SubmitResult.Win -> {
                            message = "🎉 أحسنت!"
                            messageVersion++
                        }
                        SubmitResult.Lose -> {
                            message = "😔 انتهت المحاولات"
                            messageVersion++
                        }
                        SubmitResult.NotRightLength -> {
                            message = "الكلمة يجب أن تكون $length حروف"
                            messageVersion++
                        }
                        SubmitResult.InvalidWord -> {
                            message = "أدخل حروفًا عربية فقط بلا أرقام أو رموز"
                            messageVersion++
                        }
                        SubmitResult.TooShort -> {
                            message = "الكلمة يجب أن تكون $length حروف"
                            messageVersion++
                        }
                        SubmitResult.NotInDictionary -> {
                            message = "«$guess» ليست في قاموس اللعبة — جرّب كلمة أخرى"
                            messageVersion++
                        }
                        SubmitResult.BreaksGreen -> {
                            message = "الوضع الصعب: أبقِ الحروف الخضراء في مواضعها"
                            messageVersion++
                        }
                        SubmitResult.MissingYellow -> {
                            message = "الوضع الصعب: ضمّن الحروف الصفراء"
                            messageVersion++
                        }
                        SubmitResult.AlreadyOver -> Unit
                        SubmitResult.Accepted -> Unit
                    }
                    tick++
                },
                onBackspace = {
                    game.removeLetter()
                    tick++
                },
            )
        }

        // Help dialog overlay
        if (showHelpDialog) {
            Dialog(
                onDismissRequest = { showHelpDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bg),
                ) {
                    BackHandler { showHelpDialog = false }
                    HowToPlayScreen(onBack = { showHelpDialog = false })
                }
            }
        }

        // Result bottom sheet
        if (showResultSheet) {
            ModalBottomSheet(
                onDismissRequest = { showResultSheet = false },
                containerColor = colors.card,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (game.isWon) "🎉 أحسنت!" else "😔 حظ أوفر",
                        color = if (game.isWon) colors.correct else colors.accent2,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "الكلمة: ${game.targetWord}",
                        color = colors.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    ) {
                        StatChip("المحاولات ${game.attemptsUsed}/${game.totalRows}", colors)
                        StatChip("السلسلة 🔥 ${prefs.currentStreak}", colors)
                        StatChip("الانتصارات ${prefs.gamesWon}", colors)
                    }

                    if (unlockedBadgeName != null) {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accent.copy(alpha = 0.2f))
                                .border(1.dp, colors.accent, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "🏅 شارة جديدة: $unlockedBadgeName",
                                color = colors.accent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    when (mode) {
                        GameMode.ENDLESS, GameMode.RANDOM -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.accent)
                                        .clickable {
                                            showResultSheet = false
                                            roundKey++
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("الكلمة التالية ◀", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surface)
                                        .clickable(onClick = onBack),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("تغيير الطول", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        GameMode.DAILY -> {
                            val nextLen = (3..7).firstOrNull { !prefs.isDailyFinished(it) }

                            if (nextLen != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.accent)
                                            .clickable {
                                                showResultSheet = false
                                                length = nextLen
                                                roundKey++
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("التحدي التالي", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.surface)
                                            .clickable(onClick = onBack),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("الرئيسية", color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    MidnightCountdown(colors)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.accent)
                                            .clickable(onClick = onBack),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("الرئيسية", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .clickable {
                                val shareText = buildString {
                                    val scoreStr = if (game.isWon) "${game.attemptsUsed}/${game.totalRows}" else "X/6"
                                    append("احزر الكلمة — $scoreStr\n")
                                    game.attempts.forEach { guess ->
                                        val eval = game.evaluate(guess)
                                        val rowStr = eval.joinToString("") { status ->
                                            when (status) {
                                                2 -> "🟩"
                                                1 -> "🟨"
                                                else -> "⬛"
                                            }
                                        }
                                        append("\u200F").append(rowStr).append("\n")
                                    }
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة النتيجة"))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("مشاركة النتيجة 📤", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun GameActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    colors: ThemeColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatChip(text: String, colors: ThemeColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MidnightCountdown(colors: ThemeColors) {
    var timeRemaining by remember { mutableStateOf("00:00:00") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.util.Calendar.getInstance()
            val midnight = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val diff = midnight.timeInMillis - now.timeInMillis
            val hours = (diff / (1000 * 60 * 60)) % 24
            val minutes = (diff / (1000 * 60)) % 60
            val seconds = (diff / 1000) % 60
            timeRemaining = "%02d:%02d:%02d".format(hours, minutes, seconds)
            delay(1000)
        }
    }
    Text(
        text = "كلمة جديدة بعد $timeRemaining",
        color = colors.textDim,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

