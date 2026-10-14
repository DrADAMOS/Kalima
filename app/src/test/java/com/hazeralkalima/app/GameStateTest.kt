package com.hazeralkalima.app

import com.hazeralkalima.app.data.GameMode
import com.hazeralkalima.app.data.GameState
import com.hazeralkalima.app.data.LetterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    @Test
    fun testEvaluateDuplicateLetters() {
        val game = GameState(GameMode.RANDOM, 4, "تفاح")
        val eval = game.evaluate("تفتح")
        assertEquals(2, eval[0]) // ت
        assertEquals(2, eval[1]) // ف
        assertEquals(0, eval[2]) // ت (second t when target has only one ت) -> absent
        assertEquals(2, eval[3]) // ح
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
        assertEquals(com.hazeralkalima.app.data.SubmitResult.Win, result)
        assertTrue(game.isWon)
        assertTrue(game.isGameOver)
    }

    @Test
    fun testNoCrashOnCurrentRowLettersAfterLoss() {
        val game = GameState(GameMode.RANDOM, 3, "باب")
        repeat(6) {
            game.submitGuess("بحر")
        }
        assertTrue(game.isGameOver)
        val letters = game.currentRowLetters()
        assertEquals("", letters)
    }
}
