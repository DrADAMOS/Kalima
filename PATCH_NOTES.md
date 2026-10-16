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


# Round 5 — dictionary precision, kid-safe mode, hints, sync hardening

- `arabic-guess-words.txt`: 20,598 → ~4,600 entries. Kept only trusted words (curated lists, JSON snapshot, frequency list, regression guesses جحش/سلب/رقى/رقي) and plain inflections of them. Regenerate with `py -3 tools/refine_guess_list.py` (or `build_arabic_dictionary.py`, which now applies the same gate).
- `blocked-words.txt` (new asset) + built-in fallback: `Dictionary.isSafe()` now blocks whole words; `GameState(safeMode=true)` rejects them as guesses (`SubmitResult.BlockedWord`); GameScreen shows a message.
- Answer pool rebuilt: removed fillers ("...نا"/"...كم"/"...ي" forms and non-words), de-duplicated, headings now match real lengths. Answers per length: 3→335, 4→436, 5→426, 6→145, 7→65, 8→22 (8 letters is still thin).
- `useHint`: count-based check so repeated letters (e.g. ممرات) can be revealed.
- `DictionarySync`: requires date-style `version` not older than the cached one, caps at 50,000 words, rejects snapshots with >5% invalid entries, drops blocked words.
- `Dictionary.isReady`, Android guard for the JVM-only file lookup, `TooShort` removed.
- Plausibility rule fixed: no longer rejects real words with doubled ه/و/ي (فاكهة، طاووس، تخييم) or hamza before ة (قراءة).
- Tests added: DictionaryTest (blocklist, whole-word matching, pools, no fillers), GameStateTest (repeated-letter hints, safe mode), Python refine tests. Python suite: 10 tests pass. Kotlin tests were NOT run here (no Android SDK) — run `./gradlew testDebugUnitTest assembleDebug`.

# Round 7 — enrichment only (no words removed)
- Kept every existing word; appended ~490 new answers (3-8 letters) and ~60 common verbs as guess-only words. Normalized answer counts: 3->548, 4->662, 5->716, 6->294, 7->106, 8->29.
