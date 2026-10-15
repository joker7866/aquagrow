package com.aquagrow.domain.reminders

import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.NotificationSettings
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.usecase.OccurrenceActions
import com.aquagrow.testing.FakeAquariumRepository
import com.aquagrow.testing.FakeGardenRepository
import com.aquagrow.testing.FakeTaskRepository
import com.aquagrow.testing.FakeTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    private val start = LocalTime.of(22, 0)
    private val end = LocalTime.of(6, 0)
    private val day = LocalDate.of(2026, 10, 8)

    @Test fun outsideIsUnchanged() {
        val t = day.atTime(9, 0)
        assertEquals(t, QuietHours.shift(t, start, end))
    }

    @Test fun lateEveningMovesToNextMorning() = assertEquals(day.plusDays(1).atTime(6, 0), QuietHours.shift(day.atTime(23, 30), start, end))

    @Test fun earlyMorningMovesToSameMorning() = assertEquals(day.atTime(6, 0), QuietHours.shift(day.atTime(5, 0), start, end))

    @Test fun boundaries() {
        assertTrue(QuietHours.contains(LocalTime.of(22, 0), start, end))
        assertFalse(QuietHours.contains(LocalTime.of(6, 0), start, end))
    }

    @Test fun sameDayWindow() {
        val s = LocalTime.of(13, 0)
        val e = LocalTime.of(15, 0)
        assertEquals(day.atTime(15, 0), QuietHours.shift(day.atTime(14, 0), s, e))
        assertEquals(day.atTime(16, 0), QuietHours.shift(day.atTime(16, 0), s, e))
    }

    @Test fun emptyWindowNeverShifts() = assertFalse(QuietHours.contains(LocalTime.NOON, LocalTime.NOON, LocalTime.NOON))
}

class ReminderPlannerTest {
    private val time = FakeTime(LocalDate.of(2026, 10, 8), LocalTime.of(6, 30))
    private val today = time.today
    private val settings = NotificationSettings(defaultReminderTime = LocalTime.of(7, 0), quietHoursStart = LocalTime.of(22, 0), quietHoursEnd = LocalTime.of(6, 0))

    private fun task(id: Long, type: TaskType = TaskType.WATERING, at: LocalTime? = null, maint: MaintenanceType? = null) = CareTask(id = id, title = "T$id", module = CareModule.GARDEN, type = type, maintenanceType = maint, startDate = today, reminderTime = at, recurrence = RecurrenceRule.everyDays(1))

    private fun occ(t: CareTask, date: LocalDate = today, status: OccurrenceStatus = OccurrenceStatus.PENDING, snooze: java.time.Instant? = null) = TaskOccurrence(t, date, date, status, snooze)

    private fun at(dt: LocalDateTime) = dt.toInstant(ZoneOffset.UTC)

    @Test fun usesDefaultTimeWhenTaskHasNone() {
        val plan = ReminderPlanner.plan(listOf(occ(task(1))), settings, time.now(), time.zone())
        assertEquals(at(today.atTime(7, 0)), plan.single().triggerAt)
    }

    @Test fun taskTimeInQuietHoursIsDelayedNotDropped() {
        val plan = ReminderPlanner.plan(listOf(occ(task(1, at = LocalTime.of(23, 0)))), settings, time.now(), time.zone())
        assertEquals(at(today.plusDays(1).atTime(6, 0)), plan.single().triggerAt)
    }

    @Test fun snoozeOverridesTime() {
        val snooze = at(today.atTime(9, 15))
        val plan = ReminderPlanner.plan(listOf(occ(task(1), snooze = snooze)), settings, time.now(), time.zone())
        assertEquals(snooze, plan.single().triggerAt)
    }

    @Test fun handledAndPastAreSkipped() {
        val done = occ(task(1), status = OccurrenceStatus.DONE)
        val past = occ(task(2, at = LocalTime.of(5, 0)), date = today.minusDays(1))
        assertTrue(ReminderPlanner.plan(listOf(done, past), settings, time.now(), time.zone()).isEmpty())
    }

    @Test fun disabledChannelIsSkipped() {
        val s = settings.copy(disabledChannels = setOf(ReminderChannel.WATER_CHANGE.name))
        val plan = ReminderPlanner.plan(
            listOf(occ(task(1, TaskType.MAINTENANCE, maint = MaintenanceType.WATER_CHANGE)), occ(task(2, TaskType.FEEDING))),
            s,
            time.now(),
            time.zone(),
        )
        assertEquals(listOf(2L), plan.map { it.taskId })
    }

    @Test fun remindersOffPlansNothing() = assertTrue(ReminderPlanner.plan(listOf(occ(task(1))), settings.copy(remindersEnabled = false), time.now(), time.zone()).isEmpty())

    @Test fun duplicateOccurrencesCollapseToOneKey() {
        val t = task(1)
        val plan = ReminderPlanner.plan(listOf(occ(t), occ(t)), settings, time.now(), time.zone())
        assertEquals(1, plan.size)
        assertEquals("rem-1-$today", plan.single().key)
    }

    @Test fun channelMapping() {
        assertEquals(ReminderChannel.FILTER, ReminderChannel.forTask(TaskType.MAINTENANCE, MaintenanceType.FILTER_MAINTENANCE))
        assertEquals(ReminderChannel.TESTING, ReminderChannel.forTask(TaskType.WATER_TEST, null))
        assertEquals(ReminderChannel.SOWING, ReminderChannel.forTask(TaskType.SOWING, null))
    }
}

class DuplicatePreventionTest {
    private val time = FakeTime()
    private val tasks = FakeTaskRepository()
    private val garden = FakeGardenRepository()
    private val aquarium = FakeAquariumRepository()
    private val actions = OccurrenceActions(tasks, garden, aquarium, time)

    @Test fun markingDoneTwiceWritesOneLog() = runTest {
        val id = tasks.saveTask(CareTask(title = "Water", module = CareModule.GARDEN, type = TaskType.WATERING, plantId = 5, startDate = time.today, recurrence = RecurrenceRule.everyDays(1)))
        assertTrue(actions.markDone(id, time.today))
        assertFalse(actions.markDone(id, time.today))
        assertEquals(1, garden.watering.value.size)
        assertEquals(1, tasks.states.value.size)
    }

    @Test fun undoRemovesExactlyTheLogItCreated() = runTest {
        val id = tasks.saveTask(CareTask(title = "Feed", module = CareModule.AQUARIUM, type = TaskType.FEEDING, aquariumId = 2, startDate = time.today, recurrence = RecurrenceRule.everyDays(1)))
        actions.markDone(id, time.today)
        assertEquals(1, aquarium.feeding.value.size)
        actions.undo(id, time.today)
        assertTrue(aquarium.feeding.value.isEmpty())
        assertTrue(tasks.states.value.isEmpty())
    }

    @Test fun rescheduleThenDoneKeepsSingleState() = runTest {
        val id = tasks.saveTask(CareTask(title = "Water", module = CareModule.GARDEN, type = TaskType.WATERING, plantId = 5, startDate = time.today, recurrence = RecurrenceRule.everyDays(1)))
        actions.reschedule(id, time.today, time.today.plusDays(1))
        actions.markDone(id, time.today)
        val state = tasks.states.value.single()
        assertEquals(OccurrenceStatus.DONE, state.status)
        assertEquals(time.today.plusDays(1), state.rescheduledDate)
    }
}
