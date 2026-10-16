package com.adamos.kalima.data

import androidx.compose.runtime.Immutable

/** حالة كل خلية في الشبكة */
enum class LetterStatus { EMPTY, CORRECT, PRESENT, ABSENT }

/** أوضاع اللعب */
enum class GameMode { DAILY, ENDLESS, RANDOM }

/** خلية حرف واحدة */
@Immutable
data class LetterCell(
    val letter: String = "",
    val status: LetterStatus = LetterStatus.EMPTY,
)

/**
 * حالة جولة واحدة من اللعبة.
 * منطق المطابقة: أخضر = صحيح في مكانه، أصفر = موجود بمكان خاطئ، رمادي = غير موجود.
 * يراعي تكرار الحروف، ويطبّع الألف والتاء المربوطة.
 */
class GameState(
    val mode: GameMode,
    val wordLength: Int,
    val targetWord: String,
    val maxAttempts: Int = 6,
    val extraRows: Int = 2,
    var hintCount: Int = 2,
    val hardMode: Boolean = false,
    /** وضع الأطفال الآمن: تُرفض الكلمات المحجوبة كتخمين. */
    val safeMode: Boolean = false,
) {
    init {
        require(wordLength > 0) { "طول الكلمة يجب أن يكون أكبر من صفر" }
        require(Dictionary.normalize(targetWord).length == wordLength) {
            "طول الكلمة الهدف (${targetWord.length}) لا يطابق wordLength ($wordLength)"
        }
    }

    var currentAttempt: Int = 0
        private set
    var currentColumn: Int = 0
        private set
    var isGameOver: Boolean = false
        private set
    var isWon: Boolean = false
        private set
    var extraRowsUsed: Int = 0
        private set

    val attempts: MutableList<String> = mutableListOf()

    val attemptsUsed: Int get() = attempts.size

    /** عدد الصفوف المعروضة حاليًا */
    val totalRows: Int get() = maxAttempts + extraRowsUsed

    private val grid: MutableList<MutableList<LetterCell>> = MutableList(maxAttempts) {
        MutableList(wordLength) { LetterCell() }
    }

    /** تتبع صريح لخلايا التلميحات لمنع خلطها مع الحروف المكتوبة يدوياً */
    private val hintedCells = mutableSetOf<Pair<Int, Int>>()

    fun cellAt(row: Int, col: Int): LetterCell =
        grid.getOrNull(row)?.getOrNull(col) ?: LetterCell()

    /** الصف الحالي قبل الإرسال */
    fun currentRowLetters(): String =
        if (isGameOver || currentAttempt >= grid.size) ""
        else (0 until wordLength).joinToString("") { grid[currentAttempt][it].letter }

    /** هل العمود في الصف الحالي مكشوف بتلميح؟ */
    private fun isHintedCol(col: Int): Boolean = (currentAttempt to col) in hintedCells

    /** إضافة حرف للصف الحالي (مع تخطي الأعمدة المكشوفة بالتلميح) */
    fun addLetter(letter: String) {
        if (isGameOver || currentAttempt >= grid.size) return
        while (currentColumn < wordLength && isHintedCol(currentColumn)) {
            currentColumn++
        }
        if (currentColumn >= wordLength) return
        grid[currentAttempt][currentColumn] = LetterCell(letter, LetterStatus.EMPTY)
        currentColumn++
        while (currentColumn < wordLength && isHintedCol(currentColumn)) {
            currentColumn++
        }
    }

    /** حذف آخر حرف مكتوب، مع الحفاظ على الخانات التي كشفها التلميح. */
    fun removeLetter() {
        if (isGameOver) return
        var col = currentColumn - 1
        while (col >= 0 && isHintedCol(col)) col--
        if (col < 0) return
        grid[currentAttempt][col] = LetterCell()
        currentColumn = col
    }

    /** إضافة صف إضافي (محدود) */
    fun addExtraRow(): Boolean {
        if (extraRowsUsed >= extraRows || isGameOver) return false
        extraRowsUsed++
        grid.add(MutableList(wordLength) { LetterCell() })
        return true
    }

    /**
     * حروف كشفها التلميح في الصف الحالي.
     */
    fun hintRevealedLetters(): Set<String> =
        hintedCells.asSequence()
            .filter { (row, _) -> row == currentAttempt }
            .mapNotNull { (row, col) -> grid.getOrNull(row)?.getOrNull(col)?.letter }
            .filter { it.isNotEmpty() }
            .map { Dictionary.normalize(it) }
            .toSet()

    /**
     * كشف حرف صحيح في موضعه.
     */
    fun useHint(): Boolean {
        if (hintCount <= 0 || isGameOver) return false
        val limit = minOf(wordLength, targetWord.length)
        for (c in 0 until limit) {
            val doneBefore = (0 until currentAttempt).any {
                grid.getOrNull(it)?.getOrNull(c)?.status == LetterStatus.CORRECT
            }
            val cell = grid.getOrNull(currentAttempt)?.getOrNull(c)
            val isFilledInCurrentRow = cell != null && cell.letter.isNotEmpty()
            // عدّ الحرف بدل البحث عن وجوده: يسمح بكشف المكرَّر في كلمات مثل «ممرات»
            // ما دام عدد ظهوره في الصف الحالي أقل من عدد ظهوره في الكلمة الهدف.
            val letterNorm = Dictionary.normalize(targetWord[c].toString())
            val totalInTarget = targetWord.count { Dictionary.normalize(it.toString()) == letterNorm }
            val usedInRow = (0 until wordLength).count {
                val l = grid.getOrNull(currentAttempt)?.getOrNull(it)?.letter.orEmpty()
                l.isNotEmpty() && Dictionary.normalize(l) == letterNorm
            }
            val letterQuotaReached = usedInRow >= totalInTarget
            if (!doneBefore && !isFilledInCurrentRow && !letterQuotaReached && cell != null) {
                hintCount--
                grid[currentAttempt][c] = LetterCell(targetWord[c].toString(), LetterStatus.EMPTY)
                hintedCells += currentAttempt to c
                while (currentColumn < wordLength && isHintedCol(currentColumn)) {
                    currentColumn++
                }
                return true
            }
        }
        return false
    }

    /**
     * تقييم تخمين — يرجع قائمة بنفس طول الكلمة:
     * 2 = صحيح، 1 = موجود بمكان خاطئ، 0 = غير موجود
     */
    fun evaluate(guess: String): List<Int> {
        require(guess.length == wordLength) {
            "طول التخمين (${guess.length}) يجب أن يطابق طول الجولة ($wordLength)"
        }
        val result = MutableList(wordLength) { 0 }
        val target = targetWord.map { Dictionary.normalize(it.toString()) }.toMutableList()
        val attempt = guess.map { Dictionary.normalize(it.toString()) }.toMutableList()

        // المرور الأول: الأخضر
        for (i in 0 until wordLength) {
            if (attempt[i] == target[i]) {
                result[i] = 2
                target[i] = "#"
                attempt[i] = "*"
            }
        }

        // المرور الثاني: الأصفر
        for (i in 0 until wordLength) {
            if (attempt[i] == "*") continue
            val idx = target.indexOf(attempt[i])
            if (idx != -1) {
                result[i] = 1
                target[idx] = "#"
                attempt[i] = "*"
            }
        }

        return result
    }

    /** إرسال تخمين للسيستم */
    fun submitGuess(guess: String): SubmitResult {
        if (isGameOver) return SubmitResult.AlreadyOver
        if (guess.length != wordLength) return SubmitResult.NotRightLength
        if (!Dictionary.isValidGuess(guess)) return SubmitResult.InvalidWord
        if (safeMode && !Dictionary.isSafe(guess)) return SubmitResult.BlockedWord
        if (!Dictionary.isGuessable(guess)) return SubmitResult.NotInDictionary

        // فحص الوضع الصعب (Hard Mode)
        if (hardMode && attemptsUsed > 0) {
            val lastGuess = attempts.last()
            val lastEval = evaluate(lastGuess)

            // 1. يجب الحفاظ على الحروف الصحيحة (خضراء) في مكانها
            for (i in 0 until wordLength) {
                if (lastEval[i] == 2 && Dictionary.normalize(guess[i].toString()) != Dictionary.normalize(lastGuess[i].toString())) {
                    return SubmitResult.BreaksGreen
                }
            }

            // 2. يجب استخدام الحروف الموجودة (صفراء) في التخمين الجديد
            val normalizedGuess = Dictionary.normalize(guess)
            for (i in 0 until wordLength) {
                if (lastEval[i] == 1 &&
                    !normalizedGuess.contains(Dictionary.normalize(lastGuess[i].toString()))
                ) {
                    return SubmitResult.MissingYellow
                }
            }
        }

        attempts.add(guess)
        val eval = evaluate(guess)

        // تعيين حالة الخلايا في الصف الحالي
        for (i in 0 until wordLength) {
            val status = when (eval[i]) {
                2 -> LetterStatus.CORRECT
                1 -> LetterStatus.PRESENT
                else -> LetterStatus.ABSENT
            }
            grid[currentAttempt][i] = LetterCell(guess[i].toString(), status)
        }

        if (guess.map { Dictionary.normalize(it.toString()) } == targetWord.map { Dictionary.normalize(it.toString()) }) {
            isWon = true
            isGameOver = true
            return SubmitResult.Win
        }

        currentAttempt++
        currentColumn = 0

        if (currentAttempt >= totalRows) {
            isGameOver = true
            return SubmitResult.Lose
        }

        return SubmitResult.Accepted
    }

    /** حروف لوحة المفاتيح الملونة (أولوية: أخضر > أصفر > رمادي) */
    fun usedLetters(): Map<String, LetterStatus> {
        val map = mutableMapOf<String, LetterStatus>()
        for (r in 0 until currentAttempt) {
            val guess = attempts.getOrNull(r) ?: continue
            val eval = evaluate(guess)
            for (i in guess.indices) {
                val letter = Dictionary.normalize(guess[i].toString())
                val status = when (eval[i]) {
                    2 -> LetterStatus.CORRECT
                    1 -> LetterStatus.PRESENT
                    else -> LetterStatus.ABSENT
                }
                val current = map[letter]
                if (current == null || status.ordinal > current.ordinal) {
                    map[letter] = status
                }
            }
        }
        return map
    }

    /** لقطة كاملة للحالة الحالية للشبكة */
    fun rowsSnapshot(): List<List<LetterCell>> =
        grid.map { row -> row.map { it.copy() } }
}

/** نتيجة محاولة التخمين */
enum class SubmitResult {
    Accepted,
    Win,
    Lose,
    NotRightLength,
    InvalidWord,
    NotInDictionary,
    /** كلمة محجوبة في وضع الأطفال الآمن */
    BlockedWord,
    BreaksGreen,
    MissingYellow,
    AlreadyOver,
}
