package com.adamos.kalima.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.data.GameMode
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.theme.GameTheme
import com.adamos.kalima.ui.theme.KalimaTheme
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager
import kotlinx.coroutines.delay

/** الشاشة الرئيسية — اختيار الوضع والدخول للعبة (تظهر بالكامل بدون تمرير مع استجابة مرنة لكافة الشاشات) */
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
    val currentTheme by prefs.themeState.collectAsState(initial = prefs.gameTheme)
    val colors: ThemeColors = ThemeManager.of(currentTheme)

    var hardMode by remember { mutableStateOf(prefs.hardMode) }
    val totalDaily = Dictionary.lengths.size
    var dailyDone by remember { mutableStateOf(prefs.dailyCompletedCount()) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val floatTransition = rememberInfiniteTransition(label = "home-floating-glyphs")
    val floatY by floatTransition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(animation = tween(2400), repeatMode = RepeatMode.Reverse),
        label = "home-floating-y",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        val availableHeight = this.maxHeight
        val availableWidth = this.maxWidth
        val isCompactHeight = availableHeight < 640.dp
        val isUltraCompact = availableHeight < 560.dp

        // الرموز العائمة في الخلفية (zIndex -1 لمنع حجب العناصر التفاعلية)
        if (!isCompactHeight) {
            Text(
                text = colors.bgGlyphs.firstOrNull().orEmpty(),
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = (availableHeight * 0.08f).coerceIn(40.dp, 80.dp))
                    .zIndex(-1f)
                    .graphicsLayer { translationY = floatY; alpha = 0.18f },
            )
            Text(
                text = colors.bgGlyphs.getOrNull(1).orEmpty(),
                fontSize = 24.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = (availableHeight * 0.08f).coerceIn(40.dp, 80.dp))
                    .zIndex(-1f)
                    .graphicsLayer { translationY = -floatY; alpha = 0.18f },
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 15 },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = (availableWidth * 0.04f).coerceIn(12.dp, 24.dp),
                        vertical = (availableHeight * 0.01f).coerceIn(4.dp, 12.dp)
                    ),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. شريط الأيقونات العلوي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderIcon(Icons.AutoMirrored.Filled.HelpOutline, "كيف تلعب", colors, isCompactHeight, onHowToPlay)
                    HeaderIcon(Icons.Filled.EmojiEvents, "السجلات", colors, isCompactHeight, onLeaderboard)
                    HeaderIcon(Icons.Filled.School, "التعلّم", colors, isCompactHeight, onLearn)
                    HeaderIcon(Icons.Filled.Settings, "الإعدادات", colors, isCompactHeight, onSettings)
                }

                // 2. عنوان اللعبة المصغّر المتكيف
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val titleSize = if (isUltraCompact) 20.sp else if (isCompactHeight) 22.sp else 26.sp
                    Text(
                        text = "احزر الكلمة",
                        color = colors.text,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!isUltraCompact) {
                        Text(
                            text = "خمّن الكلمة!",
                            color = colors.textDim,
                            fontSize = if (isCompactHeight) 11.sp else 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // 3. لوحة الترحيب المتكيفة
                if (!isUltraCompact) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(
                                horizontal = 12.dp,
                                vertical = if (isCompactHeight) 4.dp else 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "رحلة الكلمات تبدأ هنا!",
                                color = colors.accentText,
                                fontSize = if (isCompactHeight) 13.sp else 14.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "اكتشف مفردات واجمع النجوم ⭐",
                                color = colors.text,
                                fontSize = if (isCompactHeight) 11.sp else 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text("🏆", fontSize = if (isCompactHeight) 20.sp else 24.sp)
                    }
                }

                // 4. بطاقات أوضاع اللعب (تخطيط 2×2 في الشاشات القصيرة أو عمودي في الشاشات العادية)
                if (isCompactHeight) {
                    // شبكة 2×2 للشاشات القصيرة
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CompactModeCard(
                                title = "تحدي اليوم",
                                subtitle = "$dailyDone/$totalDaily مكتمل",
                                color = colors.accent,
                                icon = Icons.Filled.WbSunny,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (!prefs.canPlayNow()) {
                                        Toast.makeText(context, "انتهى وقت اللعب اليومي.", Toast.LENGTH_LONG).show()
                                    } else {
                                        val nextLen = Dictionary.lengths.firstOrNull { !prefs.isDailyFinished(it) } ?: 5
                                        onPlay(GameMode.DAILY, nextLen)
                                    }
                                },
                            )
                            CompactModeCard(
                                title = "بلا نهاية",
                                subtitle = "ابنِ سلسلتك",
                                color = colors.correct,
                                icon = Icons.Filled.AllInclusive,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (prefs.canPlayNow()) onPlay(GameMode.ENDLESS, 5)
                                    else Toast.makeText(context, "انتهى وقت اللعب اليومي.", Toast.LENGTH_LONG).show()
                                },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CompactModeCard(
                                title = "عشوائي",
                                subtitle = "طول مفاجئ",
                                color = colors.accent2,
                                icon = Icons.Filled.Casino,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (prefs.canPlayNow()) onPlay(GameMode.RANDOM, Dictionary.lengths.random())
                                    else Toast.makeText(context, "انتهى وقت اللعب اليومي.", Toast.LENGTH_LONG).show()
                                },
                            )
                            CompactModeCard(
                                title = "التعلّم 📚",
                                subtitle = "بطاقات كلمات",
                                color = Color(0xFF9C27B0),
                                icon = Icons.Filled.School,
                                colors = colors,
                                modifier = Modifier.weight(1f),
                                onClick = onLearn,
                            )
                        }
                    }
                } else {
                    // قائمة عمودية للشاشات العادية والكبيرة
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ModeCard(
                            title = "تحدي اليوم",
                            subtitle = "$totalDaily ألغاز · $dailyDone/$totalDaily مكتمل",
                            color = colors.accent,
                            icon = Icons.Filled.WbSunny,
                            colors = colors,
                            onClick = {
                                if (!prefs.canPlayNow()) {
                                    Toast.makeText(context, "انتهى وقت اللعب اليومي. غيّر الحد من الإعدادات أو ارجع غداً.", Toast.LENGTH_LONG).show()
                                } else {
                                    val nextLen = Dictionary.lengths.firstOrNull { !prefs.isDailyFinished(it) } ?: 5
                                    onPlay(GameMode.DAILY, nextLen)
                                }
                            },
                        )
                        ModeCard(
                            title = "بلا نهاية",
                            subtitle = "واصل التخمين، وابنِ سلسلتك",
                            color = colors.correct,
                            icon = Icons.Filled.AllInclusive,
                            colors = colors,
                            onClick = {
                                if (prefs.canPlayNow()) onPlay(GameMode.ENDLESS, 5)
                                else Toast.makeText(context, "انتهى وقت اللعب اليومي. غيّر الحد من الإعدادات أو ارجع غداً.", Toast.LENGTH_LONG).show()
                            },
                        )
                        ModeCard(
                            title = "عشوائي",
                            subtitle = "طول كلمة مفاجئ",
                            color = colors.accent2,
                            icon = Icons.Filled.Casino,
                            colors = colors,
                            onClick = {
                                if (prefs.canPlayNow()) onPlay(GameMode.RANDOM, Dictionary.lengths.random())
                                else Toast.makeText(context, "انتهى وقت اللعب اليومي. غيّر الحد من الإعدادات أو ارجع غداً.", Toast.LENGTH_LONG).show()
                            },
                        )
                        ModeCard(
                            title = "وضع التعلّم 📚",
                            subtitle = "بطاقات وكلمات جديدة بدون ضغط",
                            color = Color(0xFF9C27B0),
                            icon = Icons.Filled.School,
                            colors = colors,
                            onClick = onLearn,
                        )
                    }
                }

                // 5. مفتاح الوضع الصعب المتكيف
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.card)
                        .padding(horizontal = 10.dp, vertical = if (isCompactHeight) 2.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔥", fontSize = if (isCompactHeight) 14.sp else 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "الوضع الصعب",
                        color = colors.text,
                        fontSize = if (isCompactHeight) 11.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Switch(
                        checked = hardMode,
                        onCheckedChange = {
                            hardMode = it
                            prefs.hardMode = it
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.accent),
                        modifier = Modifier.graphicsLayer(scaleX = 0.85f, scaleY = 0.85f),
                    )
                }

                // 6. اختيار سريع لطول الكلمة متمركز لشاشات الحجم 3 حتى 8
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "اختيار سريع لطول الكلمة",
                        color = colors.textDim,
                        fontSize = if (isCompactHeight) 10.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    ) {
                        val btnSize = (availableWidth.value * 0.09f).dp.coerceIn(26.dp, 36.dp)
                        Dictionary.lengths.filter { len -> len < 7 || (Dictionary.wordsForLength(len, onlySafe = false).size >= 60) }.forEach { len ->
                            Box(
                                modifier = Modifier
                                    .size(btnSize)
                                    .clip(CircleShape)
                                    .background(colors.card)
                                    .border(1.2.dp, colors.accent.copy(alpha = 0.4f), CircleShape)
                                    .clickable { onPlay(GameMode.RANDOM, len) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$len",
                                    color = colors.text,
                                    fontSize = if (btnSize < 30.dp) 11.sp else 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
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
    isCompact: Boolean,
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
            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp),
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            color = colors.textDim,
            fontSize = if (isCompact) 10.sp else 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = colors.textDim,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactModeCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    colors: ThemeColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = colors.textDim,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------- Previews لكافة أحجام الشاشات ومعاملات الخروج والخط الكبير ----------

@Preview(name = "Small Phone 360x640", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun HomeScreenPreviewSmall() {
    KalimaTheme(theme = GameTheme.ORIGINAL) {
        HomeScreen(
            onPlay = { _, _ -> },
            onRewards = {},
            onLearn = {},
            onSettings = {},
            onLeaderboard = {},
            onHowToPlay = {},
        )
    }
}

@Preview(name = "Standard Phone 411x891", widthDp = 411, heightDp = 891, showBackground = true)
@Composable
fun HomeScreenPreviewStandard() {
    KalimaTheme(theme = GameTheme.ORIGINAL) {
        HomeScreen(
            onPlay = { _, _ -> },
            onRewards = {},
            onLearn = {},
            onSettings = {},
            onLeaderboard = {},
            onHowToPlay = {},
        )
    }
}

@Preview(name = "Tablet 800x1280", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun HomeScreenPreviewTablet() {
    KalimaTheme(theme = GameTheme.ORIGINAL) {
        HomeScreen(
            onPlay = { _, _ -> },
            onRewards = {},
            onLearn = {},
            onSettings = {},
            onLeaderboard = {},
            onHowToPlay = {},
        )
    }
}

@Preview(name = "Font Scale 1.5 - 360x640", widthDp = 360, heightDp = 640, fontScale = 1.5f, showBackground = true)
@Composable
fun HomeScreenPreviewFontScale() {
    KalimaTheme(theme = GameTheme.ORIGINAL) {
        HomeScreen(
            onPlay = { _, _ -> },
            onRewards = {},
            onLearn = {},
            onSettings = {},
            onLeaderboard = {},
            onHowToPlay = {},
        )
    }
}
