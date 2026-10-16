package com.adamos.kalima

import com.adamos.kalima.data.Dictionary
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DictionaryBuildTest {

    @Test
    fun verifyPackagedAssetQualityAndNormalization() {
        val assetFile = findFile(
            "src/main/assets/arabic-guess-words.txt",
            "../app/src/main/assets/arabic-guess-words.txt",
            "app/src/main/assets/arabic-guess-words.txt"
        ) ?: throw IllegalStateException("arabic-guess-words.txt asset file not found")

        val lines = assetFile.readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue("Asset must contain at least 20,000 verified entries, found: ${lines.size}", lines.size >= 20_000)

        val verifiedNorm = mutableSetOf<String>()
        val lengthCounts = mutableMapOf<Int, Int>()

        for (word in lines) {
            val norm = Dictionary.normalize(word)
            assertTrue("Word length out of range 3..8: $word", norm.length in 3..8)
            assertTrue("Duplicate normalized entry in asset: $word", verifiedNorm.add(norm))
            assertTrue("Plausibility check failed: $word", Dictionary.isPlausibleExpandedEntry(word))
            lengthCounts[norm.length] = (lengthCounts[norm.length] ?: 0) + 1
        }

        // Verify key regression words are in packaged asset
        val regressions = listOf("جحش", "سلب", "رقى", "رقي", "سماء", "مدرسة", "كتاب", "دفء", "ماء", "فاكهة", "طاووس", "تخييم", "قراءة")
        for (reg in regressions) {
            val normReg = Dictionary.normalize(reg)
            assertTrue("Regression word $reg ($normReg) must be present in asset", verifiedNorm.contains(normReg))
        }

        println("=== PACKAGED ASSET VERIFICATION PASSED ===")
        println("Total Verified Packaged Words: ${lines.size}")
        for (len in 3..8) {
            println("  - Length $len: ${lengthCounts[len] ?: 0} words")
        }
    }

    private fun findFile(vararg paths: String): File? {
        for (p in paths) {
            val f = File(p)
            if (f.exists()) return f
        }
        return null
    }
}
