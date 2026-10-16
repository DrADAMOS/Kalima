package com.adamos.kalima

import com.adamos.kalima.data.Dictionary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryTest {
    @Test
    fun onlineArabicWordsAreAddedToGuessDictionaryWithoutBecomingAnswers() {
        Dictionary.applyOnlineWords(listOf("رقى", "رقي"))

        assertTrue(Dictionary.isGuessable("رقى"))
        assertTrue(Dictionary.isGuessable("رقي"))
        assertFalse(Dictionary.isKnownWord("رقى"))
        assertFalse(Dictionary.isKnownWord("رقي"))
    }

    @Test
    fun expandedDictionaryWordsAreGuessableWithoutBecomingAnswers() {
        // Regression: this valid Arabic word was missing from the old small snapshot.
        Dictionary.applyOnlineWords(listOf("سلب"))
        assertTrue(Dictionary.isGuessable("سلب"))
        assertFalse(Dictionary.isKnownWord("سلب"))
    }

    @Test
    fun normalizationIgnoresArabicDiacriticsAndTatweel() {
        assertTrue(Dictionary.isValidGuess("رُقَى"))
        assertTrue(Dictionary.isValidGuess("رقيّ"))
        assertTrue(Dictionary.normalize("رُقَى" ) == Dictionary.normalize("رقى"))
        assertTrue(Dictionary.normalize("رقيّ") == Dictionary.normalize("رقي"))
        assertTrue(Dictionary.normalize("مــدرسَة") == Dictionary.normalize("مدرسة"))
    }

    @Test
    fun remoteWordValidationChecksNormalizedArabicLettersAndLength() {
        assertTrue(Dictionary.isValidGuess("رُقَى"))
        assertFalse(Dictionary.isValidGuess("abc"))
        assertFalse(Dictionary.isValidGuess("رق"))
        assertFalse(Dictionary.isValidGuess("رقى123"))
    }

    @Test
    fun plausibleExpandedEntriesAcceptRealArabicWords() {
        assertTrue(Dictionary.isPlausibleExpandedEntry("فاكهة"))
        assertTrue(Dictionary.isPlausibleExpandedEntry("طاووس"))
        assertTrue(Dictionary.isPlausibleExpandedEntry("تخييم"))
        assertTrue(Dictionary.isPlausibleExpandedEntry("قراءة"))
        assertFalse(Dictionary.isPlausibleExpandedEntry("ءءء"))
    }

    @Test
    fun normalizationMapsTaMarbutaToHaForMatching() {
        assertTrue(Dictionary.normalize("مدرسة") == Dictionary.normalize("مدرسه"))
        assertTrue(Dictionary.normalize("بيضة") == Dictionary.normalize("بيضه"))
        assertTrue(Dictionary.normalize("عائلة") == Dictionary.normalize("عائله"))
    }
    @Test
    fun blockedWordsAreRejectedInSafeModeOnly() {
        assertFalse(Dictionary.isSafe("خمر"))
        assertFalse(Dictionary.isSafe("حشيش"))
        assertTrue(Dictionary.isSafe("ماء"))
        assertFalse(Dictionary.isGuessable("خمر", safeOnly = true))
    }

    @Test
    fun blocklistMatchesWholeWordsNotSubstrings() {
        // «دم» داخل «قدم» و«ندم» ليس محتوى غير لائق
        assertTrue(Dictionary.isSafe("قدم"))
        assertTrue(Dictionary.isSafe("ندم"))
        assertTrue(Dictionary.isSafe("كتاب"))
    }

    @Test
    fun answerPoolsExistForEveryLengthAndAreSafe() {
        Dictionary.lengths.forEach { len ->
            val pool = Dictionary.wordsForLength(len, onlySafe = true)
            assertTrue("no answers for length $len", pool.isNotEmpty())
            assertTrue(pool.all { Dictionary.isSafe(it) })
        }
    }

    @Test
    fun answerCountsMeetMinimumTargetsPerLength() {
        val targets = mapOf(3 to 400, 4 to 450, 5 to 500, 6 to 200, 7 to 60, 8 to 20)
        targets.forEach { (length, minTarget) ->
            val count = Dictionary.wordsForLength(length).size
            assertTrue("Length $length count ($count) is below minimum target $minTarget", count >= minTarget)
        }
    }

    @Test
    fun answersContainNoFillerPossessiveForms() {
        Dictionary.lengths.forEach { len ->
            Dictionary.wordsForLength(len).forEach { w ->
                val n = Dictionary.normalize(w)
                assertFalse("filler answer: $w", n.length >= 6 && (n.endsWith("نا") || n.endsWith("كم")))
            }
        }
    }

    @Test
    fun everyAnswerIsGuessable() {
        Dictionary.lengths.forEach { len ->
            Dictionary.wordsForLength(len).forEach { answer ->
                assertTrue("Answer $answer should be guessable", Dictionary.isGuessable(answer))
            }
        }
    }

    @Test
    fun offlineExpandedDictionaryLoadsHighVolumeWords() {
        val file = listOf("src/main/assets/arabic-guess-words.txt", "app/src/main/assets/arabic-guess-words.txt")
            .map { java.io.File(it) }
            .firstOrNull { it.exists() }
        assertTrue("Asset file arabic-guess-words.txt must exist", file != null && file.exists())
        val lines = file!!.readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue("Asset must contain at least 20,000 verified entries, found: ${lines.size}", lines.size >= 20_000)

        // Verify normalization and length constraints
        val seenNorm = mutableSetOf<String>()
        lines.forEach { word ->
            val norm = Dictionary.normalize(word)
            assertTrue("Word length out of range 3..8: $word", norm.length in 3..8)
            assertTrue("Duplicate normalized entry in asset: $word", seenNorm.add(norm))
            assertTrue("Word failed plausibility check: $word", Dictionary.isPlausibleExpandedEntry(word))
        }

        // Verify key regression words are in packaged asset
        val regressions = listOf("جحش", "سلب", "رقى", "رقي", "سماء", "مدرسة", "كتاب", "دفء", "ماء", "فاكهة", "طاووس", "تخييم", "قراءة")
        for (reg in regressions) {
            val normReg = Dictionary.normalize(reg)
            assertTrue("Regression word $reg ($normReg) must be present in asset", seenNorm.contains(normReg))
        }
    }
}
