# Kalima Gameplay & Dictionary Quality Audit Patch

Project package: `com.adamos.kalima`

Changed files:
- `app/src/main/java/com/adamos/kalima/data/GameState.kt`: Fix hint placement and keyboard highlighting with persistent revealed letters, cursor skipping, and backspace protection.
- `app/src/main/java/com/adamos/kalima/data/Preferences.kt`: Expand badge unlocking checks for `streak_3`, `all_lengths`, and `speed_five`.
- `app/src/main/java/com/adamos/kalima/ui/GameScreen.kt`: Pass `revealedLetters` to `ArabicKeyboard` and ensure dynamic share result summary.
- `app/src/main/java/com/adamos/kalima/ui/SettingsScreen.kt`: Require parental PIN before toggling off safe dictionary, and update Kid Mode description.
- `app/src/main/java/com/adamos/kalima/ui/LeaderboardScreen.kt`: Replace fake hard-coded leaderboard with honest local personal records and stats.
- `app/src/main/java/com/adamos/kalima/ui/HomeScreen.kt`: Support all dictionary lengths (3–8) in daily mode and quick selection, fitting entirely without scrolling.
- `app/src/test/java/com/adamos/kalima/GameStateTest.kt`: Add regression tests for hint skipping and backspace protection.

Build/test status: Verified via `./gradlew testDebugUnitTest assembleDebug` (`21 tests completed, 0 failed`).
