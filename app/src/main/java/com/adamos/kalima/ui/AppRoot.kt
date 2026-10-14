package com.adamos.kalima.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.adamos.kalima.data.GameMode
import com.adamos.kalima.data.Preferences

/** الشاشات المتاحة في التطبيق */
enum class Screen { HOME, GAME, REWARDS, LEARN, SETTINGS, LEADERBOARD, HOW_TO_PLAY }

/**
 * جذر التطبيق — يدير التنقل بين الشاشات.
 */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val prefs = remember { Preferences(context) }
    var screen by remember { mutableStateOf(if (!prefs.howToSeen) Screen.HOW_TO_PLAY else Screen.HOME) }
    var gameMode by remember { mutableStateOf(GameMode.DAILY) }
    var gameLength by remember { mutableStateOf(5) }

    val handleHowToBack = {
        if (!prefs.howToSeen) {
            prefs.howToSeen = true
        }
        screen = Screen.HOME
    }

    when (screen) {
        Screen.HOME -> HomeScreen(
            onPlay = { mode, length ->
                gameMode = mode
                gameLength = length
                screen = Screen.GAME
            },
            onRewards = { screen = Screen.REWARDS },
            onLearn = { screen = Screen.LEARN },
            onSettings = { screen = Screen.SETTINGS },
            onLeaderboard = { screen = Screen.LEADERBOARD },
            onHowToPlay = { screen = Screen.HOW_TO_PLAY },
        )

        Screen.GAME -> GameScreen(
            mode = gameMode,
            initialLength = gameLength,
            onBack = { screen = Screen.HOME },
            onPlayAnother = { mode, len ->
                gameMode = mode
                gameLength = len
                screen = Screen.GAME
            },
        )

        Screen.REWARDS -> RewardsScreen(onBack = { screen = Screen.HOME })
        Screen.LEARN -> LearnScreen(onBack = { screen = Screen.HOME })
        Screen.SETTINGS -> SettingsScreen(
            onBack = { screen = Screen.HOME },
            onHowToPlay = { screen = Screen.HOW_TO_PLAY },
        )
        Screen.LEADERBOARD -> LeaderboardScreen(onBack = { screen = Screen.HOME })
        Screen.HOW_TO_PLAY -> HowToPlayScreen(onBack = handleHowToBack)
    }
}

