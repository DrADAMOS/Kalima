package com.adamos.kalima

import com.adamos.kalima.data.Dictionary
import com.adamos.kalima.data.GameMode
import com.adamos.kalima.data.GameState
import com.adamos.kalima.data.LetterStatus
import com.adamos.kalima.data.SubmitResult
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
        // "باب" فيها ب مكررة، فالتلميح الثاني يجب أن يتخطى ما كُشف
        val game = GameState(GameMode.RANDOM, 3, "باب", hintCount = 2)
        assertTrue(game.useHint())
        assertTrue(game.useHint())
        val allLetters = game.rowsSnapshot().flatten()
            .map { it.letter }
            .filter { it.isNotEmpty() }
        assertEquals(allLetters.size, allLetters.toSet().size)
    }

    @Test
    fun testHintSkippingAndBackspaceProtection() {
        val target = Dictionary.randomWord(4)
        val game = GameState(GameMode.RANDOM, 4, target, hintCount = 1)
        assertTrue(game.useHint())
        game.addLetter("ق")
        game.addLetter("ل")
        game.addLetter("م")
        assertEquals(4, game.currentRowLetters().length)
    }

    @Test
    fun backspaceDeletesTypedLettersButPreservesHintedCells() {
        val game = GameState(GameMode.RANDOM, 4, "سلام", hintCount = 1)
        assertTrue(game.useHint())
        game.addLetter("ق")
        game.addLetter("ل")
        assertEquals(3, game.currentRowLetters().length)
        game.removeLetter()
        game.removeLetter()
        assertEquals("س", game.currentRowLetters())
        assertEquals(setOf("س"), game.hintRevealedLetters())
    }

    @Test
    fun typedLettersAreNotReportedAsHintRevealed() {
        val game = GameState(GameMode.RANDOM, 4, "سلام", hintCount = 1)
        game.addLetter("ق")
        game.addLetter("ل")
        assertTrue(game.hintRevealedLetters().isEmpty())
    }

    // --- Focused Backspace Regression Tests ---

    @Test
    fun testHintedCellBeforeTypedLetters() {
        val game = GameState(GameMode.RANDOM, 4, "كتاب", hintCount = 1)
        assertTrue(game.useHint()) // index 0 is 'ك'
        game.addLetter("ت")
        game.addLetter("ا")
        game.addLetter("ب")
        assertEquals("كتاب", game.currentRowLetters())
        game.removeLetter()
        game.removeLetter()
        game.removeLetter()
        game.removeLetter() // should not delete hinted index 0
        assertEquals("ك", game.cellAt(0, 0).letter)
        assertTrue(game.cellAt(0, 1).letter.isEmpty())
        assertTrue(game.cellAt(0, 2).letter.isEmpty())
        assertTrue(game.cellAt(0, 3).letter.isEmpty())
    }

    @Test
    fun testHintedCellBetweenTypedLetters() {
        val game = GameState(GameMode.RANDOM, 4, "قلمي", hintCount = 1)
        game.addLetter("ق") // index 0
        assertTrue(game.useHint()) // index 1 is 'ل'
        game.addLetter("م") // index 2
        game.addLetter("ي") // index 3
        assertEquals("قلمي", game.currentRowLetters())

        game.removeLetter() // index 3
        game.removeLetter() // index 2
        game.removeLetter() // skips hinted index 1, removes index 0
        assertEquals("ل", game.cellAt(0, 1).letter)
        assertTrue(game.cellAt(0, 0).letter.isEmpty())
        assertTrue(game.cellAt(0, 2).letter.isEmpty())
        assertTrue(game.cellAt(0, 3).letter.isEmpty())
    }

    @Test
    fun testMultipleHintedCellsAndConsecutiveBackspaces() {
        val game = GameState(GameMode.RANDOM, 4, "سلام", hintCount = 2)
        assertTrue(game.useHint()) // index 0 ('س')
        game.addLetter("ق")       // index 1
        assertTrue(game.useHint()) // index 2 ('ا')
        game.addLetter("م")       // index 3
        
        game.removeLetter() // removes index 3 ('م')
        game.removeLetter() // skips hinted index 2 ('ا'), removes index 1 ('ق')
        
        // Hinted cells at index 0 ('س') and index 2 ('ا') must remain intact
        assertEquals("س", game.cellAt(0, 0).letter)
        assertEquals("ا", game.cellAt(0, 2).letter)
        assertTrue(game.cellAt(0, 1).letter.isEmpty())
        assertTrue(game.cellAt(0, 3).letter.isEmpty())

        // Subsequent addLetter should fill next available unhinted cell (index 1)
        game.addLetter("ل")
        assertEquals("ل", game.cellAt(0, 1).letter)
    }

    @Test
    fun testBackspaceWhenEveryEarlierCellIsHinted() {
        val game = GameState(GameMode.RANDOM, 3, "بحر", hintCount = 3)
        assertTrue(game.useHint()) // index 0 ('ب')
        assertTrue(game.useHint()) // index 1 ('ح')
        assertTrue(game.useHint()) // index 2 ('ر')
        
        val beforeLetters = game.currentRowLetters()
        assertEquals("بحر", beforeLetters)
        game.addLetter("ل")
        assertEquals("بحر", game.currentRowLetters()) // unchanged because row is full of hints

        game.removeLetter()
        game.removeLetter()
        game.removeLetter()
        
        // All three hinted cells must remain intact
        assertEquals("ب", game.cellAt(0, 0).letter)
        assertEquals("ح", game.cellAt(0, 1).letter)
        assertEquals("ر", game.cellAt(0, 2).letter)
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
    @Test
    fun hintCanRevealRepeatedLettersInSameRow() {
        val game = GameState(GameMode.RANDOM, 5, "ممرات", hintCount = 2)
        assertTrue(game.useHint())
        assertTrue(game.useHint())
        assertEquals("م", game.cellAt(0, 0).letter)
        assertEquals("م", game.cellAt(0, 1).letter)
        assertEquals(0, game.hintCount)
        assertFalse(game.useHint())
    }

    @Test
    fun hintDoesNotRevealLetterBeyondItsCountInTarget() {
        val game = GameState(GameMode.RANDOM, 3, "باب", hintCount = 3)
        game.addLetter("ب")
        game.addLetter("ب")
        // الخانتان 0 و1 ممتلئتان، و«ب» ظهرت مرتين وهذا عددها في الهدف: لا تلميح متاح
        assertFalse(game.useHint())
        assertEquals(3, game.hintCount)
    }

    @Test
    fun safeModeRejectsBlockedWordButNormalModeDoesNot() {
        val safe = GameState(GameMode.RANDOM, 3, "باب", safeMode = true)
        assertEquals(SubmitResult.BlockedWord, safe.submitGuess("خمر"))

        val normal = GameState(GameMode.RANDOM, 3, "باب", safeMode = false)
        assertTrue(normal.submitGuess("خمر") != SubmitResult.BlockedWord)
    }
}
