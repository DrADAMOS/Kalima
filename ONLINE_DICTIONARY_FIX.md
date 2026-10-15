# Kalima online dictionary sync fix

This patch keeps the bundled dictionary as the offline fallback and adds a background refresh from:

`https://raw.githubusercontent.com/DrADAMOS/Kalima/main/dictionary/remote-dictionary-v1.json`

Changes:
- Adds Android `INTERNET` permission.
- Starts `DictionarySync` after loading the bundled dictionary.
- Loads a cached successful snapshot first, then refreshes the JSON on a background executor.
- Validates schema version and Arabic guess lengths before merging downloaded words into a thread-safe set.
- Logs HTTP/parse/network failures instead of silently hiding them.
- Adds a unit test for merging downloaded words.

The current bundled JSON includes `مضض`, so the game should accept it offline too, provided the submitted word is being checked through `Dictionary.isGuessable`.

Build verification was attempted with `./gradlew testDebugUnitTest assembleDebug`, but the environment could not resolve `services.gradle.org` to download Gradle. This patch has therefore **not** been claimed as build-verified. Re-run the unit tests and debug build in Android Studio or on the user's Windows machine.


## Follow-up hardening (2026-10-15)

- Replaced regex scraping of the bundled JSON with schema-checked JSON parsing of the `answers` and `guesses` arrays only.
- Normalization now removes Arabic tashkeel and tatweel and normalizes common alef variants before validation and comparison.
- `isValidGuess` validates the normalized form, so tashkeel does not cause a valid Arabic guess to be rejected as malformed.
- Added unit tests for online-word insertion, keeping guesses separate from secret answers, and Arabic normalization.
- The app build/tests could not be executed in this environment because the Gradle wrapper distribution host (`services.gradle.org`) could not be resolved. Run tests and build locally before installing.
