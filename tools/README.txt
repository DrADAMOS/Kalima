Kalima dictionary builder (Windows PowerShell; no Python required)

1. Open PowerShell in the Kalima project root (D:\Android\Projects\Kalima).
2. Run:
   powershell -ExecutionPolicy Bypass -File .\tools\build_arabic_dictionary.ps1

The script downloads the public word list to %TEMP% if needed, filters Arabic-only entries to 3–8 letters, normalizes and deduplicates them, retains every accepted 3- and 4-letter word, and samples only longer words if the target budget (150,000 by default) is exceeded.

The builder always verifies these guess-list regression entries:
   جحش, سلب, رقى, رقي

It writes:
   app\src\main\assets\arabic-guess-words.txt

Then run:
   .\gradlew.bat testDebugUnitTest
   .\gradlew.bat assembleDebug

The project's Dictionary.kt must load arabic-guess-words.txt during normal application initialization. Verify this integration if using a different checkout.

The source is a broad lexicon with acknowledged errors; the builder does not prove every entry is a valid word. It adds only to the guess dictionary and does not add entries to daily answer pools. The Python builder follows the same short-word retention policy and includes unit tests.
