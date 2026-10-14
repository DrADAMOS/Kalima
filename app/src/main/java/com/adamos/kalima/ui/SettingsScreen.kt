package com.adamos.kalima.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.theme.ThemeColors
import com.adamos.kalima.ui.theme.ThemeManager

/** شاشة الإعدادات — الثيم والمستوى والرقابة الأبوية */
@Composable
fun SettingsScreen(onBack: () -> Unit, onHowToPlay: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    val colors: ThemeColors = ThemeManager.of(prefs.gameTheme)

    var theme by remember { mutableStateOf(prefs.gameTheme) }
    var ageLevel by remember { mutableStateOf(prefs.ageLevel) }
    var kidMode by remember { mutableStateOf(prefs.kidMode) }
    var safeDict by remember { mutableStateOf(prefs.safeDictionaryOnly) }
    // يُزاد عند تغيير حالة PIN ليُعاد رسم صف الحماية فورًا
    var pinStateVersion by remember { mutableStateOf(0) }

    var pinDialogMode by remember { mutableStateOf<PinDialogMode?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }

    // يُقرأ عند كل إعادة رسم حتى يظهر التغيير بلا إعادة تشغيل الشاشة
    val hasPin = remember(pinStateVersion) { prefs.hasParentalPin }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // الشريط العلوي
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
            Text("الإعدادات", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            SectionTitle("🎨 ثيم اللعبة", colors)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeManager.all.forEach { t ->
                    val tc = ThemeManager.of(t)
                    val selected = theme == t
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tc.bg)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) tc.accent2 else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                theme = t
                                prefs.gameTheme = t
                            }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(tc.icon, fontSize = 20.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tc.label,
                            color = if (selected) tc.accent2 else tc.textDim,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("🐣 مستوى الصعوبة", colors)

            val ageLabels = listOf("🐣 مبتدئ (4–7 سنوات)", "🐥 متوسط (8–11 سنة)", "🐓 متقدم (12+)")
            ageLabels.forEachIndexed { index, label ->
                val selected = ageLevel == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) colors.accent.copy(alpha = 0.15f) else colors.surface)
                        .border(
                            width = 1.dp,
                            color = if (selected) colors.accent else Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            ageLevel = index
                            prefs.ageLevel = index
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = if (selected) colors.accent else colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    if (selected) Text("✓", color = colors.accent, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("🔒 الرقابة الأبوية", colors)

            ToggleRow(
                label = "👶 وضع الطفل فقط",
                desc = "يخفي الإعلانات والمشتريات",
                checked = kidMode,
                colors = colors,
            ) { newChecked ->
                when {
                    // تعطيل وضع الطفل بينما هناك PIN محمي -> طلب التحقق
                    !newChecked && kidMode && hasPin -> {
                        pinInput = ""
                        pinConfirmInput = ""
                        pinError = null
                        pinDialogMode = PinDialogMode.VERIFY_TO_UNLOCK
                    }
                    // تفعيل وضع الطفل بلا PIN -> اطلب ضبطه، ولا تُفعّل شيئًا قبل نجاحه
                    newChecked && !hasPin -> {
                        pinInput = ""
                        pinConfirmInput = ""
                        pinError = null
                        pinDialogMode = PinDialogMode.SET_NEW
                    }
                    // الحالة العادية
                    else -> {
                        kidMode = newChecked
                        prefs.kidMode = newChecked
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // صف حالة حماية PIN — يُقرأ من hasPin حتى يظهر التغيير فورًا
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (hasPin) "🔒 محمي بـ PIN" else "غير محمي — أضف PIN",
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    pinInput = ""
                    pinConfirmInput = ""
                    pinError = null
                    pinDialogMode = PinDialogMode.SET_NEW
                }) {
                    Text(
                        text = if (hasPin) "تغيير" else "إضافة",
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            ToggleRow(
                label = "📖 قاموس آمن فقط",
                desc = "تصفية الكلمات غير المناسبة",
                checked = safeDict,
                colors = colors,
            ) {
                safeDict = it
                prefs.safeDictionaryOnly = it
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("📖 المساعدة", colors)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .clickable(onClick = onHowToPlay)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("كيف تلعب وقواعد اللعبة", color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("←", color = colors.textDim, fontSize = 16.sp)
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("ℹ️ حول", colors)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(14.dp),
            ) {
                Text("الإصدار", color = colors.text, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("1.0.0", color = colors.textDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // حوار إدخال وتحقق الـ PIN
    if (pinDialogMode != null) {
        AlertDialog(
            onDismissRequest = {
                pinDialogMode = null
                pinError = null
            },
            title = {
                Text(
                    text = if (pinDialogMode == PinDialogMode.SET_NEW) "تعيين رمز PIN للرقابة الأبوية" else "أدخل رمز PIN للمتابعة",
                    color = colors.text,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("رمز PIN (4 أرقام فأكثر)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    if (pinDialogMode == PinDialogMode.SET_NEW) {
                        OutlinedTextField(
                            value = pinConfirmInput,
                            onValueChange = { pinConfirmInput = it },
                            label = { Text("تأكيد رمز PIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
                    if (pinError != null) {
                        Text(text = pinError!!, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    when (pinDialogMode) {
                        PinDialogMode.SET_NEW -> {
                            when {
                                pinInput.length < 4 || pinInput.any { !it.isDigit() } ->
                                    pinError = "يجب أن يكون الـ PIN مكونًا من 4 أرقام على الأقل"
                                pinInput != pinConfirmInput ->
                                    pinError = "الرمزان غير متطابقين"
                                prefs.setParentalPin(pinInput) -> {
                                    // لا يُفعَّل وضع الطفل إلا بعد نجاح ضبط الـ PIN
                                    kidMode = true
                                    prefs.kidMode = true
                                    pinStateVersion++
                                    pinDialogMode = null
                                    pinError = null
                                }
                                else -> pinError = "تعذّر حفظ الـ PIN"
                            }
                        }
                        PinDialogMode.VERIFY_TO_UNLOCK -> {
                            if (prefs.verifyParentalPin(pinInput)) {
                                kidMode = false
                                prefs.kidMode = false
                                pinDialogMode = null
                                pinError = null
                            } else {
                                pinError = "رمز PIN غير صحيح"
                            }
                        }
                        null -> {}
                    }
                }) {
                    Text("تم")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pinDialogMode = null
                    pinError = null
                }) {
                    Text("إلغاء", color = colors.textDim)
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String, colors: ThemeColors) {
    Text(
        text = text,
        color = colors.textDim,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun ToggleRow(
    label: String,
    desc: String,
    checked: Boolean,
    colors: ThemeColors,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(desc, color = colors.textDim, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent),
        )
    }
}

private enum class PinDialogMode { SET_NEW, VERIFY_TO_UNLOCK }

