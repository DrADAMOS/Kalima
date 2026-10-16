Kalima review fixes — 2026-10-15

Apply these files over the matching paths in the current Kalima project. This is a focused source patch, not a complete project.

Fixed:
- GameState tracks hinted cells explicitly; typed EMPTY cells are no longer mistaken for hints.
- Backspace skips hinted cells but deletes typed letters. Added regression tests for backspace and keyboard hint highlighting.
- Daily challenge completion/next challenge uses Dictionary.lengths, including 8-letter puzzles. Loss share score uses the actual total row count instead of hard-coded X/6.
- Added contrast-safe accentText and darker textDim palette values, with contrast tests for light backgrounds and white surfaces.
- Added home floating themed glyphs, staggered mode-card entrance, and screen transitions.
- Set the launch window background from the saved theme and load the large dictionary assets on an IO dispatcher.
- Added a simple parental daily play-time limit selector (unlimited/15/30/60 minutes), session minute tracking, and keyboard blocking when the limit is reached.
- The expanded dictionary loader rejects obvious degenerate entries consisting of one repeated character or three identical consecutive characters.
- Increased some small settings/rewards/home labels.

Validation:
- Source review and targeted static checks performed.
- Android unit tests/build could NOT be executed in this environment because the Gradle 9.6.0 wrapper distribution download failed with UnknownHostException for services.gradle.org.
- The dictionary cleanup is intentionally conservative: it removes obvious generated noise but does NOT claim to linguistically validate the whole 300k-word list. A curated/validated lexicon remains open work.
- No commit or push was made.

After applying, run on Windows PowerShell:
  .\gradlew.bat testDebugUnitTest
  .\gradlew.bat assembleDebug
