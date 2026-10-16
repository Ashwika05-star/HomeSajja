package com.homesajja.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateFilterTest {

    @Test
    fun allFilterMatchesAnyTimestamp() {
        assertTrue(DateFilter.All.matches(0L))
        assertTrue(DateFilter.All.matches(System.currentTimeMillis()))
        assertTrue(DateFilter.All.matches(System.currentTimeMillis() - 500L * 24 * 60 * 60 * 1000L))
    }

    @Test
    fun last30DaysFilterMatchesRecentOnly() {
        val now = System.currentTimeMillis()
        assertTrue(DateFilter.Last30Days.matches(now))
        assertTrue(DateFilter.Last30Days.matches(now - 10L * 24 * 60 * 60 * 1000L))
        assertTrue(DateFilter.Last30Days.matches(now - 29L * 24 * 60 * 60 * 1000L))
        assertFalse(DateFilter.Last30Days.matches(now - 35L * 24 * 60 * 60 * 1000L))
    }

    @Test
    fun last3MonthsFilterMatchesWithin90Days() {
        val now = System.currentTimeMillis()
        assertTrue(DateFilter.Last3Months.matches(now))
        assertTrue(DateFilter.Last3Months.matches(now - 60L * 24 * 60 * 60 * 1000L))
        assertTrue(DateFilter.Last3Months.matches(now - 89L * 24 * 60 * 60 * 1000L))
        assertFalse(DateFilter.Last3Months.matches(now - 95L * 24 * 60 * 60 * 1000L))
    }

    @Test
    fun yearFilterMatchesWithinCalendarYear() {
        val filter2025 = DateFilter.Year(2025)

        val cal = Calendar.getInstance()
        cal.set(2025, Calendar.JUNE, 15, 12, 0, 0)
        assertTrue(filter2025.matches(cal.timeInMillis))

        cal.set(2024, Calendar.DECEMBER, 31, 12, 0, 0)
        assertFalse(filter2025.matches(cal.timeInMillis))

        cal.set(2026, Calendar.JANUARY, 1, 12, 0, 0)
        assertFalse(filter2025.matches(cal.timeInMillis))
    }

    @Test
    fun defaultOptionsGeneratesAllLast30DaysLast3MonthsAnd5Years() {
        val options = DateFilter.defaultOptions(currentYear = 2026)
        assertEquals(8, options.size)
        assertEquals(DateFilter.All, options[0])
        assertEquals(DateFilter.Last30Days, options[1])
        assertEquals(DateFilter.Last3Months, options[2])
        assertEquals(DateFilter.Year(2026), options[3])
        assertEquals(DateFilter.Year(2025), options[4])
        assertEquals(DateFilter.Year(2024), options[5])
        assertEquals(DateFilter.Year(2023), options[6])
        assertEquals(DateFilter.Year(2022), options[7])
    }

    @Test
    fun hindiLabelsAreLocalized() {
        assertEquals("सभी समय", DateFilter.All.label(true))
        assertEquals("पिछले 30 दिन", DateFilter.Last30Days.label(true))
        assertEquals("पिछले 3 महीने", DateFilter.Last3Months.label(true))
        assertEquals("2026", DateFilter.Year(2026).label(true))
    }
}
