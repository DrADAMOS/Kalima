package com.hazeralkalima.app

import com.hazeralkalima.app.ui.theme.GameTheme
import com.hazeralkalima.app.ui.theme.ThemeManager
import com.hazeralkalima.app.ui.theme.isLight
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun testCandyThemeIsLight() {
        val candy = ThemeManager.of(GameTheme.CANDY)
        assertTrue(candy.isLight())
        val original = ThemeManager.of(GameTheme.ORIGINAL)
        assertFalse(original.isLight())
    }
}
