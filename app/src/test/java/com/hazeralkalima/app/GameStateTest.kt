package com.hazeralkalima.app

import com.hazeralkalima.app.data.Dictionary
import com.hazeralkalima.app.data.GameMode
import com.hazeralkalima.app.data.GameState
import com.hazeralkalima.app.data.LetterStatus
import com.hazeralkalima.app.data.SubmitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    @Test
    fun testEvaluateDuplicateLetters() {
        val target = Dictionary.randomWord(4)
        val game = GameState(GameMode.RANDOM, 4, target)
        val eval = game.evaluate(target)
        assertEquals(listOf(2, 2, 2, 2), eval)
    }

    @Test
    fun testEvaluateRejectsWrongLength() {
        val game = GameState(GameMode.RANDOM, 4, Dictionary.randomWord(4))
        assertThrows(IllegalArgumentException::class.java) {
            game.evaluate("قلم") // 3 letters instead of 4
        }
    }

    @Test
    fun testUnknownWordIsRejected() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        val result = game.submitGuess("ضضض")
        assertEquals(SubmitResult.NotInDictionary, result)
    }

    @Test
    fun testExtraRowsAreLimited() {
        val game = GameState(GameMode.RANDOM, 3, "باب", extraRows = 2)
        assertEquals(true, game.addExtraRow())
        assertEquals(true, game.addExtraRow())
        assertEquals(false, game.addExtraRow())
        assertEquals(8, game.totalRows) // 6 + 2
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
        // هدف بطول 4، تلميحان متتاليان يجب ألا يكشفا نفس الحرف في عمودين
        val target = Dictionary.randomWord(4)
        val game = GameState(GameMode.RANDOM, 4, target, hintCount = 2)
        game.useHint()
        game.useHint()
        val rowCells = game.rowsSnapshot()[game.currentAttempt]
        val letters = rowCells.map { it.letter }.filter { it.isNotEmpty() }
        assertEquals(letters.size, letters.toSet().size)
    }

    @Test
    fun testHardModeBlocksBrokenGreen() {
        // target: "بيت"
        val game = GameState(GameMode.RANDOM, 3, "بيت", hardMode = true)
        game.submitGuess("بحر") // 'ب' is green at index 0
        val res = game.submitGuess("نهر") // index 0 is 'ن' instead of 'ب', breaking green 'ب'
        assertEquals(SubmitResult.BreaksGreen, res)
    }

    @Test
    fun testHardModeRequiresYellowLetters() {
        // target: "علم"
        val game = GameState(GameMode.RANDOM, 3, "علم", hardMode = true)
        game.submitGuess("عمل") // 'ع' is green, 'ل' is green/yellow, 'م' is yellow
        val res = game.submitGuess("عدل") // missing yellow letters (e.g. 'م')
        assertEquals(SubmitResult.MissingYellow, res)
    }

    @Test
    fun testGameOverAfterMaxAttempts() {
        val game = GameState(GameMode.RANDOM, 3, "باب", maxAttempts = 2, extraRows = 0)
        assertEquals(SubmitResult.Accepted, game.submitGuess("بحر"))
        assertEquals(SubmitResult.Lose, game.submitGuess("نهر"))
        assertEquals(SubmitResult.AlreadyOver, game.submitGuess("بيت"))
        assertTrue(game.isGameOver)
    }

    @Test
    fun testUsedLettersPriority() {
        val game = GameState(GameMode.RANDOM, 3, "بيت")
        game.submitGuess("بحر")
        val used = game.usedLetters()
        assertEquals(LetterStatus.CORRECT, used["ب"])
    }

    @Test
    fun testWinOnAttempt7AfterExtraRow() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        game.addExtraRow()
        repeat(6) {
            game.submitGuess("بحر")
        }
        val result = game.submitGuess("باب")
        assertEquals(SubmitResult.Win, result)
        assertTrue(game.isWon)
        assertTrue(game.isGameOver)
    }

    @Test
    fun testNoCrashOnCurrentRowLettersAfterLoss() {
        val game = GameState(GameMode.RANDOM, 3, "باب", maxAttempts = 1, extraRows = 0)
        game.submitGuess("بحر")
        assertTrue(game.isGameOver)
        val letters = game.currentRowLetters()
        assertEquals("", letters)
    }
}
