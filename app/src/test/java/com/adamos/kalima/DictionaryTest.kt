package com.adamos.kalima

import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.ui.theme.GameTheme
import com.adamos.kalima.ui.theme.ThemeManager
import com.adamos.kalima.ui.theme.isLight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryTest {

    @Test
    fun testIsSafe() {
        assertFalse(Dictionary.isSafe("دم"))
        assertTrue(Dictionary.isSafe("قدم"))
        assertTrue(Dictionary.isSafe("ندم"))
        assertTrue(Dictionary.isSafe("دمية"))
        assertFalse(Dictionary.isSafe("موت"))
        assertTrue(Dictionary.isSafe("بيت"))
    }

    @Test
    fun testNormalize() {
        assertEquals("ا", Dictionary.normalize("أ"))
        assertEquals("ا", Dictionary.normalize("إ"))
        assertEquals("ا", Dictionary.normalize("آ"))
        assertEquals("ه", Dictionary.normalize("ة"))
        assertEquals("سلامه", Dictionary.normalize("سلامة"))
    }

    @Test
    fun testLengths() {
        Dictionary.lengths.forEach { len ->
            assertTrue(len in 3..8)
        }
    }

    @Test
    fun testRandomWordLength() {
        Dictionary.lengths.forEach { len ->
            val word = Dictionary.randomWord(len)
            assertEquals(len, Dictionary.normalize(word).length)
        }
    }

    @Test
    fun testCanonicalArabicSpellingIsPreserved() {
        assertTrue(Dictionary.wordsForLength(5).contains("مدرسة"))
        assertFalse(Dictionary.wordsForLength(5).contains("مدرسه"))
        assertTrue(Dictionary.isGuessable("مدرسه")) // comparison still normalizes variants
    }

    @Test
    fun testEnrichedDictionaryHasEveryLengthFromThreeToEight() {
        assertEquals(listOf(3, 4, 5, 6, 7, 8), Dictionary.lengths)
        Dictionary.lengths.forEach { length ->
            val words = Dictionary.wordsForLength(length)
            assertTrue("Expected a useful word pool for length $length", words.size >= 10)
            // No empty words or disallowed symbols
            words.forEach { w ->
                assertTrue(w.isNotBlank())
                assertTrue(w.all { ch -> ch in '\u0621'..'\u064A' || ch == '\u0629' || ch == '\u0649' })
                assertEquals(length, Dictionary.normalize(w).length)
            }
        }
    }

    @Test
    fun testNoDuplicatesPerLength() {
        Dictionary.lengths.forEach { length ->
            val words = Dictionary.wordsForLength(length)
            val uniqueNormalized = words.map { Dictionary.normalize(it) }.toSet()
            assertEquals("Duplicates found in length $length", words.size, uniqueNormalized.size)
        }
    }

    @Test
    fun testDailyWordConsistency() {
        val word1 = Dictionary.dailyWord(5, "2026-10-14")
        val word2 = Dictionary.dailyWord(5, "2026-10-14")
        assertEquals(word1, word2)
    }

    @Test
    fun testIsValidGuess() {
        assertTrue(Dictionary.isValidGuess("ضضضض"))
        assertFalse(Dictionary.isValidGuess("1234"))
        assertTrue(Dictionary.isValidGuess("بيت"))
    }

    @Test
    fun testRemoteDictionaryWordModad() {
        assertTrue("Expected word 'مضض' to be guessable from remote dictionary", Dictionary.isGuessable("مضض"))
    }

    @Test
    fun testCandyThemeIsLight() {
        val candy = ThemeManager.of(GameTheme.CANDY)
        assertTrue(candy.isLight())
        val original = ThemeManager.of(GameTheme.ORIGINAL)
        assertFalse(original.isLight())
    }
}
