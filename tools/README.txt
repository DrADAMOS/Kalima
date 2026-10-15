Kalima dictionary builder (Windows PowerShell; no Python required)

1. Extract this folder into your Kalima project root (D:\Android\Projects\Kalima), so tools\build_arabic_dictionary.ps1 is inside that project.
2. Open PowerShell in the Kalima project root.
3. Run:
   powershell -ExecutionPolicy Bypass -File .\tools\build_arabic_dictionary.ps1

The script downloads the public word list to %TEMP%, filters Arabic-only entries to 3-8 letters, deduplicates them using Kalima-like normalization, selects up to 150,000 entries distributed across the source, and writes:
   app\src\main\assets\arabic-guess-words.txt

Then run:
   .\gradlew.bat testDebugUnitTest
   .\gradlew.bat assembleDebug

The project's Dictionary.kt must contain loadExpandedGuessAsset(context) and call it from Dictionary.init(context). The Kalima expanded-dictionary builder project previously supplied contains this integration. If using a different local checkout, verify that before building.

The source list is a broad lexicon with acknowledged errors; this builder does not prove every entry is a valid word. It only adds to guessable words and does not add entries to daily answer pools.
