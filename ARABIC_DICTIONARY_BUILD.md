# Kalima expanded Arabic guess dictionary

Kalima's previous bundled snapshot has only about 646 guess entries. These builders create a broader offline guess list from MustafaLinux/arabic-words-list (the upstream repository declares MIT licensing; its README warns that some errors and concatenated forms remain). The builders filter normalized Arabic-only words of 3–8 letters and merge snapshot guess entries in the Python builder. The secret answers list is never copied into this guess list.

## Selection policy

- Keep every accepted 3- and 4-letter candidate. These are core gameplay guesses and must not be lost because of a sampling stride.
- If the total would exceed the target budget, sample only words of 5–8 letters using a deterministic, evenly spaced selection from the sorted longer-word bucket.
- Always include the regression guesses جحش, سلب, رقى, and رقي. These are guesses only; this does not add them to the secret answer pool.
- The word list is not a linguistic validator. The upstream source acknowledges errors, so generated entries must remain in the guess dictionary and must not automatically become secret answers.

The --max-words option is a target for the source vocabulary after retaining all 3–4 letter candidates. The output can exceed it if the short-word bucket plus regression entries exceeds the target.

## Build on Windows

From the repository root in PowerShell:

    powershell -ExecutionPolicy Bypass -File .\tools\build_arabic_dictionary.ps1
    py -3 tools\build_arabic_dictionary.py
    py -3 -m unittest discover -s tools\tests -v
    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat assembleDebug

The builders download the upstream text file (about 47 MB) if it is not already available. The PowerShell builder caches it under %TEMP%; the Python builder uses tools/cache. You can also supply an existing source file:

    py -3 tools\build_arabic_dictionary.py --source D:\Downloads\arabic-words.txt
    powershell -ExecutionPolicy Bypass -File .\tools\build_arabic_dictionary.ps1 -SourcePath D:\Downloads\arabic-words.txt

The generated asset is app/src/main/assets/arabic-guess-words.txt. The app loads it line-by-line at startup and makes it available offline. The tests verify short-word selection, normalization, deduplication, the four regression guesses, and that snapshot answers are not copied into the guess list.

## Attribution and license

Source: https://github.com/MustafaLinux/arabic-words-list

The upstream repository declares MIT licensing. Keep its license and attribution when redistributing derived dictionary data. This project package includes the builder but does not bundle the upstream 47 MB source text itself.
