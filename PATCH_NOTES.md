# Kalima theme + dictionary patch

Extract the archive into the existing `D:\Android\Projects\Kalima` project folder and allow these four files to be replaced. This patch deliberately excludes build files, Gradle wrapper settings, `.idea`, `.git`, local.properties, and generated outputs so your existing build configuration stays untouched.

Changed files:
- `app/src/main/java/com/hazeralkalima/app/ui/theme/ThemeManager.kt`: child-friendly blue/gold adventure palette for the default theme.
- `app/src/main/java/com/hazeralkalima/app/ui/HomeScreen.kt`: adds a storybook-adventure banner using correctly spelled text and decorative icons; it does not copy the malformed letter tiles from the reference poster.
- `app/src/main/java/com/hazeralkalima/app/data/Dictionary.kt`: expands the Arabic answer pool across lengths 3–8; preserves canonical spelling (e.g. `مدرسة`) for display while normalizing only for comparisons; removes obvious malformed/incomplete entries.
- `app/src/test/java/com/hazeralkalima/app/DictionaryTest.kt`: adds tests for spelling preservation and length coverage.

Build/test status: source-level changes were checked for dictionary length coverage in the working environment, but Android Gradle compilation was not run here. Run `\.gradlew.bat testDebugUnitTest assembleDebug` in your existing project after applying the patch.
