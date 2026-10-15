package com.aquagrow.domain.schedule

import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** The choices offered by the schedule picker. */
enum class ScheduleKind { ONCE, DAILY, EVERY_N_DAYS, WEEKLY, EVERY_N_WEEKS, MONTHLY }

/**
 * Form-level description of a schedule, shared by every screen that schedules something
 * (watering, fertilizer, feeding, maintenance, custom tasks). [interval] is null until the
 * user picks one: the app never fills in a "recommended" interval.
 */
data class ScheduleInput(
    val kind: ScheduleKind = ScheduleKind.EVERY_N_DAYS,
    val interval: Int? = null,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val startDate: LocalDate,
    val time: LocalTime? = null,
) {
    val needsInterval: Boolean
        get() = kind == ScheduleKind.EVERY_N_DAYS || kind == ScheduleKind.EVERY_N_WEEKS || kind == ScheduleKind.MONTHLY

    /** Null when valid, otherwise why not. */
    fun validate(): ScheduleError? = when {
        needsInterval && interval == null -> ScheduleError.MISSING_INTERVAL
        needsInterval && interval !in 1..MAX_INTERVAL -> ScheduleError.INTERVAL_RANGE
        kind == ScheduleKind.WEEKLY && daysOfWeek.isEmpty() -> ScheduleError.NO_DAYS
        else -> null
    }

    fun toRule(): RecurrenceRule = when (kind) {
        ScheduleKind.ONCE -> RecurrenceRule.Once
        ScheduleKind.DAILY -> RecurrenceRule.everyDays(1)
        ScheduleKind.EVERY_N_DAYS -> RecurrenceRule.everyDays(interval ?: 1)
        ScheduleKind.WEEKLY -> RecurrenceRule.weekly(daysOfWeek)
        ScheduleKind.EVERY_N_WEEKS -> RecurrenceRule.weekly(daysOfWeek.ifEmpty { setOf(startDate.dayOfWeek) }, interval ?: 1)
        ScheduleKind.MONTHLY -> RecurrenceRule.monthly(startDate.dayOfMonth, interval ?: 1)
    }

    companion object {
        const val MAX_INTERVAL = 90

        fun fromTask(task: CareTask): ScheduleInput {
            val r = task.recurrence
            val kind = when (r.type) {
                RecurrenceType.ONCE -> ScheduleKind.ONCE
                RecurrenceType.DAILY -> if (r.interval == 1) ScheduleKind.DAILY else ScheduleKind.EVERY_N_DAYS
                RecurrenceType.WEEKLY -> if (r.interval == 1) ScheduleKind.WEEKLY else ScheduleKind.EVERY_N_WEEKS
                RecurrenceType.MONTHLY -> ScheduleKind.MONTHLY
            }
            return ScheduleInput(kind, r.interval, r.daysOfWeek, task.startDate, task.reminderTime)
        }
    }
}

enum class ScheduleError { MISSING_INTERVAL, INTERVAL_RANGE, NO_DAYS }
