# Kalima expanded Arabic guess dictionary

Kalima's offline guess list is generated from MustafaLinux/arabic-words-list (MIT licensed). Strict quality filtering and bigram plausibility checks ensure noisy entries are rejected while real Arabic words are retained.

## Quality selection policy

- **Hamza filter:** Reject entries containing standalone hamza "ء" at the beginning or middle, or ending with "ء" unless following valid patterns (e.g. "...اء", "دفء", "ماء", "سماء").
- **Repeated letters:** Reject words with 3+ identical consecutive letters, and words with 2+ consecutive instances of non-doubleable letters (ا, ى, ة, ه, ء, ؤ, ئ, و, ي).
- **Glued prefixes:** Reject long words starting with concatenated prefix errors ("ولل", "وبال", "فبال", "وكال", "فكال", "واست", "فاست", "انامو") unless present in curated sets.
- **Bigram plausibility test:** Build a letter-bigram frequency table from curated words in `Dictionary.kt` (`baseWords`/`baseGuesses`) and `remote-dictionary-v1.json`. Candidates containing zero or near-zero frequency bigrams are dropped.
- **Optional frequency ranking:** An optional local frequency ranking list can be passed via `--freq` / `-FreqPath` to filter out low-frequency candidates.
- **Target budget:** Target final dictionary size is 30,000–60,000 normalized words (default target: 45,000). Every curated answer and regression entry (`جحش`, `سلب`, `رقى`, `رقي`, `سماء`, `مدرسة`, `كتاب`, `دفء`, `ماء`) is strictly preserved.

## Build commands

From the repository root in PowerShell:

    powershell -ExecutionPolicy Bypass -File .\tools\build_arabic_dictionary.ps1
    python3 tools/build_arabic_dictionary.py
    python3 tools/clean_existing_guess_list.py
    python3 -m unittest discover -s tools/tests -v
    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat assembleDebug

The generated asset is `app/src/main/assets/arabic-guess-words.txt`. The app loads it line-by-line at startup and makes it available offline.

## Attribution and license

Source: https://github.com/MustafaLinux/arabic-words-list (MIT License).
