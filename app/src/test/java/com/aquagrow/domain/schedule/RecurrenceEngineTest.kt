package com.aquagrow.domain.schedule

import com.aquagrow.domain.model.RecurrenceRule
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceEngineTest {
    private val start = LocalDate.of(2026, 1, 1) // Thursday

    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

    @Test
    fun once_onlyOnStartDate() {
        assertEquals(listOf(start), RecurrenceEngine.occurrences(RecurrenceRule.Once, start, null, start.minusDays(5), start.plusDays(5)))
        assertTrue(RecurrenceEngine.occurrences(RecurrenceRule.Once, start, null, start.plusDays(1), start.plusDays(9)).isEmpty())
    }

    @Test
    fun everyThreeDays_alignsToStartNotToWindow() {
        // Window starts mid-cycle: Jan 5 is not on the grid (1, 4, 7, 10...).
        val result = RecurrenceEngine.occurrences(RecurrenceRule.everyDays(3), start, null, d(1, 5), d(1, 12))
        assertEquals(listOf(d(1, 7), d(1, 10)), result)
    }

    @Test
    fun daily_respectsEndDate() {
        val result = RecurrenceEngine.occurrences(RecurrenceRule.everyDays(1), start, d(1, 3), start, d(1, 10))
        assertEquals(listOf(d(1, 1), d(1, 2), d(1, 3)), result)
    }

    @Test
    fun weekly_onChosenDays() {
        val rule = RecurrenceRule.weekly(setOf(MONDAY, FRIDAY))
        val result = RecurrenceEngine.occurrences(rule, start, null, start, d(1, 12))
        assertEquals(listOf(d(1, 2), d(1, 5), d(1, 9), d(1, 12)), result)
    }

    @Test
    fun everyTwoWeeks_skipsOffWeeks() {
        val rule = RecurrenceRule.weekly(setOf(WEDNESDAY), everyWeeks = 2)
        // Start week (Mon Dec 29) is week 0: Wed Dec 31 is before start, so first is Jan 14.
        val result = RecurrenceEngine.occurrences(rule, start, null, start, d(1, 31))
        assertEquals(listOf(d(1, 14), d(1, 28)), result)
    }

    @Test
    fun weekly_withNoDaysUsesStartWeekday() {
        val result = RecurrenceEngine.occurrences(RecurrenceRule.weekly(emptySet()), start, null, start, d(1, 20))
        assertEquals(listOf(d(1, 1), d(1, 8), d(1, 15)), result)
    }

    @Test
    fun monthly_day31_clampsToShortMonths() {
        val rule = RecurrenceRule.monthly(31)
        val result = RecurrenceEngine.occurrences(rule, d(1, 31), null, d(1, 1), d(4, 30))
        assertEquals(listOf(d(1, 31), d(2, 28), d(3, 31), d(4, 30)), result)
    }

    @Test
    fun everyThreeMonths_alignsToStartMonth() {
        val rule = RecurrenceRule.monthly(15, everyMonths = 3)
        val result = RecurrenceEngine.occurrences(rule, d(1, 15), null, d(2, 1), d(12, 31))
        assertEquals(listOf(d(4, 15), d(7, 15), d(10, 15)), result)
    }

    @Test
    fun nothingBeforeStart() {
        assertTrue(RecurrenceEngine.occurrences(RecurrenceRule.everyDays(1), start, null, d(1, 1).minusDays(10), d(1, 1).minusDays(1)).isEmpty())
    }

    @Test
    fun nextOnOrAfter_findsNextOccurrence() {
        assertEquals(d(1, 7), RecurrenceEngine.nextOnOrAfter(RecurrenceRule.everyDays(3), start, null, d(1, 5)))
    }
}
