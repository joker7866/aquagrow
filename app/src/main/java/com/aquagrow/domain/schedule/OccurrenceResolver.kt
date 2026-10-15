package com.aquagrow.domain.schedule

import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.OccurrenceState
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskOccurrence
import java.time.LocalDate

/**
 * Turns tasks + stored occurrence states into the occurrences shown to the user.
 * Each (taskId, originalDate) pair appears at most once, so nothing is ever duplicated,
 * even when an occurrence is rescheduled onto a day that has its own occurrence.
 */
object OccurrenceResolver {

    /** How far back unhandled occurrences still count as "overdue". */
    const val OVERDUE_LOOKBACK_DAYS = 14L

    /**
     * Occurrences whose current date lies in [from]..[to].
     * [states] must include every state whose original OR rescheduled date is in range.
     */
    fun resolve(
        tasks: List<CareTask>,
        states: List<OccurrenceState>,
        from: LocalDate,
        to: LocalDate,
    ): List<TaskOccurrence> {
        val stateByKey = states.associateBy { it.taskId to it.originalDate }
        val result = LinkedHashMap<Pair<Long, LocalDate>, TaskOccurrence>()

        for (task in tasks) {
            if (!task.enabled) continue
            for (date in RecurrenceEngine.occurrences(task.recurrence, task.startDate, task.endDate, from, to)) {
                val occ = merge(task, date, stateByKey[task.id to date])
                if (occ.date in from..to) result[task.id to date] = occ
            }
        }

        // Occurrences rescheduled INTO the range from a date outside it.
        val taskById = tasks.filter { it.enabled }.associateBy { it.id }
        for (state in states) {
            val moved = state.rescheduledDate ?: continue
            val key = state.taskId to state.originalDate
            if (key in result || moved !in from..to) continue
            val task = taskById[state.taskId] ?: continue
            result[key] = merge(task, state.originalDate, state)
        }

        return result.values.sortedWith(
            compareBy<TaskOccurrence> { it.date }
                .thenBy { it.task.reminderTime }
                .thenBy { it.task.title },
        )
    }

    /**
     * For each task, the most recent unhandled occurrence before [today] (within the lookback).
     * Only the latest one per task is returned, so a week of missed daily watering shows
     * as one overdue item instead of seven.
     */
    fun overdue(
        tasks: List<CareTask>,
        states: List<OccurrenceState>,
        today: LocalDate,
    ): List<TaskOccurrence> {
        val from = today.minusDays(OVERDUE_LOOKBACK_DAYS)
        val past = resolve(tasks, states, from, today.minusDays(1))
        val handledLater = resolve(tasks, states, from, today)
            .filter { it.isHandled }
            .groupBy { it.task.id }
            .mapValues { (_, v) -> v.maxOf { it.date } }

        return past
            .filter { it.isOverdue(today) }
            // Ignore misses older than something the user already handled for this task.
            .filter { occ -> handledLater[occ.task.id]?.let { occ.date.isAfter(it) } ?: true }
            .groupBy { it.task.id }
            .map { (_, list) -> list.maxBy { it.date } }
            .sortedBy { it.date }
    }

    private fun merge(task: CareTask, originalDate: LocalDate, state: OccurrenceState?) = TaskOccurrence(
        task = task,
        originalDate = originalDate,
        date = state?.rescheduledDate ?: originalDate,
        status = state?.status ?: OccurrenceStatus.PENDING,
        snoozedUntil = state?.snoozedUntil,
        handledAt = state?.handledAt,
    )
}
