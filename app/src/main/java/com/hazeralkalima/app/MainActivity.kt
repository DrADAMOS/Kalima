package com.hazeralkalima.app

import android.app.Activity
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
import com.hazeralkalima.app.data.Preferences
import com.hazeralkalima.app.ui.AppRoot
import com.hazeralkalima.app.ui.theme.KalimaTheme
import com.hazeralkalima.app.ui.theme.ThemeManager
import com.hazeralkalima.app.ui.theme.isLight
import com.hazeralkalima.app.ui.theme.systemBarArgb

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Preferences(this)
        val themeColors = ThemeManager.of(prefs.gameTheme)
        val isLight = themeColors.isLight()
        val barColor = themeColors.systemBarArgb()

        enableEdgeToEdge(
            statusBarStyle = if (isLight)
                SystemBarStyle.light(barColor, barColor)
            else
                SystemBarStyle.dark(barColor),
            navigationBarStyle = if (isLight)
                SystemBarStyle.light(barColor, barColor)
            else
                SystemBarStyle.dark(barColor),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
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

    // أشرطة النظام تتبع الثيم المختار
    LaunchedEffect(theme) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val colors = ThemeManager.of(theme)
        WindowInsetsControllerCompat(activity.window, view).apply {
            isAppearanceLightStatusBars = colors.isLight()
            isAppearanceLightNavigationBars = colors.isLight()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        KalimaTheme(theme = prefs.gameTheme) {
            AppRoot()
        }
    }
}
