package com.homesajja.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `a stored value comes back as the same mode`() {
        ThemeMode.entries.forEach { mode -> assertEquals(mode, ThemeMode.fromStored(mode.name)) }
    }

    @Test
    fun `nothing stored, or something unrecognised, means System`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("SEPIA"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("dark"))
    }

    @Test
    fun `Light and Dark ignore the phone setting, System follows it`() {
        assertFalse(ThemeMode.LIGHT.resolveDark(systemDark = true))
        assertFalse(ThemeMode.LIGHT.resolveDark(systemDark = false))
        assertTrue(ThemeMode.DARK.resolveDark(systemDark = true))
        assertTrue(ThemeMode.DARK.resolveDark(systemDark = false))
        assertTrue(ThemeMode.SYSTEM.resolveDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.resolveDark(systemDark = false))
    }

    @Test
    fun `the three choices appear in the order Light, Dark, System`() {
        assertEquals(listOf("Light", "Dark", "System"), ThemeMode.entries.map { it.displayName })
    }
}
