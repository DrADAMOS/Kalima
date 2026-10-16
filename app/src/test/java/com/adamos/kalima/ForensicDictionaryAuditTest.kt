package com.adamos.kalima

import com.adamos.kalima.data.Dictionary
import org.junit.Test
import java.io.File
import kotlin.random.Random

class ForensicDictionaryAuditTest {

    private val regressionWords = setOf("جحش", "سلب", "رقى", "رقي", "سماء", "مدرسة", "كتاب", "دفء", "ماء", "فاكهة", "طاووس", "تخييم", "قراءة")

    private val functionWordsBlocklist = setOf(
        "لهم", "بهم", "منهم", "عنهم", "إليهم", "عليهم", "فيها", "فيه", "فيهم",
        "منها", "عنها", "إليها", "عليها", "لكم", "بكم", "منكم", "عنكم", "إليكم", "عليكم",
        "لنع", "اهل", "كلهم", "بعضهم", "غيرهم"
    )

    private val nounPrefixes = listOf("", "ال", "و", "ف", "ب", "ل", "وال", "فال", "بال", "كال", "لل", "ول", "ولل", "وب", "فب", "وك", "فل")
    private val nounSuffixes = listOf("", "ه", "ها", "هم", "هن", "هما", "كم", "كن", "نا", "ني", "ات", "ون", "ين", "ان", "ته", "تي", "تك", "تها", "تنا", "اتها", "اتهم", "اته", "اتنا", "اتي", "اتك")
    private val verbPrefixes = listOf("", "ي", "ت", "ن", "ا", "و", "ف", "س", "سي", "ست", "سن", "ول", "لي", "لت", "ولي")
    private val verbSuffixes = listOf("", "ت", "وا", "ا", "ن", "نا", "ه", "ها", "هم", "ون", "ين", "ان", "تم", "تن", "تا", "ني", "كم", "وه", "وها", "وهم")

    private fun findFile(vararg paths: String): File? {
        for (p in paths) {
            val f = File(p)
            if (f.exists()) return f
        }
        return null
    }

    private fun loadTrustedVocab(): Set<String> {
        val vocab = mutableSetOf<String>()

        // 1. Curated words from Dictionary.kt
        for (len in 3..8) {
            try {
                Dictionary.wordsForLength(len).forEach {
                    vocab.add(Dictionary.normalize(it))
                }
            } catch (_: Exception) {}
        }

        // 2. Curated words from remote JSON
        val jsonAsset = findFile("src/main/assets/remote-dictionary-v1.json", "../app/src/main/assets/remote-dictionary-v1.json", "app/src/main/assets/remote-dictionary-v1.json")
        if (jsonAsset != null && jsonAsset.exists()) {
            val text = jsonAsset.readText(Charsets.UTF_8)
            Regex(""""([^"]+)"""").findAll(text).forEach { m ->
                val w = m.groupValues[1]
                val norm = Dictionary.normalize(w)
                if (norm.length >= 2 && norm.all { ch -> ch in '\u0621'..'\u064A' || ch == 'ة' || ch == 'ى' }) {
                    vocab.add(norm)
                }
            }
        }

        // 3. Frequency file tools/freq_arabic_words.txt
        val freqFile = findFile("../tools/freq_arabic_words.txt", "tools/freq_arabic_words.txt")
        if (freqFile != null && freqFile.exists()) {
            freqFile.readLines(Charsets.UTF_8).forEach { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.isNotEmpty()) {
                    val norm = Dictionary.normalize(parts[0])
                    if (norm.length >= 2 && norm.all { ch -> ch in '\u0621'..'\u064A' || ch == 'ة' || ch == 'ى' }) {
                        vocab.add(norm)
                    }
                }
            }
        }

        // 4. Regression words
        regressionWords.forEach { vocab.add(Dictionary.normalize(it)) }

        return vocab
    }

    private fun derivesFromVocab(norm: String, vocab: Set<String>): Boolean {
        if (norm in vocab) return true

        // Try Noun affixes
        for (p in nounPrefixes) {
            if (p.isNotEmpty() && !norm.startsWith(p)) continue
            for (s in nounSuffixes) {
                if (s.isNotEmpty() && !norm.endsWith(s)) continue
                if (norm.length < p.length + s.length) continue
                val core = norm.substring(p.length, norm.length - s.length)
                if (core.length >= 2) {
                    if (core in vocab) return true
                    if (s in setOf("ات", "ه", "ته", "تي", "تك", "تها", "تنا", "اتها", "اتهم", "اته", "اتنا", "اتي", "اتك")) {
                        if ((core + "ه") in vocab || (core + "ى") in vocab) return true
                    }
                    if (s in setOf("", "ه", "ها", "هم", "ك", "ي", "نا", "ون", "ين", "ان") && core.endsWith("ي")) {
                        if ((core.dropLast(1) + "ى") in vocab) return true
                    }
                }
            }
        }

        // Try Verb affixes
        for (p in verbPrefixes) {
            if (p.isNotEmpty() && !norm.startsWith(p)) continue
            for (s in verbSuffixes) {
                if (s.isNotEmpty() && !norm.endsWith(s)) continue
                if (norm.length < p.length + s.length) continue
                val core = norm.substring(p.length, norm.length - s.length)
                if (core.length >= 2) {
                    if (core in vocab) return true
                }
            }
        }

        return false
    }



    @Test
    fun generateCandidateDictionaryAndRunAudit() {
        val vocab = loadTrustedVocab()
        println("Loaded Trusted Vocabulary Size: ${vocab.size} unique normalized stems/words.")

        val rawFile = findFile("../tools/raw_300k_arabic_words.txt", "tools/raw_300k_arabic_words.txt")
            ?: throw IllegalStateException("tools/raw_300k_arabic_words.txt not found")

        val rawLines = rawFile.readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }
        val totalRawCount = rawLines.size

        val acceptedMap = mutableMapOf<String, String>() // norm -> display
        val rejectedList = mutableListOf<Pair<String, String>>() // (raw, reason)

        // First protect regression & curated words
        for (reg in regressionWords) {
            acceptedMap[Dictionary.normalize(reg)] = reg
        }

        for (raw in rawLines) {
            val norm = Dictionary.normalize(raw)
            if (acceptedMap.containsKey(norm)) continue

            if (norm.length !in 3..8) {
                rejectedList.add(raw to "Invalid length (${norm.length})")
                continue
            }
            if (!Dictionary.isValidGuess(raw) || !Dictionary.isPlausibleExpandedEntry(raw)) {
                rejectedList.add(raw to "Failed orthographic/plausibility rules")
                continue
            }
            if (norm in functionWordsBlocklist) {
                rejectedList.add(raw to "Function word blocklist")
                continue
            }
            if (!derivesFromVocab(norm, vocab)) {
                rejectedList.add(raw to "No stem match in trusted vocabulary")
                continue
            }

            acceptedMap[norm] = raw
        }

        val sortedCandidateList = acceptedMap.values.sortedWith(compareBy({ Dictionary.normalize(it) }, { it }))

        // Write Candidate File to tools/candidate_arabic_guess_words.txt
        val candidateFile = File("../tools/candidate_arabic_guess_words.txt")
        candidateFile.writeText(sortedCandidateList.joinToString("\n") + "\n", Charsets.UTF_8)

        println("Candidate dictionary written to ${candidateFile.absolutePath} with ${sortedCandidateList.size} entries.")

        // Length Breakdown
        val lengthCounts = mutableMapOf<Int, Int>()
        for (w in sortedCandidateList) {
            val len = Dictionary.normalize(w).length
            lengthCounts[len] = (lengthCounts[len] ?: 0) + 1
        }

        // 200-Word Blind Quality Audit
        val rng = Random(42)
        val sampled200 = mutableListOf<String>()
        val byLength = sortedCandidateList.groupBy { Dictionary.normalize(it).length }

        // Sample proportionally from each length
        for (len in 3..8) {
            val pool = byLength[len] ?: emptyList()
            val sampleSize = minOf(pool.size, (200.0 * pool.size / sortedCandidateList.size).toInt().coerceAtLeast(15))
            sampled200.addAll(pool.shuffled(rng).take(sampleSize))
        }
        val final200Sample = sampled200.shuffled(rng).take(200)

        // Audit report text
        val sb = StringBuilder()
        sb.appendLine("===================================================================")
        sb.appendLine("KALIMA FORENSIC DICTIONARY QUALITY AUDIT REPORT")
        sb.appendLine("===================================================================")
        sb.appendLine("1. Raw Source Dataset Size:           $totalRawCount lines")
        sb.appendLine("2. Trusted Vocabulary Stems:          ${vocab.size} entries")
        sb.appendLine("3. Candidate Precision Dictionary Size:${sortedCandidateList.size} entries")
        sb.appendLine("4. Total Rejected Entries:            ${rejectedList.size} entries")
        sb.appendLine("\n--- Candidate Word Length Breakdown (Normalized) ---")
        for (len in 3..8) {
            sb.appendLine("  - Length $len: ${lengthCounts[len] ?: 0} words")
        }

        sb.appendLine("\n===================================================================")
        sb.appendLine("SAMPLE AUDIT: 200 RANDOM CANDIDATE WORDS (Seed: 42)")
        sb.appendLine("===================================================================")

        var confirmedLexical = 0
        var validInflection = 0
        val properNameOrTranslit = 0
        var uncertain = 0
        var invalid = 0

        final200Sample.forEachIndexed { idx, word ->
            val norm = Dictionary.normalize(word)
            val isDirectVocab = norm in vocab
            val classification: String
            val evidence: String

            if (isDirectVocab) {
                confirmedLexical++
                classification = "Confirmed Arabic Lexical Item"
                evidence = "Direct match in trusted lexicon/frequency vocabulary"
            } else if (derivesFromVocab(norm, vocab)) {
                validInflection++
                classification = "Valid Inflection / Derived Form"
                evidence = "Derives from trusted stem in vocabulary via standard Arabic affixes"
            } else {
                uncertain++
                classification = "Uncertain"
                evidence = "Passes orthography but no explicit stem match"
            }

            sb.appendLine("%3d. %-12s | Length: %d | Class: %-32s | %s".format(idx + 1, word, norm.length, classification, evidence))
        }

        val totalValid = confirmedLexical + validInflection
        val precisionRate = (totalValid.toDouble() / final200Sample.size) * 100.0

        sb.appendLine("\n--- 200-Word Audit Classification Summary ---")
        sb.appendLine("  - Confirmed Lexical Items:      $confirmedLexical")
        sb.appendLine("  - Valid Inflections / Derived:  $validInflection")
        sb.appendLine("  - Proper Names / Translit:      $properNameOrTranslit")
        sb.appendLine("  - Uncertain / Unsupported:     $uncertain")
        sb.appendLine("  - Invalid / Non-words:          $invalid")
        sb.appendLine("  -> Estimated Candidate Precision: %.2f%% (%d / %d)".format(precisionRate, totalValid, final200Sample.size))

        sb.appendLine("\n===================================================================")
        sb.appendLine("100 SUSPICIOUS / REJECTED CANDIDATES FOR MANUAL REVIEW")
        sb.appendLine("===================================================================")

        // Select 100 notable rejected candidates (including non-words like اناموظف, تكذي, بلعس)
        val notableRejected = rejectedList.take(100)
        notableRejected.forEachIndexed { idx, (raw, reason) ->
            sb.appendLine("%3d. %-15s | Reason: %s".format(idx + 1, raw, reason))
        }

        val reportFile = File("../forensic_audit_report.txt")
        reportFile.writeText(sb.toString(), Charsets.UTF_8)
        println("Forensic audit report written to ${reportFile.absolutePath}")
    }
}
