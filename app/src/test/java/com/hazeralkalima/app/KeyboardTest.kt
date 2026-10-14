package com.hazeralkalima.app

import com.hazeralkalima.app.ui.components.KeyboardRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardTest {

    @Test
    fun testKeyboardLayoutAndRows() {
        assertEquals(3, KeyboardRows.size)
        KeyboardRows.forEach { row ->
            assertEquals(11, row.size)
        }

        val allKeys = KeyboardRows.flatten()
        assertEquals(33, allKeys.size)
        assertEquals(33, allKeys.toSet().size) // 33 distinct keys

        assertEquals("ض", KeyboardRows[0].first())
        assertEquals("ش", KeyboardRows[1].first())
        assertEquals("ذ", KeyboardRows[2].first())

        assertEquals(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"),
            KeyboardRows[0]
        )

        val required = setOf(
            "ا", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص",
            "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م", "ن", "ه", "و", "ي",
            "ء", "ؤ", "ئ", "ى", "ة"
        )
        assertTrue(allKeys.containsAll(required))
    }
}
