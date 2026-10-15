package com.aquagrow.domain.usecase

import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.UserPreferences
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.time.TimeProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

data class TodayTasks(
    val today: LocalDate,
    /** Overdue first (oldest first), then today's in time order. */
    val items: List<TaskOccurrence>,
)

/** Today's list: overdue + due today, limited to the modules the user has switched on. */
class ObserveTodayTasksUseCase @Inject constructor(
    private val tasks: TaskRepository,
    private val prefs: UserPreferencesRepository,
    private val time: TimeProvider,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<TodayTasks> = time.observeToday().flatMapLatest { today ->
        combine(
            tasks.observeOverdue(today),
            tasks.observeOccurrences(today, today),
            prefs.preferences,
        ) { overdue, dueToday, p ->
            TodayTasks(today, (overdue + dueToday).filter { p.isModuleOn(it.task.module) })
        }
    }
}

fun UserPreferences.isModuleOn(module: CareModule): Boolean = when (module) {
    CareModule.GARDEN -> gardenEnabled
    CareModule.AQUARIUM -> aquariumEnabled
}

data class DayCare(val date: LocalDate, val scheduled: Int, val done: Int)

/** Rolling care summary of the last 7 days, ending today (mockup: Home chart). */
class ObserveWeeklyCareUseCase @Inject constructor(
    private val tasks: TaskRepository,
    private val prefs: UserPreferencesRepository,
    private val time: TimeProvider,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<DayCare>> = time.observeToday().flatMapLatest { today ->
        val start = today.minusDays(6)
        combine(tasks.observeOccurrences(start, today), prefs.preferences) { occ, p ->
            summarizeWeek(start, occ.filter { p.isModuleOn(it.task.module) })
        }
    }

    companion object {
        /** Pure function so it can be unit-tested without a database. */
        fun summarizeWeek(start: LocalDate, occurrences: List<TaskOccurrence>): List<DayCare> {
            val byDay = occurrences.groupBy { it.date }
            return (0L..6L).map { offset ->
                val day = start.plusDays(offset)
                val list = byDay[day].orEmpty()
                DayCare(day, scheduled = list.size, done = list.count { it.status == OccurrenceStatus.DONE })
            }
        }
    }
}
