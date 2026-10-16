package com.adamos.kalima.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.data.GameMode
import com.adamos.kalima.data.GameState
import com.adamos.kalima.data.LetterStatus
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.data.SubmitResult
import com.adamos.kalima.ui.components.ArabicKeyboard
import com.adamos.kalima.ui.components.ClassroomBackground
import com.adamos.kalima.ui.components.DecorativeObjectsBar
import com.adamos.kalima.ui.components.WordGrid
import com.adamos.kalima.ui.theme.GameTheme
import com.adamos.kalima.ui.theme.KalimaTheme
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager
import kotlinx.coroutines.delay

/** شاشة اللعب الرئيسية — هدر مقسم إلى صفين واضحين مع فصل تام عن شريط النظام */
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
    val currentTheme by prefs.themeState.collectAsState(initial = prefs.gameTheme)
    val colors: ThemeColors = ThemeManager.of(currentTheme)

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
            safeMode = safeOnly,
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
    var shakeToken by remember { mutableStateOf(0) }
    var revealToken by remember { mutableStateOf(0) }
    var revealRowIndex by remember { mutableStateOf(-1) }
    var isRevealing by remember { mutableStateOf(false) }
    var hintPulseToken by remember { mutableStateOf(0) }
    var playTimeVersion by remember { mutableStateOf(0) }
    val canPlayNow = remember(playTimeVersion, prefs.kidMode, prefs.dailyLimitMinutes) { prefs.canPlayNow() }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showResultSheet by remember { mutableStateOf(dailyFinished) }
    var unlockedBadgeName by remember(roundKey) { mutableStateOf<String?>(null) }
    var recordedInMem by remember(game) { mutableStateOf(false) }

    LaunchedEffect(revealToken) {
        if (revealToken > 0) {
            isRevealing = true
            delay((length - 1).coerceAtLeast(0) * 75L + 240L)
            isRevealing = false
        }
    }

    LaunchedEffect(game, prefs.kidMode, prefs.dailyLimitMinutes) {
        if (!prefs.kidMode || prefs.dailyLimitMinutes <= 0 || game.isGameOver) return@LaunchedEffect
        if (!prefs.canPlayNow()) {
            message = "انتهى وقت اللعب اليومي. ارجع غداً أو غيّر الحد من الإعدادات."
            messageVersion++
            return@LaunchedEffect
        }
        while (!game.isGameOver && prefs.canPlayNow()) {
            delay(60_000L)
            if (game.isGameOver) break
            prefs.addPlayMinutes(1)
            playTimeVersion++
            if (!prefs.canPlayNow()) {
                message = "انتهى وقت اللعب اليومي. ارجع غداً أو غيّر الحد من الإعدادات."
                messageVersion++
                break
            }
        }
    }

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
                val dailyAllDone = Dictionary.lengths.all { prefs.isDailyFinished(it) && prefs.getDailyResult(it)?.first == true }
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = this.maxHeight
        val isShortScreen = availableHeight < 640.dp

        // خلفية الفصل الدافئة (جدران بدون أي أيقونات مزيفة في الأعلى)
        ClassroomBackground(colors = colors)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
        ) {

            // الصف (أ) — عنوان اللعبة وأزرار التنقل الأساسية (أسفل شريط النظام تماماً)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent.copy(alpha = 0.18f))
                    .height(if (isShortScreen) 38.dp else 44.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = colors.text,
                    modifier = Modifier
                        .size(if (isShortScreen) 20.dp else 22.dp)
                        .clickable(onClick = onBack),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "$modeTitle • $length حروف",
                    color = colors.accentText,
                    fontSize = if (isShortScreen) 14.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "كيف تلعب",
                    tint = colors.text,
                    modifier = Modifier
                        .size(if (isShortScreen) 20.dp else 22.dp)
                        .clickable(onClick = { showHelpDialog = true }),
                )
            }

            // شريط التنبيهات (يتوسع فقط عند وجود رسالة تنبيه)
            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                message?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isShortScreen) 20.dp else 24.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.card)
                                .padding(horizontal = 10.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(msg, color = colors.text, fontSize = if (isShortScreen) 11.sp else 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // الصف (ب) — أدوات التحكم باللعبة والمساعدات الثانوية
            if (!game.isGameOver) {
                DecorativeObjectsBar(
                    hintCount = game.hintCount,
                    extraRowsRemaining = game.extraRows - game.extraRowsUsed,
                    colors = colors,
                    pulseHintToken = hintPulseToken,
                    isCompact = isShortScreen,
                    onAddRow = {
                        if (!game.addExtraRow()) {
                            message = "لا توجد صفوف إضافية"
                            messageVersion++
                        }
                        tick++
                    },
                    onUseHint = {
                        if (game.useHint()) {
                            hintPulseToken++
                        } else {
                            message = "لا توجد تلميحات"
                            messageVersion++
                        }
                        tick++
                    },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isShortScreen) 30.dp else 36.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isShortScreen) 28.dp else 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accent)
                            .clickable { showResultSheet = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "عرض النتيجة وتفاصيل الجولة 🏆",
                            color = colors.onAccent,
                            fontSize = if (isShortScreen) 12.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // الحروف الخاطئة المستبعدة
            val incorrectLetters = remember(game.usedLetters(), tick) {
                game.usedLetters().filter { it.value == LetterStatus.ABSENT }.keys.toList()
            }

            // 3D Cartoon Chalkboard Container (يأخذ المساحة المرنة المتبقية)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = if (isShortScreen) 2.dp else 4.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(colors.woodFrameGrad), RoundedCornerShape(16.dp))
                    .border(1.5.dp, colors.brassAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(if (isShortScreen) 5.dp else 8.dp) // Thick wooden frame
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.chalkboardBg)
                    .border(2.dp, Color(0xFF1B120B), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // Brass corner fasteners (4 metallic corner dots)
                Box(modifier = Modifier.fillMaxSize()) {
                    BrassBolt(Modifier.align(Alignment.TopStart))
                    BrassBolt(Modifier.align(Alignment.TopEnd))
                    BrassBolt(Modifier.align(Alignment.BottomStart))
                    BrassBolt(Modifier.align(Alignment.BottomEnd))

                    // Counter badge inside chalkboard top
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "المحاولات: ${game.attemptsUsed}/${game.totalRows}",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = if (isShortScreen) 10.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Chalk doodles (فقط على الشاشات غير القصيرة لعدم مزاحمة الشبكة)
                    if (!isShortScreen) {
                        Text(
                            text = "☀️",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 6.dp)
                                .alpha(0.30f),
                        )
                        Text(
                            text = "🌴",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp, top = 6.dp)
                                .alpha(0.30f),
                        )
                    }
                }

                WordGrid(
                    rows = game.rowsSnapshot(),
                    currentAttempt = game.currentAttempt,
                    currentColumn = game.currentColumn,
                    wordLength = length,
                    colors = colors,
                    isGameOver = game.isGameOver,
                    incorrectLetters = incorrectLetters,
                    shakeToken = shakeToken,
                    revealRowIndex = revealRowIndex,
                    revealToken = revealToken,
                )
                Text(text = tick.toString(), modifier = Modifier.size(0.dp).clearAndSetSemantics {}, color = Color.Transparent)
            }

            // Keyboard
            ArabicKeyboard(
                usedLetters = game.usedLetters(),
                colors = colors,
                enabled = !game.isGameOver && !dailyFinished && !isRevealing && canPlayNow,
                revealedLetters = game.hintRevealedLetters(),
                onNextRound = if (game.isGameOver) {
                    {
                        showResultSheet = false
                        roundKey++
                    }
                } else null,
                onKey = {
                    game.addLetter(it)
                    tick++
                },
                onEnter = {
                    if (game.isGameOver) return@ArabicKeyboard
                    val guess = game.currentRowLetters()
                    val submittedRowIndex = game.currentAttempt
                    when (game.submitGuess(guess)) {
                        SubmitResult.Win -> {
                            revealRowIndex = submittedRowIndex
                            revealToken++
                            message = "🎉 أحسنت!"
                            messageVersion++
                        }
                        SubmitResult.Lose -> {
                            revealRowIndex = submittedRowIndex
                            revealToken++
                            message = "😔 انتهت المحاولات"
                            messageVersion++
                        }
                        SubmitResult.NotRightLength -> {
                            shakeToken++
                            message = "الكلمة يجب أن تكون $length حروف"
                            messageVersion++
                        }
                        SubmitResult.InvalidWord -> {
                            shakeToken++
                            message = "أدخل حروفًا عربية فقط بلا أرقام أو رموز"
                            messageVersion++
                        }
                        SubmitResult.BlockedWord -> {
                            shakeToken++
                            message = "هذه الكلمة غير مناسبة في وضع الأطفال — جرّب كلمة أخرى"
                            messageVersion++
                        }
                        SubmitResult.NotInDictionary -> {
                            shakeToken++
                            message = "«$guess» ليست في قاموس اللعبة — جرّب كلمة أخرى"
                            messageVersion++
                        }
                        SubmitResult.BreaksGreen -> {
                            shakeToken++
                            message = "الوضع الصعب: أبقِ الحروف الخضراء في مواضعها"
                            messageVersion++
                        }
                        SubmitResult.MissingYellow -> {
                            shakeToken++
                            message = "الوضع الصعب: ضمّن الحروف الصفراء"
                            messageVersion++
                        }
                        SubmitResult.AlreadyOver -> Unit
                        SubmitResult.Accepted -> {
                            revealRowIndex = submittedRowIndex
                            revealToken++
                        }
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val resultScale = remember { androidx.compose.animation.core.Animatable(0.88f) }
                    val resultRotation = remember { androidx.compose.animation.core.Animatable(0f) }
                    LaunchedEffect(showResultSheet, game.isWon) {
                        if (showResultSheet) {
                            resultScale.snapTo(0.82f)
                            resultRotation.snapTo(0f)
                            resultScale.animateTo(
                                if (game.isWon) 1.08f else 0.96f,
                                animationSpec = spring(dampingRatio = 0.42f, stiffness = 300f),
                            )
                            resultScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f))
                            if (!game.isWon) {
                                resultRotation.animateTo(-4f, animationSpec = tween(55))
                                resultRotation.animateTo(4f, animationSpec = tween(55))
                                resultRotation.animateTo(-3f, animationSpec = tween(55))
                                resultRotation.animateTo(3f, animationSpec = tween(55))
                                resultRotation.animateTo(0f, animationSpec = spring())
                            }
                        }
                    }
                    Text(
                        text = if (game.isWon) "🎉 أحسنت!" else "😔 حظ أوفر",
                        color = if (game.isWon) colors.correct else colors.accent2,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.graphicsLayer(scaleX = resultScale.value, scaleY = resultScale.value, rotationZ = resultRotation.value),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "الكلمة: ${game.targetWord}",
                        color = colors.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    ) {
                        StatChip("المحاولات ${game.attemptsUsed}/${game.totalRows}", colors)
                        StatChip("السلسلة 🔥 ${prefs.currentStreak}", colors)
                        StatChip("الانتصارات ${prefs.gamesWon}", colors)
                    }

                    if (unlockedBadgeName != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accent.copy(alpha = 0.2f))
                                .border(1.dp, colors.accent, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "🏅 شارة جديدة: $unlockedBadgeName",
                                color = colors.accentText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    when (mode) {
                        GameMode.ENDLESS, GameMode.RANDOM -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.accent)
                                        .clickable {
                                            showResultSheet = false
                                            roundKey++
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("الكلمة التالية ◀", color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surface)
                                        .clickable(onClick = onBack),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("تغيير الطول", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        GameMode.DAILY -> {
                            val nextLen = Dictionary.lengths.firstOrNull { !prefs.isDailyFinished(it) }

                            if (nextLen != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.accent)
                                            .clickable {
                                                showResultSheet = false
                                                length = nextLen
                                                roundKey++
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("التحدي التالي", color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.surface)
                                            .clickable(onClick = onBack),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("الرئيسية", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    MidnightCountdown(colors)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.accent)
                                            .clickable(onClick = onBack),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("الرئيسية", color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .clickable {
                                val shareText = buildString {
                                    val scoreStr = if (game.isWon) "${game.attemptsUsed}/${game.totalRows}" else "X/${game.totalRows}"
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
                        Text("مشاركة النتيجة 📤", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/** مسمار زاوية معدني للنحاس */
@Composable
private fun BrassBolt(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(6.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFD700))
            .border(0.8.dp, Color(0xFFB8860B), CircleShape)
    )
}

@Composable
private fun StatChip(text: String, colors: ThemeColors) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
}

// ---------- Previews لمختلف أبعاد الشاشات ومستويات تكبير الخط ----------

@Preview(name = "Small Phone 360x640", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun GameScreenPreviewSmall() {
    KalimaTheme(theme = GameTheme.ORIGINAL) {
        GameScreen(
            mode = GameMode.DAILY,
            initialLength = 5,
            onBack = {},
            onPlayAnother = { _, _ -> },
        )
    }
}

@Preview(name = "Medium Phone 360x800", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
fun GameScreenPreviewMedium() {
    KalimaTheme(theme = GameTheme.ORIGINAL) {
        GameScreen(
            mode = GameMode.DAILY,
            initialLength = 5,
            onBack = {},
            onPlayAnother = { _, _ -> },
        )
    }
}

@Preview(name = "Large Phone 393x852", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
fun GameScreenPreviewLarge() {
    KalimaTheme(theme = GameTheme.ORIGINAL) {
        GameScreen(
            mode = GameMode.DAILY,
            initialLength = 5,
            onBack = {},
            onPlayAnother = { _, _ -> },
        )
    }
}

@Preview(name = "FontScale 1.5 - 360x640", widthDp = 360, heightDp = 640, fontScale = 1.5f, showBackground = true)
@Composable
fun GameScreenPreviewFontScale() {
    KalimaTheme(theme = GameTheme.ORIGINAL) {
        GameScreen(
            mode = GameMode.DAILY,
            initialLength = 5,
            onBack = {},
            onPlayAnother = { _, _ -> },
        )
    }
}
