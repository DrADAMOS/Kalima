package com.adamos.kalima.data

/** حالة كل خلية في الشبكة */
enum class LetterStatus { EMPTY, CORRECT, PRESENT, ABSENT }

/** أوضاع اللعب */
enum class GameMode { DAILY, ENDLESS, RANDOM }

/** خلية حرف واحدة */
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

    fun cellAt(row: Int, col: Int): LetterCell =
        grid.getOrNull(row)?.getOrNull(col) ?: LetterCell()

    /** الصف الحالي قبل الإرسال */
    fun currentRowLetters(): String =
        if (isGameOver || currentAttempt >= grid.size) ""
        else (0 until wordLength).joinToString("") { grid[currentAttempt][it].letter }

    /** إضافة حرف للصف الحالي */
    fun addLetter(letter: String) {
        if (isGameOver || currentAttempt >= grid.size || currentColumn >= wordLength) return
        grid[currentAttempt][currentColumn] = LetterCell(letter, LetterStatus.EMPTY)
        currentColumn++
    }

    /** حذف آخر حرف */
    fun removeLetter() {
        if (isGameOver || currentColumn == 0) return
        currentColumn--
        grid[currentAttempt][currentColumn] = LetterCell()
    }

    /** إضافة صف إضافي (محدود) */
    fun addExtraRow(): Boolean {
        if (extraRowsUsed >= extraRows || isGameOver) return false
        extraRowsUsed++
        grid.add(MutableList(wordLength) { LetterCell() })
        return true
    }

    /**
     * حروف كشفها التلميح في الصف الحالي (بحالة EMPTY).
     * تُستخدم لتمييزها في لوحة المفاتيح عن الحروف غير المستخدمة.
     */
    fun hintRevealedLetters(): Set<String> =
        grid.flatten()
            .filter { it.status == LetterStatus.EMPTY && it.letter.isNotEmpty() }
            .map { Dictionary.normalize(it.letter) }
            .toSet()

    /**
     * كشف حرف صحيح في موضعه.
     * لا يكشف عمودًا كان صحيحًا في محاولة سابقة، ولا عمودًا ممتلئًا
     * في الصف الحالي، ولا حرفًا مكشوفًا أصلًا في الصف الحالي.
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
            val isSameLetterRevealedEmpty = (0 until wordLength).any {
                grid.getOrNull(currentAttempt)?.getOrNull(it)?.letter == targetWord[c].toString()
            }
            if (!doneBefore && !isFilledInCurrentRow && !isSameLetterRevealedEmpty && cell != null) {
                hintCount--
                grid[currentAttempt][c] = LetterCell(targetWord[c].toString(), LetterStatus.EMPTY)
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
            }
        }
        return result
    }

    /** إرسال تخمين ويرجع النتيجة */
    fun submitGuess(guess: String): SubmitResult {
        if (isGameOver) return SubmitResult.AlreadyOver
        if (guess.length != wordLength) return SubmitResult.NotRightLength
        if (!Dictionary.isValidGuess(guess)) return SubmitResult.InvalidWord
        if (!Dictionary.isGuessable(guess)) return SubmitResult.NotInDictionary

        // قيود الوضع الصعب
        if (hardMode && attempts.isNotEmpty()) {
            val lastGuess = attempts.last()
            val lastEval = evaluate(lastGuess)
            val normalizedGuess = Dictionary.normalize(guess)

            for (i in 0 until wordLength) {
                if (lastEval[i] == 2 &&
                    Dictionary.normalize(guess[i].toString()) !=
                    Dictionary.normalize(lastGuess[i].toString())
                ) {
                    return SubmitResult.BreaksGreen
                }
            }
            for (i in 0 until wordLength) {
                if (lastEval[i] == 1 &&
                    !normalizedGuess.contains(Dictionary.normalize(lastGuess[i].toString()))
                ) {
                    return SubmitResult.MissingYellow
                }
            }
        }

        // اكتب الصف وألوانه
        val evaluation = evaluate(guess)
        for (i in 0 until wordLength) {
            val status = when (evaluation[i]) {
                2 -> LetterStatus.CORRECT
                1 -> LetterStatus.PRESENT
                else -> LetterStatus.ABSENT
            }
            grid[currentAttempt][i] = LetterCell(guess[i].toString(), status)
        }
        attempts.add(guess)

        if (evaluation.all { it == 2 }) {
            isGameOver = true
            isWon = true
            return SubmitResult.Win
        }

        currentAttempt++
        currentColumn = 0

        if (currentAttempt >= totalRows) {
            isGameOver = true
            isWon = false
            return SubmitResult.Lose
        }
        return SubmitResult.Accepted
    }

    private fun LetterStatus.priority(): Int = when (this) {
        LetterStatus.CORRECT -> 3
        LetterStatus.PRESENT -> 2
        LetterStatus.ABSENT -> 1
        LetterStatus.EMPTY -> 0
    }

    /** خريطة الحروف المستخدمة وأفضل حالة لكل منها (مطبّعة لتشارك أ/إ/آ مع ا) */
    fun usedLetters(): Map<String, LetterStatus> {
        val map = mutableMapOf<String, LetterStatus>()
        for (r in 0 until currentAttempt) {
            for (c in 0 until wordLength) {
                val cell = grid.getOrNull(r)?.getOrNull(c) ?: continue
                if (cell.letter.isEmpty()) continue
                val normalizedKey = Dictionary.normalize(cell.letter)
                val existing = map[normalizedKey]
                if (existing == null || cell.status.priority() > existing.priority()) {
                    map[normalizedKey] = cell.status
                }
            }
        }
        return map
    }

    /** كل الصفوف للعرض (نسخة عميقة لحماية الحالة الداخلية) */
    fun rowsSnapshot(): List<List<LetterCell>> = grid.map { it.toList() }
}

/** نتيجة إرسال تخمين */
enum class SubmitResult {
    Accepted,
    Win,
    Lose,
    /** ليس حروفًا عربية، أو طول خارج النطاق المسموح */
    InvalidWord,
    /** عربية صحيحة لكنها ليست في قاموس اللعبة */
    NotInDictionary,
    /** طول التخمين لا يطابق طول الجولة */
    NotRightLength,
    /** أُبقيت للتوافق مع الاختبارات القديمة */
    TooShort,
    BreaksGreen,
    MissingYellow,
    AlreadyOver,
}

