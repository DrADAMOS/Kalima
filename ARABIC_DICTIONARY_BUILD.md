# Kalima expanded Arabic guess dictionary

Kalima's previous bundled snapshot has only about 646 guess entries. This builder creates a much broader offline guess list from `MustafaLinux/arabic-words-list` (MIT-licensed upstream repository; its README estimates about 3.5 million unique surface forms and notes that some errors/concatenations remain). It filters to normalized Arabic-only words of 3–8 letters and merges the current snapshot's guess entries. The secret `answers` list is never expanded by this process.

## Build on Windows

From the repository root in PowerShell:

```powershell
py -3 tools\build_arabic_dictionary.py
py -3 -m unittest discover -s tools\tests -v
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The first command downloads the upstream text file (about 47 MB) if it is not already cached, then creates `app/src/main/assets/arabic-guess-words.txt`. If the development machine cannot reach GitHub, download `arabic-words.txt` from the upstream repository and run:

```powershell
py -3 tools\build_arabic_dictionary.py --source D:\Downloads\arabic-words.txt
```

The generated asset is loaded line-by-line at startup and is available offline. The build deliberately fails if the regression words `سلب`, `رقى`, or `رقي` are absent. This is a word-list, not a full linguistic validator: upstream says the list can contain a small percentage of spelling errors or joined words, so the app keeps the candidate list separate from secret answers.

## Attribution and license

Source: https://github.com/MustafaLinux/arabic-words-list
The upstream repository declares MIT licensing. Keep its `LICENSE` and attribution when redistributing the source dataset; this project package includes the builder but does not bundle the 47 MB source text itself.
