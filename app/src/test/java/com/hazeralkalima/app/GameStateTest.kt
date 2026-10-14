package com.hazeralkalima.app

import com.hazeralkalima.app.data.GameMode
import com.hazeralkalima.app.data.GameState
import com.hazeralkalima.app.data.LetterStatus
import com.hazeralkalima.app.data.SubmitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    @Test
    fun testEvaluateDuplicateLetters() {
        val game = GameState(GameMode.RANDOM, 4, "تفاح")
        val eval = game.evaluate("تفتح")
        assertEquals(2, eval[0]) // ت
        assertEquals(2, eval[1]) // ف
        assertEquals(0, eval[2]) // ت الثانية والهدف فيه ت واحدة -> غير موجودة
        assertEquals(2, eval[3]) // ح
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEvaluateRejectsWrongLength() {
        val game = GameState(GameMode.RANDOM, 4, "تفاح")
        game.evaluate("تفا")
    }

    @Test
    fun testUsedLettersPriority() {
        val game = GameState(GameMode.RANDOM, 3, "بيت")
        game.submitGuess("نهر")
        val used = game.usedLetters()
        assertEquals(LetterStatus.ABSENT, used["ن"])
    }

    @Test
    fun testWinOnAttempt7AfterExtraRow() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        game.addExtraRow()
        repeat(6) {
            game.submitGuess("نهر")
        }
        val result = game.submitGuess("باب")
        assertEquals(SubmitResult.Win, result)
        assertTrue(game.isWon)
        assertTrue(game.isGameOver)
    }

    @Test
    fun testExtraRowsAreLimited() {
        val game = GameState(GameMode.RANDOM, 3, "باب", extraRows = 2)
        assertTrue(game.addExtraRow())
        assertTrue(game.addExtraRow())
        assertFalse(game.addExtraRow())
        assertEquals(8, game.totalRows)
    }

    @Test
    fun testNoCrashOnCurrentRowLettersAfterLoss() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        repeat(6) {
            game.submitGuess("نهر")
        }
        assertTrue(game.isGameOver)
        assertEquals("", game.currentRowLetters())
    }

    @Test
    fun testUnknownWordIsRejected() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        // "ببب" حروف عربية لكنها ليست في قاموس اللعبة
        assertEquals(SubmitResult.NotInDictionary, game.submitGuess("ببب"))
    }

    @Test
    fun testNonArabicIsRejected() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        assertEquals(SubmitResult.InvalidWord, game.submitGuess("abc"))
    }

    @Test
    fun testWrongLengthUsesNotRightLength() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        assertEquals(SubmitResult.NotRightLength, game.submitGuess("نهرن"))
    }

    @Test
    fun testHintExhaustsCount() {
        val game = GameState(GameMode.RANDOM, 3, "باب", hintCount = 1)
        assertTrue(game.useHint())
        assertFalse(game.useHint())
        assertEquals(0, game.hintCount)
    }

    @Test
    fun testHintDoesNotDuplicateRevealedLetter() {
        val game = GameState(GameMode.RANDOM, 4, "مطر", hintCount = 2)
        game.useHint()
        game.useHint()
        val rowCells = game.rowsSnapshot()[game.currentAttempt]
        val letters = rowCells.map { it.letter }.filter { it.isNotEmpty() }
        assertEquals(letters.size, letters.toSet().size)
    }

    @Test
    fun testGameOverAfterMaxAttempts() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        repeat(5) { assertEquals(SubmitResult.Accepted, game.submitGuess("نهر")) }
        assertEquals(SubmitResult.Lose, game.submitGuess("نهر"))
        assertTrue(game.isGameOver)
        assertFalse(game.isWon)
        assertEquals(SubmitResult.AlreadyOver, game.submitGuess("نهر"))
    }
}
