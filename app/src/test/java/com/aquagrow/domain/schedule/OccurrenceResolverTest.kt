package com.aquagrow.domain.schedule

import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.OccurrenceState
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.usecase.ObserveWeeklyCareUseCase
import com.aquagrow.ui.screens.home.Greeting
import com.aquagrow.ui.screens.home.greetingFor
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrenceResolverTest {
    private val today = LocalDate.of(2026, 10, 8)
    private val daily = CareTask(
        id = 1,
        title = "Water",
        module = CareModule.GARDEN,
        type = TaskType.WATERING,
        startDate = today.minusDays(30),
        recurrence = RecurrenceRule.everyDays(1),
    )

    @Test
    fun appliesStoredState() {
        val states = listOf(OccurrenceState(1, today, OccurrenceStatus.DONE))
        val occ = OccurrenceResolver.resolve(listOf(daily), states, today, today).single()
        assertEquals(OccurrenceStatus.DONE, occ.status)
    }

    @Test
    fun rescheduledOccurrence_movesAndIsNotDuplicated() {
        // Yesterday's watering moved to today: today should show TWO occurrences
        // (today's own + the moved one), each exactly once, and yesterday none.
        val states = listOf(OccurrenceState(1, today.minusDays(1), OccurrenceStatus.PENDING, rescheduledDate = today))
        val todayList = OccurrenceResolver.resolve(listOf(daily), states, today, today)
        assertEquals(2, todayList.size)
        assertEquals(setOf(today, today.minusDays(1)), todayList.map { it.originalDate }.toSet())
        assertTrue(OccurrenceResolver.resolve(listOf(daily), states, today.minusDays(1), today.minusDays(1)).isEmpty())
    }

    @Test
    fun rescheduledFromOutsideWindow_isIncluded() {
        val states = listOf(OccurrenceState(1, today.minusDays(20), OccurrenceStatus.PENDING, rescheduledDate = today.plusDays(3)))
        val result = OccurrenceResolver.resolve(listOf(daily), states, today.plusDays(3), today.plusDays(3))
        assertEquals(setOf(today.plusDays(3), today.minusDays(20)), result.map { it.originalDate }.toSet())
    }

    @Test
    fun disabledTasksProduceNothing() {
        assertTrue(OccurrenceResolver.resolve(listOf(daily.copy(enabled = false)), emptyList(), today, today).isEmpty())
    }

    @Test
    fun overdue_onlyLatestMissPerTask() {
        val overdue = OccurrenceResolver.overdue(listOf(daily), emptyList(), today)
        assertEquals(1, overdue.size)
        assertEquals(today.minusDays(1), overdue.single().date)
    }

    @Test
    fun overdue_ignoresMissesOlderThanHandledOne() {
        val states = listOf(OccurrenceState(1, today.minusDays(1), OccurrenceStatus.DONE))
        assertTrue(OccurrenceResolver.overdue(listOf(daily), states, today).isEmpty())
    }

    @Test
    fun overdue_respectsLookback() {
        val weekly = daily.copy(startDate = today.minusDays(40), recurrence = RecurrenceRule.Once)
        assertTrue(OccurrenceResolver.overdue(listOf(weekly), emptyList(), today).isEmpty())
    }

    @Test
    fun weeklySummary_countsScheduledAndDonePerDay() {
        val monday = LocalDate.of(2026, 10, 5)
        val states = listOf(OccurrenceState(1, monday, OccurrenceStatus.DONE), OccurrenceState(1, monday.plusDays(1), OccurrenceStatus.SKIPPED))
        val occ = OccurrenceResolver.resolve(listOf(daily), states, monday, monday.plusDays(6))
        val week = ObserveWeeklyCareUseCase.summarizeWeek(monday, occ)
        assertEquals(7, week.size)
        assertEquals(1, week[0].done)
        assertEquals(0, week[1].done) // skipped is not done
        assertEquals(7, week.sumOf { it.scheduled })
    }

    @Test
    fun greeting_byTimeOfDay() {
        assertEquals(Greeting.MORNING, greetingFor(LocalTime.of(6, 0)))
        assertEquals(Greeting.AFTERNOON, greetingFor(LocalTime.of(13, 0)))
        assertEquals(Greeting.EVENING, greetingFor(LocalTime.of(21, 0)))
        assertEquals(Greeting.EVENING, greetingFor(LocalTime.of(2, 0)))
    }
}
