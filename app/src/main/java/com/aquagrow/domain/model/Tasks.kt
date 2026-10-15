package com.aquagrow.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * How a task repeats.
 * - ONCE: only on the start date.
 * - DAILY: every [interval] days ("every N days" is DAILY with interval N).
 * - WEEKLY: every [interval] weeks on [daysOfWeek] (empty = the start date's weekday).
 * - MONTHLY: every [interval] months on [dayOfMonth] (null = the start date's day),
 *   clamped to the month's length (31 → 30 in April, 28/29 in February).
 */
data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val dayOfMonth: Int? = null,
) {
    init {
        require(interval >= 1) { "interval must be >= 1" }
        require(dayOfMonth == null || dayOfMonth in 1..31) { "dayOfMonth must be 1..31" }
    }

    companion object {
        val Once = RecurrenceRule(RecurrenceType.ONCE)
        fun everyDays(n: Int) = RecurrenceRule(RecurrenceType.DAILY, interval = n)
        fun weekly(days: Set<DayOfWeek>, everyWeeks: Int = 1) = RecurrenceRule(RecurrenceType.WEEKLY, interval = everyWeeks, daysOfWeek = days)
        fun monthly(dayOfMonth: Int?, everyMonths: Int = 1) = RecurrenceRule(RecurrenceType.MONTHLY, interval = everyMonths, dayOfMonth = dayOfMonth)
    }
}

data class CareTask(
    val id: Long = 0,
    val title: String,
    val module: CareModule,
    val type: TaskType,
    val plantId: Long? = null,
    val aquariumId: Long? = null,
    val fishId: Long? = null,
    /** Display name of the linked plant / fish / tank, resolved by the repository. */
    val subjectName: String? = null,
    val maintenanceType: MaintenanceType? = null,
    val fertilizerType: FertilizerType? = null,
    /** Product or food name (fertilizer, fish food). */
    val itemName: String? = null,
    /** Free text the user typed, e.g. portion guidance. The app never suggests amounts. */
    val amountText: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    /** Null = use the default reminder time from settings. */
    val reminderTime: LocalTime? = null,
    val recurrence: RecurrenceRule = RecurrenceRule.Once,
    val enabled: Boolean = true,
    val notes: String? = null,
)

/** One computed occurrence of a task, merged with whatever the user did to it. */
data class TaskOccurrence(
    val task: CareTask,
    /** The date the recurrence rule produced; the stable key for this occurrence. */
    val originalDate: LocalDate,
    /** Where it currently sits on the calendar (differs if rescheduled). */
    val date: LocalDate,
    val status: OccurrenceStatus = OccurrenceStatus.PENDING,
    val snoozedUntil: Instant? = null,
    val handledAt: Instant? = null,
) {
    val isHandled: Boolean get() = status != OccurrenceStatus.PENDING
    fun isOverdue(today: LocalDate): Boolean = !isHandled && date.isBefore(today)
}

/** What the user did to one occurrence (stored; occurrences themselves are not). */
data class OccurrenceState(
    val taskId: Long,
    val originalDate: LocalDate,
    val status: OccurrenceStatus,
    val rescheduledDate: LocalDate? = null,
    val snoozedUntil: Instant? = null,
    val handledAt: Instant? = null,
)
