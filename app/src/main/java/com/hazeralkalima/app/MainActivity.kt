package com.hazeralkalima.app

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
import androidx.compose.runtime.SideEffect
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
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
    val activity = context as? Activity
    val prefs = remember { Preferences(context) }
    val theme = prefs.gameTheme

    SideEffect {
        if (activity == null) return@SideEffect
        val colors = ThemeManager.of(theme)
        val color = colors.systemBarArgb()
        val light = colors.isLight()

        if (activity is ComponentActivity) {
            activity.enableEdgeToEdge(
                statusBarStyle = if (light) SystemBarStyle.light(color, color)
                                 else SystemBarStyle.dark(color),
                navigationBarStyle = if (light) SystemBarStyle.light(color, color)
                                     else SystemBarStyle.dark(color),
            )
        }

        WindowInsetsControllerCompat(activity.window, view).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        KalimaTheme(theme) {
            AppRoot()
        }
    }
}
