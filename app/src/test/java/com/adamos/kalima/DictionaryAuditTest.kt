package com.adamos.kalima

import com.adamos.kalima.data.Dictionary
import org.junit.Test
import java.io.File

class DictionaryAuditTest {

    @Test
    fun auditCurrentDatasets() {
        val sb = StringBuilder()
        val guessFile = findFile("src/main/assets/arabic-guess-words.txt", "../app/src/main/assets/arabic-guess-words.txt", "app/src/main/assets/arabic-guess-words.txt")
        val rawFile = findFile("../tools/raw_300k_arabic_words.txt", "tools/raw_300k_arabic_words.txt")
        val jsonAsset = findFile("src/main/assets/remote-dictionary-v1.json", "../app/src/main/assets/remote-dictionary-v1.json", "app/src/main/assets/remote-dictionary-v1.json")
        val jsonDict = findFile("../dictionary/remote-dictionary-v1.json", "dictionary/remote-dictionary-v1.json")

        auditFile("assets/arabic-guess-words.txt", guessFile, sb)
        auditFile("tools/raw_300k_arabic_words.txt", rawFile, sb)
        auditJsonFile("assets/remote-dictionary-v1.json", jsonAsset, sb)
        auditJsonFile("dictionary/remote-dictionary-v1.json", jsonDict, sb)

        // Test with enhanced hamza rule
        auditRawWithEnhancedRules(rawFile, sb)

        val outputFile = File("audit_output.txt")
        outputFile.writeText(sb.toString(), Charsets.UTF_8)
    }

    private fun findFile(vararg paths: String): File? {
        for (p in paths) {
            val f = File(p)
            if (f.exists()) return f
        }
        return null
    }

    private fun auditJsonFile(label: String, file: File?, sb: StringBuilder) {
        sb.appendLine("==========================================")
        sb.appendLine("AUDITING JSON: $label (Path: ${file?.absolutePath})")
        if (file == null || !file.exists()) {
            sb.appendLine("File does NOT exist!")
            return
        }
        val text = file.readText(Charsets.UTF_8)
        val versionMatch = Regex(""""version"\s*:\s*"([^"]+)"""").find(text)
        val schemaMatch = Regex(""""schemaVersion"\s*:\s*(\d+)""").find(text)
        val answersCount = Regex(""""answers"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(text)?.let {
            Regex(""""([^"]+)"""").findAll(it.groupValues[1]).count()
        } ?: 0
        val guessesCount = Regex(""""guesses"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(text)?.let {
            Regex(""""([^"]+)"""").findAll(it.groupValues[1]).count()
        } ?: 0

        sb.appendLine("Schema Version: ${schemaMatch?.groupValues?.get(1) ?: "N/A"} | Version: ${versionMatch?.groupValues?.get(1) ?: "N/A"}")
        sb.appendLine("Answers Count: $answersCount")
        sb.appendLine("Guesses Count: $guessesCount")
        sb.appendLine("==========================================")
    }

    private fun isPlausibleEnhanced(word: String): Boolean {
        val normalized = Dictionary.normalize(word)
        if (normalized.length !in 3..8) return false
        if (normalized.toSet().size <= 1) return false
        if (normalized.windowed(3).any { it[0] == it[1] && it[1] == it[2] }) return false
        val undoubleable = setOf('ا', 'ى', 'ة', 'ء', 'ؤ', 'ئ')
        for (i in 0 until normalized.length - 1) {
            if (normalized[i] == normalized[i + 1] && normalized[i] in undoubleable) return false
        }
        val hamzaIdx = normalized.indexOf('ء')
        if (hamzaIdx >= 0) {
            // Cannot start with standalone hamza
            if (hamzaIdx == 0) return false
            // Cannot have multiple hamzas
            if (normalized.indexOf('ء', hamzaIdx + 1) >= 0) return false
            
            // Standalone hamza is valid at end (سماء, دفء, ماء, جزء)
            // Or in the middle/end if preceded by alef or waw (تثاءب, مملوءة, قراءة, تساءل, دناءة)
            val isEnd = hamzaIdx == normalized.length - 1
            val prevChar = normalized[hamzaIdx - 1]
            val isPrecededByAlefOrWaw = prevChar == 'ا' || prevChar == 'و' || prevChar == 'ي' || prevChar == 'ف' || prevChar == 'ب' || prevChar == 'د' || prevChar == 'ز' || prevChar == 'ر' || prevChar == 'ك' || prevChar == 'ن' || prevChar == 'ل' || prevChar == 'ط' || prevChar == 'ث'
            if (!isEnd && !isPrecededByAlefOrWaw) return false
        }
        return true
    }

    private fun auditRawWithEnhancedRules(file: File?, sb: StringBuilder) {
        sb.appendLine("==========================================")
        sb.appendLine("ENHANCED RULES AUDIT on ${file?.name}")
        if (file == null || !file.exists()) return

        val rawLines = file.readLines(Charsets.UTF_8)
        val nonEmptyLines = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
        val seenNorm = mutableSetOf<String>()
        val acceptedDisplay = mutableListOf<String>()
        val rejectedEntries = mutableListOf<String>()

        for (entry in nonEmptyLines) {
            val norm = Dictionary.normalize(entry)
            if (norm.length !in 3..8) {
                rejectedEntries.add("$entry (length ${norm.length})")
                continue
            }
            if (!Dictionary.isValidGuess(entry) || !isPlausibleEnhanced(entry)) {
                rejectedEntries.add("$entry (failed plausibility)")
                continue
            }
            if (seenNorm.contains(norm)) {
                continue
            }
            seenNorm.add(norm)
            acceptedDisplay.add(entry)
        }

        val lengthCounts = mutableMapOf<Int, Int>()
        for (w in acceptedDisplay) {
            val len = Dictionary.normalize(w).length
            lengthCounts[len] = (lengthCounts[len] ?: 0) + 1
        }

        sb.appendLine("Accepted Clean Entries (Enhanced): ${acceptedDisplay.size}")
        sb.appendLine("Rejected Entries Count:             ${rejectedEntries.size}")
        sb.appendLine("--- Length Breakdown (Normalized) ---")
        for (len in 3..8) {
            sb.appendLine("  - Length $len: ${lengthCounts[len] ?: 0} words")
        }
        sb.appendLine("First 15 Rejected Samples:")
        rejectedEntries.take(15).forEach { sb.appendLine("   $it") }
        sb.appendLine("==========================================")
    }

    private fun auditFile(label: String, file: File?, sb: StringBuilder) {
        sb.appendLine("==========================================")
        sb.appendLine("AUDITING: $label (Path: ${file?.absolutePath})")
        if (file == null || !file.exists()) {
            sb.appendLine("File does NOT exist!")
            return
        }

        val rawLines = file.readLines(Charsets.UTF_8)
        val originalRawCount = rawLines.size
        val nonEmptyLines = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
        val nonEmtpyCount = nonEmptyLines.size
        val uniqueRawSet = nonEmptyLines.toSet()
        val uniqueRawCount = uniqueRawSet.size

        val seenNorm = mutableSetOf<String>()
        val acceptedDisplay = mutableListOf<String>()
        val rejectedEntries = mutableListOf<String>()
        var duplicateNormCount = 0

        for (entry in nonEmptyLines) {
            val norm = Dictionary.normalize(entry)
            if (norm.length !in 3..8) {
                rejectedEntries.add("$entry (length ${norm.length})")
                continue
            }
            if (!Dictionary.isValidGuess(entry) || !Dictionary.isPlausibleExpandedEntry(entry)) {
                rejectedEntries.add("$entry (failed plausibility)")
                continue
            }
            if (seenNorm.contains(norm)) {
                duplicateNormCount++
                continue
            }
            seenNorm.add(norm)
            acceptedDisplay.add(entry)
        }

        val lengthCounts = mutableMapOf<Int, Int>()
        for (w in acceptedDisplay) {
            val len = Dictionary.normalize(w).length
            lengthCounts[len] = (lengthCounts[len] ?: 0) + 1
        }

        sb.appendLine("1. Original Raw Line Count:    $originalRawCount")
        sb.appendLine("2. Non-Empty Line Count:      $nonEmtpyCount")
        sb.appendLine("3. Unique Raw Entry Count:    $uniqueRawCount")
        sb.appendLine("4. Unique Normalized Entries: ${seenNorm.size}")
        sb.appendLine("5. Accepted Clean Entries:    ${acceptedDisplay.size}")
        sb.appendLine("6. Duplicate Normalized:      $duplicateNormCount")
        sb.appendLine("7. Rejected Entries Count:    ${rejectedEntries.size}")
        sb.appendLine("--- Length Breakdown (Normalized) ---")
        for (len in 3..8) {
            sb.appendLine("  - Length $len: ${lengthCounts[len] ?: 0} words")
        }
        if (rejectedEntries.isNotEmpty()) {
            sb.appendLine("First 15 Rejected Samples:")
            rejectedEntries.take(15).forEach { sb.appendLine("   $it") }
        }
        sb.appendLine("==========================================")
    }
}
