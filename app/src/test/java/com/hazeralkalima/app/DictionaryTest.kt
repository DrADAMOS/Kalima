package com.hazeralkalima.app

import com.hazeralkalima.app.data.Dictionary
import com.hazeralkalima.app.ui.theme.GameTheme
import com.hazeralkalima.app.ui.theme.ThemeManager
import com.hazeralkalima.app.ui.theme.isLight
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
    fun testDailyWordConsistency() {
        val word1 = Dictionary.dailyWord(5, "2026-10-14")
        val word2 = Dictionary.dailyWord(5, "2026-10-14")
        assertEquals(word1, word2)
    }

    @Test
    fun testIsValidGuess() {
        assertFalse(Dictionary.isValidGuess("ضضضض"))
        assertTrue(Dictionary.isValidGuess("بيت"))
    }

    @Test
    fun testCandyThemeIsLight() {
        val candy = ThemeManager.of(GameTheme.CANDY)
        assertTrue(candy.isLight())
        val original = ThemeManager.of(GameTheme.ORIGINAL)
        assertFalse(original.isLight())
    }
}
