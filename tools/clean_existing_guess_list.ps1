# Clean Kalima assets/arabic-guess-words.txt in place with High Precision Base Word filtering

$code = @"
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

public class HighPrecisionCleanerRunner {
    private static readonly HashSet<char> Undoubleable = new HashSet<char> {
        '\u0627', '\u0649', '\u0629', '\u0647', '\u0621', '\u0624', '\u0626', '\u0648', '\u064A'
    };

    private static readonly HashSet<string> AllowedHamzaEnds = new HashSet<string> {
        "\u0627\u0621", "\u0648\u0621", "\u064A\u0621", "\u0641\u0621", "\u0628\u0621", "\u0632\u0621", "\u062F\u0621", "\u0643\u0621", "\u0631\u0621", "\u0646\u0621", "\u0644\u0621", "\u0637\u0621", "\u062B\u0621"
    };

    // Correctly ordered long-to-short suffixes to prevent prefix-shadowing bugs
    private static readonly string[] InflectedSuffixes = new string[] {
        "\u062A\u0643\u0645\u0627", "\u062A\u0647\u0645\u0627", "\u062A\u0647\u0645", "\u062A\u0643\u0645", "\u062A\u0647\u0627", "\u062A\u0643\u0646", "\u062A\u0645\u0627", "\u062A\u0646",
        "\u0647\u0645\u0627", "\u0643\u0645\u0627", "\u0647\u0645", "\u0647\u0646", "\u0643\u0645", "\u0643\u0646", "\u0646\u0627", "\u0646\u064A", "\u0647\u0627", "\u0643", "\u0647", "\u064A", "\u0648\u0627"
    };

    private static readonly HashSet<string> FunctionWordsBlocklist = new HashSet<string> {
        "\u0644\u0647\u0645", "\u0628\u0647\u0645", "\u0645\u0646\u0647\u0645", "\u0639\u0646\u0647\u0645", "\u0625\u0644\u064A\u0647\u0645", "\u0639\u0644\u064A\u0647\u0645", "\u0641\u064A\u0647\u0627", "\u0641\u064A\u0647", "\u0641\u064A\u0647\u0645", "\u0645\u0646\u0647\u0627", "\u0639\u0646\u0647\u0627", "\u0625\u0644\u064A\u0647\u0627", "\u0639\u0644\u064A\u0647\u0627", "\u0644\u0643\u0645", "\u0628\u0643\u0645", "\u0645\u0646\u0643\u0645", "\u0639\u0646\u0643\u0645", "\u0625\u0644\u064A\u0643\u0645", "\u0639\u0644\u064A\u0643\u0645",
        "\u0644\u0646\u0639", "\u0627\u0647\u0644", "\u0643\u0644\u0647\u0645", "\u0628\u0639\u0636\u0647\u0645", "\u063A\u064A\u0631\u0647\u0645"
    };

    private static readonly HashSet<string> NativePrefixExceptions = new HashSet<string> {
        "\u0648\u0631\u062F", "\u0648\u0644\u062F", "\u0648\u0632\u0646", "\u0648\u0641\u0649", "\u0648\u0639\u062F", "\u0648\u0642\u062F", "\u0648\u062C\u062F", "\u0648\u062C\u0647", "\u0648\u0637\u0646", "\u0648\u0642\u062A",
        "\u0641\u064A\u0644", "\u0641\u0623\u0631", "\u0641\u0647\u062F", "\u0641\u0635\u0644", "\u0641\u0643\u0631", "\u0641\u0631\u062D", "\u0641\u0648\u0632", "\u0641\u062C\u0631", "\u0641\u0646", "\u0641\u0631\u0646",
        "\u0628\u0627\u0628", "\u0628\u064A\u062A", "\u0628\u062D\u0631", "\u0628\u0631\u0642", "\u0628\u0646\u062A", "\u0628\u0631\u062C", "\u0628\u0626\u0631", "\u0628\u0648\u0645\u0629", "\u0628\u0630\u0648\u0631",
        "\u0644\u064A\u0644", "\u0644\u062D\u0645", "\u0644\u0628\u0646", "\u0644\u0648\u0646", "\u0644\u0639\u0628\u0629", "\u0644\u0633\u0627\u0646", "\u0644\u0624\u0644\u0624", "\u0644\u0637\u064A\u0641",
        "\u0633\u0645\u0643", "\u0633\u0645\u0627\u0621", "\u0633\u062D\u0627\u0628", "\u0633\u064A\u0641", "\u0633\u0648\u0642", "\u0633\u0641\u064A\u0646\u0629", "\u0633\u064A\u0627\u0631\u0629", "\u0633\u0627\u0639\u0629"
    };

    public static string NormalizeForMatching(string w) {
        if (string.IsNullOrWhiteSpace(w)) return "";
        w = w.Trim();
        w = Regex.Replace(w, "[\u064B-\u065F\u0670\u06D6-\u06EDـ]", "");
        w = w.Replace('\u0623', '\u0627').Replace('\u0625', '\u0627').Replace('\u0622', '\u0627').Replace('\u0671', '\u0627').Replace('\u0629', '\u0647');
        return w;
    }

    public static string CleanDisplaySpelling(string w) {
        if (string.IsNullOrWhiteSpace(w)) return "";
        w = w.Trim();
        w = Regex.Replace(w, "[\u064B-\u065F\u0670\u06D6-\u06EDـ]", "");
        return w;
    }

    public static bool IsHighPrecisionBaseWord(
        string raw,
        HashSet<string> curatedNorm,
        Dictionary<string, int> bigramFreq,
        Dictionary<string, int> trigramFreq,
        int minBgFreq,
        int minTgFreq
    ) {
        string display = CleanDisplaySpelling(raw);
        string norm = NormalizeForMatching(raw);

        if (norm.Length < 3 || norm.Length > 8) return false;
        if (!Regex.IsMatch(norm, @"^[\u0621-\u064A]+$")) return false;

        if (curatedNorm.Contains(norm)) return true;
        if (FunctionWordsBlocklist.Contains(norm) || FunctionWordsBlocklist.Contains(display)) return false;

        // 1. Reject possessive/pronoun suffixes on non-curated words
        if (norm.Length >= 4) {
            foreach (var suf in InflectedSuffixes) {
                if (norm.EndsWith(suf) && norm.Length - suf.Length >= 3) {
                    string stem = norm.Substring(0, norm.Length - suf.Length);
                    if (!curatedNorm.Contains(stem)) return false;
                }
            }
        }

        // 2. Reject proclitic prefixes
        if (norm.Length >= 5 && (norm.StartsWith("\u0628\u0627\u0644") || norm.StartsWith("\u0641\u0627\u0644") || norm.StartsWith("\u0648\u0627\u0644") || norm.StartsWith("\u0643\u0627\u0644"))) return false;
        if (norm.Length >= 4 && norm.StartsWith("\u0644\u0644")) return false;
        if (norm.Length >= 5 && norm.StartsWith("\u0627\u0644")) return false;
        if (norm.Length >= 5 && (norm[0] == '\u0648' || norm[0] == '\u0641') && !NativePrefixExceptions.Contains(norm)) return false;

        // 3. Hamza check
        int hamzaIdx = norm.IndexOf('\u0621');
        if (hamzaIdx >= 0) {
            if (hamzaIdx < norm.Length - 1) return false;
            if (norm.Length < 2) return false;
            string end2 = norm.Substring(norm.Length - 2);
            if (!AllowedHamzaEnds.Contains(end2)) return false;
        }

        // 4. Repeated letters check
        for (int i = 0; i < norm.Length - 1; i++) {
            if (i < norm.Length - 2 && norm[i] == norm[i+1] && norm[i+1] == norm[i+2]) return false;
            if (norm[i] == norm[i+1] && Undoubleable.Contains(norm[i])) return false;
        }

        // 5. Bigram frequency check
        for (int i = 0; i < norm.Length - 1; i++) {
            string bg = norm.Substring(i, 2);
            int freq;
            if (!bigramFreq.TryGetValue(bg, out freq) || freq < minBgFreq) return false;
        }

        // 6. Trigram frequency check for length >= 4
        if (norm.Length >= 4) {
            int validTgCount = 0;
            for (int i = 0; i < norm.Length - 2; i++) {
                string tg = norm.Substring(i, 3);
                int freq;
                if (trigramFreq.TryGetValue(tg, out freq) && freq >= minTgFreq) {
                    validTgCount++;
                }
            }
            if (validTgCount < (norm.Length - 3) / 2 + 1) return false;
        }

        return true;
    }

    public static void CleanFile(string inputPath, string outputPath, HashSet<string> curatedNorm, string[] curatedRaw, Dictionary<string, int> bigramFreq, Dictionary<string, int> trigramFreq, int maxWords) {
        string[] rawLines = File.ReadAllLines(inputPath, Encoding.UTF8);
        var cleanSet = new HashSet<string>();
        var rejectedList = new List<string>();

        foreach (string raw in rawLines) {
            string disp = CleanDisplaySpelling(raw);
            if (IsHighPrecisionBaseWord(raw, curatedNorm, bigramFreq, trigramFreq, 3, 1)) {
                cleanSet.Add(disp);
            } else {
                rejectedList.Add(raw);
            }
        }

        foreach (string c in curatedRaw) {
            string disp = CleanDisplaySpelling(c);
            if (disp.Length >= 3 && disp.Length <= 8) {
                cleanSet.Add(disp);
            }
        }

        var sortedKept = new List<string>(cleanSet);
        sortedKept.Sort();

        List<string> finalKept;
        if (maxWords > 0 && sortedKept.Count > maxWords) {
            var curatedInList = new HashSet<string>();
            var nonCurated = new List<string>();
            foreach (string w in sortedKept) {
                string norm = NormalizeForMatching(w);
                if (curatedNorm.Contains(norm)) curatedInList.Add(w);
                else nonCurated.Add(w);
            }
            int remaining = Math.Max(0, maxWords - curatedInList.Count);
            if (remaining < nonCurated.Count) {
                double step = (double)nonCurated.Count / remaining;
                var sampled = new HashSet<string>(curatedInList);
                for (int i = 0; i < remaining; i++) {
                    sampled.Add(nonCurated[(int)(i * step)]);
                }
                finalKept = new List<string>(sampled);
                finalKept.Sort();
            } else {
                finalKept = sortedKept;
            }
        } else {
            finalKept = sortedKept;
        }

        var utf8NoBom = new UTF8Encoding(false);
        using (var writer = new StreamWriter(outputPath, false, utf8NoBom)) {
            writer.NewLine = "\n";
            foreach (string word in finalKept) {
                writer.WriteLine(word);
            }
        }

        Console.WriteLine("=== Cleaned Summary ===");
        Console.WriteLine("Before count: " + rawLines.Length.ToString("N0"));
        Console.WriteLine("After count:  " + finalKept.Count.ToString("N0"));
        Console.WriteLine("Rejected:     " + rejectedList.Count.ToString("N0") + "\n");

        var rand = new Random(1234);
        Console.WriteLine("=== 30 Random Kept Samples ===");
        var keptSamples = new List<string>();
        for (int i = 0; i < Math.Min(30, finalKept.Count); i++) {
            int idx = rand.Next(finalKept.Count);
            keptSamples.Add(finalKept[idx]);
        }
        Console.WriteLine(string.Join(", ", keptSamples));

        Console.WriteLine("\n=== 30 Random Rejected Samples ===");
        var rejSamples = new List<string>();
        for (int i = 0; i < Math.Min(30, rejectedList.Count); i++) {
            int idx = rand.Next(rejectedList.Count);
            rejSamples.Add(rejectedList[idx]);
        }
        Console.WriteLine(string.Join(", ", rejSamples));
    }
}
"@

Add-Type -TypeDefinition $code -Language CSharp

$dictContent = [System.IO.File]::ReadAllText('app/src/main/java/com/adamos/kalima/data/Dictionary.kt', [System.Text.Encoding]::UTF8)
$jsonContent = [System.IO.File]::ReadAllText('dictionary/remote-dictionary-v1.json', [System.Text.Encoding]::UTF8)

$dictMatches = [regex]::Matches($dictContent, '"([\u0600-\u06FF]+)"') | ForEach-Object { $_.Groups[1].Value }
$jsonObj = ConvertFrom-Json $jsonContent
$jsonWords = $jsonObj.answers + $jsonObj.guesses

$allCuratedRaw = $dictMatches + $jsonWords + @("`u{062C}`u{062D}`u{0634}", "`u{0633}`u{0644}`u{0628}", "`u{0631}`u{0642}`u{0649}")

$curatedNormalized = [System.Collections.Generic.HashSet[string]]::new()
$bigramFreq = [System.Collections.Generic.Dictionary[string, int]]::new()
$trigramFreq = [System.Collections.Generic.Dictionary[string, int]]::new()

foreach ($w in $allCuratedRaw) {
    $norm = [HighPrecisionCleanerRunner]::NormalizeForMatching($w)
    if ($norm.Length -ge 3 -and $norm.Length -le 8) {
        $null = $curatedNormalized.Add($norm)
        for ($i = 0; $i -lt $norm.Length - 1; $i++) {
            $bg = $norm.Substring($i, 2)
            if (-not $bigramFreq.ContainsKey($bg)) { $bigramFreq[$bg] = 0 }
            $bigramFreq[$bg]++
        }
        for ($i = 0; $i -lt $norm.Length - 2; $i++) {
            $tg = $norm.Substring($i, 3)
            if (-not $trigramFreq.ContainsKey($tg)) { $trigramFreq[$tg] = 0 }
            $trigramFreq[$tg]++
        }
    }
}

$guessPath = 'app/src/main/assets/arabic-guess-words.txt'
[HighPrecisionCleanerRunner]::CleanFile($guessPath, $guessPath, $curatedNormalized, $allCuratedRaw, $bigramFreq, $trigramFreq, 45000)
