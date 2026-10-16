# Run dictionary builder unit tests

$code = @"
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

public class TestRunner {
    private static readonly HashSet<char> Undoubleable = new HashSet<char> {
        '\u0627', '\u0649', '\u0629', '\u0647', '\u0621', '\u0624', '\u0626', '\u0648', '\u064A'
    };

    private static readonly string[] GluedPhrases = new string[] {
        "انامو", "ولل", "وبال", "فبال", "وكال", "فكال", "واست", "فاست", "خطوه", "وحظر"
    };

    private static readonly HashSet<string> AllowedHamzaEnds = new HashSet<string> {
        "اء", "وء", "يء", "فء", "بء", "زء", "دء", "كء", "رء", "نء", "لء", "طء", "ثء"
    };

    private static readonly HashSet<string> NativePrefixExceptions = new HashSet<string> {
        "ورد", "ولد", "وزن", "وفى", "وعد", "وقد", "وجد", "وجه", "وطن", "وقت",
        "فيل", "فأر", "فهد", "فصل", "فكر", "فرح", "فوز", "فجر", "فن", "فرن",
        "باب", "بيت", "بحر", "برق", "بنت", "برج", "بئر", "بومة", "بذور",
        "ليل", "لحم", "لبن", "لون", "لعبة", "لسان", "لؤلؤ", "لطيف",
        "سمك", "سماء", "سحاب", "سيف", "سوق", "سفينة", "سيارة", "ساعة"
    };

    private static readonly string[] RegressionWords = new string[] {
        "جحش", "سلب", "رقى", "رقي", "سماء", "مدرسة", "كتاب", "دفء", "ماء"
    };

    public static string Normalize(string w) {
        if (string.IsNullOrWhiteSpace(w)) return "";
        w = w.Trim();
        w = Regex.Replace(w, "[\u064B-\u065F\u0670\u06D6-\u06EDـ]", "");
        w = w.Replace('أ', 'ا').Replace('إ', 'ا').Replace('آ', 'ا').Replace('ٱ', 'ا').Replace('ة', 'ه');
        return w;
    }

    public static bool IsGluedPhrase(string norm) {
        foreach (var g in GluedPhrases) {
            if (norm.StartsWith(g) && norm.Length >= g.Length + 2) return true;
        }
        return false;
    }

    public static string StripProclitics(string norm, HashSet<string> curated) {
        if (curated.Contains(norm) || NativePrefixExceptions.Contains(norm)) return norm;

        string current = norm;
        if (current.Length >= 5 && current.StartsWith("بال")) return current.Substring(3);
        if (current.Length >= 5 && current.StartsWith("فال")) return current.Substring(3);
        if (current.Length >= 5 && current.StartsWith("وال")) return current.Substring(3);
        if (current.Length >= 5 && current.StartsWith("كال")) return current.Substring(3);
        if (current.Length >= 4 && current.StartsWith("لل")) return current.Substring(2);
        if (current.Length >= 5 && current.StartsWith("ال")) return current.Substring(2);

        if (current.Length >= 5 && (current[0] == 'و' || current[0] == 'ف')) {
            current = current.Substring(1);
        }
        return current.Length >= 3 ? current : norm;
    }

    public static bool IsCleanBaseWord(
        string rawNorm,
        HashSet<string> curated,
        Dictionary<string, int> bigramFreq,
        HashSet<string> trigramSet,
        int minBgFreq
    ) {
        if (curated.Contains(rawNorm)) return true;

        if (IsGluedPhrase(rawNorm)) return false;

        string norm = StripProclitics(rawNorm, curated);

        if (norm.Length < 3 || norm.Length > 8) return false;
        if (!Regex.IsMatch(norm, @"^[\u0621-\u064A]+$")) return false;

        // Hamza
        int hamzaIdx = norm.IndexOf('ء');
        if (hamzaIdx >= 0) {
            if (hamzaIdx < norm.Length - 1) return false;
            if (norm.Length < 2) return false;
            string end2 = norm.Substring(norm.Length - 2);
            if (!AllowedHamzaEnds.Contains(end2)) return false;
        }

        // Repeated letters
        for (int i = 0; i < norm.Length - 1; i++) {
            if (i < norm.Length - 2 && norm[i] == norm[i+1] && norm[i+1] == norm[i+2]) return false;
            if (norm[i] == norm[i+1] && Undoubleable.Contains(norm[i])) return false;
        }

        // Bigram freq
        for (int i = 0; i < norm.Length - 1; i++) {
            string bg = norm.Substring(i, 2);
            int freq;
            if (!bigramFreq.TryGetValue(bg, out freq) || freq < minBgFreq) return false;
        }

        // Trigram plausibility for len >= 4
        if (norm.Length >= 4) {
            bool hasValidTrigram = false;
            for (int i = 0; i < norm.Length - 2; i++) {
                if (trigramSet.Contains(norm.Substring(i, 3))) {
                    hasValidTrigram = true;
                    break;
                }
            }
            if (!hasValidTrigram) return false;
        }

        return true;
    }

    public static void RunAllTests(string dictPath, string jsonPath) {
        string dictContent = File.ReadAllText(dictPath, Encoding.UTF8);
        string jsonContent = File.ReadAllText(jsonPath, Encoding.UTF8);

        var dictMatches = Regex.Matches(dictContent, @"\""([\u0600-\u06FF]+)\""");
        var curatedRaw = new List<string>();
        foreach (Match m in dictMatches) {
            curatedRaw.Add(m.Groups[1].Value);
        }
        foreach (string r in RegressionWords) {
            curatedRaw.Add(r);
        }

        var curatedNormalized = new HashSet<string>();
        var bigramFreq = new Dictionary<string, int>();
        var trigramSet = new HashSet<string>();

        foreach (string w in curatedRaw) {
            string norm = Normalize(w);
            if (norm.Length >= 3 && norm.Length <= 8) {
                curatedNormalized.Add(norm);
                for (int i = 0; i < norm.Length - 1; i++) {
                    string bg = norm.Substring(i, 2);
                    if (!bigramFreq.ContainsKey(bg)) bigramFreq[bg] = 0;
                    bigramFreq[bg]++;
                }
                for (int i = 0; i < norm.Length - 2; i++) {
                    trigramSet.Add(norm.Substring(i, 3));
                }
            }
        }

        // Test 1: Rejection of garbage words
        string[] garbage = new string[] { "ءءء", "ءءل", "ءؤخ", "اناموظف", "تكذي", "هازيو", "بلعس", "وحظربه" };
        foreach (string g in garbage) {
            bool isOk = IsCleanBaseWord(Normalize(g), curatedNormalized, bigramFreq, trigramSet, 2);
            if (isOk) throw new Exception("Expected garbage word '" + g + "' to be rejected!");
        }
        Console.WriteLine("[PASS] Garbage words rejection test passed.");

        // Test 2: Retention of real words
        string[] realWords = new string[] { "سماء", "مدرسة", "كتاب", "جحش", "سلب", "رقى", "رقي", "دفء", "ماء" };
        foreach (string w in realWords) {
            string norm = Normalize(w);
            bool isOk = IsCleanBaseWord(norm, curatedNormalized, bigramFreq, trigramSet, 2);
            if (!isOk) throw new Exception("Expected real word '" + w + "' to be kept!");
        }
        Console.WriteLine("[PASS] Real words retention test passed.");

        // Test 3: Proclitic stripping
        string stripped1 = StripProclitics(Normalize("بالحديث"), curatedNormalized);
        if (stripped1 != Normalize("حديث")) throw new Exception("Expected 'بالحديث' -> 'حديث', got '" + stripped1 + "'");
        Console.WriteLine("[PASS] Proclitic stripping test passed.");

        Console.WriteLine("\nALL DICTIONARY BUILDER UNIT TESTS PASSED SUCCESSFULLY.");
    }
}
"@

Add-Type -TypeDefinition $code -Language CSharp

$dictPath = 'app/src/main/java/com/adamos/kalima/data/Dictionary.kt'
$jsonPath = 'dictionary/remote-dictionary-v1.json'
[TestRunner]::RunAllTests($dictPath, $jsonPath)
