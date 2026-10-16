Kalima dictionary builder (Windows PowerShell; no Python required)

1. Open PowerShell in the Kalima project root (D:\Android\Projects\Kalima).
2. Run:
   powershell -ExecutionPolicy Bypass -File .\tools\build_arabic_dictionary.ps1

The script downloads the public word list to tools/cache if needed, applies strict quality filters (Hamza position rules, repeated letter checks, glued prefix rejection, and a letter-bigram plausibility test against curated words), normalizes and deduplicates them, and samples entries to a target size of 30,000–60,000 words (45,000 by default). An optional local frequency ranking list can be supplied via -FreqPath.

To clean an existing guess list in place without downloading upstream sources:
   powershell -ExecutionPolicy Bypass -File .\tools\clean_existing_guess_list.ps1
   python3 tools/clean_existing_guess_list.py

The builder always preserves curated answers and guess-list regression entries:
   جحش, سلب, رقى, رقي, سماء, مدرسة, كتاب, دفء, ماء

It writes:
   app\src\main\assets\arabic-guess-words.txt

Then run:
   .\gradlew.bat testDebugUnitTest
   .\gradlew.bat assembleDebug

The project's Dictionary.kt loads arabic-guess-words.txt during application initialization.
