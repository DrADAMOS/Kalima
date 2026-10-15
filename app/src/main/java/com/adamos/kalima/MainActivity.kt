package com.adamos.kalima

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowInsetsControllerCompat
import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.data.DictionarySync
import com.adamos.kalima.data.Preferences
import com.adamos.kalima.ui.AppRoot
import com.adamos.kalima.ui.theme.KalimaTheme
import com.adamos.kalima.ui.theme.ThemeManager
import com.adamos.kalima.ui.theme.isLight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Dictionary.init(applicationContext)
        DictionarySync.initialize(applicationContext)
        // في onCreate نجعل الأشرطة شفافة فقط. لون الخلفية واتجاه الأيقونات
        // يُضبطان من KalimaApp حسب الثيم المختار.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        setContent {
            KalimaApp()
        }
    }
}

@Composable
fun KalimaApp() {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { Preferences(context) }
    val theme = prefs.gameTheme

    // الأيقونات الداكنة ضرورة على الثيمات الفاتحة، وإلا اختفت على الخلفية الفاتحة.
    // نضبطها عبر WindowInsetsControllerCompat وحدها: هي الآلية التي يرسمها
    // النظام فعليًا على API 35، بخلاف statusBarColor المهجورة.
    LaunchedEffect(theme) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val colors = ThemeManager.of(theme)
        WindowInsetsControllerCompat(activity.window, view).apply {
            isAppearanceLightStatusBars = colors.isLight()
            isAppearanceLightNavigationBars = colors.isLight()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        KalimaTheme(theme) {
            AppRoot()
        }
    }
}
