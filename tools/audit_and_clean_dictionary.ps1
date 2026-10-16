# Audit and clean raw 300k dataset into a high-precision, offline-usable Arabic guess dictionary (250,000+ words)

$code = @"
using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

public class DictionaryAuditor {
    private static readonly HashSet<char> Undoubleable = new HashSet<char> {
        '\u0627', '\u0649', '\u0629', '\u0621', '\u0624', '\u0626'
    };

    public static string Normalize(string w) {
        if (string.IsNullOrWhiteSpace(w)) return "";
        w = w.Trim();
        w = Regex.Replace(w, "[\u064B-\u065F\u0670\u06D6-\u06EDـ]", "");
        w = w.Replace('\u0623', '\u0627').Replace('\u0625', '\u0627').Replace('\u0622', '\u0627').Replace('\u0671', '\u0627').Replace('\u0629', '\u0647');
        return w;
    }

    public static string CleanDisplay(string w) {
        if (string.IsNullOrWhiteSpace(w)) return "";
        w = w.Trim();
        return Regex.Replace(w, "[\u064B-\u065F\u0670\u06D6-\u06EDـ]", "");
    }

    public static bool IsValidOrthography(string raw) {
        string display = CleanDisplay(raw);
        string norm = Normalize(raw);

        if (norm.Length < 3 || norm.Length > 8) return false;
        if (!Regex.IsMatch(norm, @"^[\u0621-\u064A]+$")) return false;

        for (int i = 0; i < norm.Length - 2; i++) {
            if (norm[i] == norm[i + 1] && norm[i + 1] == norm[i + 2]) return false;
        }

        for (int i = 0; i < norm.Length - 1; i++) {
            if (norm[i] == norm[i + 1] && Undoubleable.Contains(norm[i])) return false;
        }

        int hamzaIdx = norm.IndexOf('\u0621');
        if (hamzaIdx >= 0) {
            string rest = norm.Substring(hamzaIdx + 1);
            bool isAllowed = (hamzaIdx == norm.Length - 1) || (
                hamzaIdx > 0 && norm[hamzaIdx - 1] == '\u0627' && (rest == "\u0647" || rest == "\u0627\u062A" || rest == "\u0627\u0646")
            );
            if (!isAllowed) return false;
        }

        return true;
    }

    public static void RunAudit(string rawPath, string dictPath, string outputAssetPath) {
        string[] rawLines = File.ReadAllLines(rawPath, Encoding.UTF8);
        int originalRawCount = rawLines.Length;

        var nonCount = 0;
        var uniqueRawSet = new HashSet<string>();
        foreach (var l in rawLines) {
            string tr = l.Trim();
            if (!string.IsNullOrEmpty(tr)) {
                nonCount++;
                uniqueRawSet.Add(tr);
            }
        }

        var seenNorm = new HashSet<string>();
        var acceptedDisplay = new List<string>();
        var rejectedCount = 0;

        foreach (var entry in uniqueRawSet) {
            string disp = CleanDisplay(entry);
            string norm = Normalize(entry);
            if (seenNorm.Contains(norm)) continue;

            if (IsValidOrthography(entry)) {
                seenNorm.Add(norm);
                acceptedDisplay.Add(disp);
            } else {
                rejectedCount++;
            }
        }

        if (File.Exists(dictPath)) {
            string dictContent = File.ReadAllText(dictPath, Encoding.UTF8);
            var matches = Regex.Matches(dictContent, "\"([\\u0600-\\u06FF]+)\"");
            foreach (Match m in matches) {
                string w = m.Groups[1].Value.Trim();
                string disp = CleanDisplay(w);
                string norm = Normalize(w);
                if (norm.Length >= 3 && norm.Length <= 8 && !seenNorm.Contains(norm)) {
                    seenNorm.Add(norm);
                    acceptedDisplay.Add(disp);
                }
            }
        }

        acceptedDisplay.Sort((a, b) => string.Compare(Normalize(a), Normalize(b), StringComparison.Ordinal));

        var byLen = new Dictionary<int, int>();
        for (int i = 3; i <= 8; i++) byLen[i] = 0;

        foreach (var w in acceptedDisplay) {
            int l = Normalize(w).Length;
            if (byLen.ContainsKey(l)) byLen[l]++;
        }

        var utf8NoBom = new UTF8Encoding(false);
        File.WriteAllLines(outputAssetPath, acceptedDisplay, utf8NoBom);

        Console.WriteLine("=== REPRODUCIBLE AUDIT REPORT ===");
        Console.WriteLine("1. Original Raw Line Count:    " + originalRawCount.ToString("N0"));
        Console.WriteLine("2. Non-Empty Entry Count:      " + nonCount.ToString("N0"));
        Console.WriteLine("3. Unique Raw Entries:         " + uniqueRawSet.Count.ToString("N0"));
        Console.WriteLine("4. Unique Normalized Entries:  " + seenNorm.Count.ToString("N0"));
        Console.WriteLine("5. Accepted Clean Entries:     " + acceptedDisplay.Count.ToString("N0"));
        Console.WriteLine("6. Rejected Entries:           " + rejectedCount.ToString("N0"));
        Console.WriteLine("\n=== LENGTH BREAKDOWN (Normalized) ===");
        foreach (var kvp in byLen) {
            Console.WriteLine("  - Length " + kvp.Key + ": " + kvp.Value.ToString("N0") + " words");
        }
        Console.WriteLine("\nFinal packaged asset " + outputAssetPath + " written with " + acceptedDisplay.Count.ToString("N0") + " entries.");
    }
}
"@

Add-Type -TypeDefinition $code -Language CSharp

$rawPath = 'tools/raw_300k_arabic_words.txt'
$dictPath = 'app/src/main/java/com/adamos/kalima/data/Dictionary.kt'
$outputAssetPath = 'app/src/main/assets/arabic-guess-words.txt'

[DictionaryAuditor]::RunAudit($rawPath, $dictPath, $outputAssetPath)
