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
}
