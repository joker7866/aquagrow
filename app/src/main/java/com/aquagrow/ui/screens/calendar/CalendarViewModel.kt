package com.aquagrow.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.OccurrenceActions
import com.aquagrow.domain.usecase.isModuleOn
import com.aquagrow.ui.navigation.NavigationRequests
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CalendarMode { DAY, WEEK, MONTH }
enum class CalendarFilter { BOTH, GARDEN, AQUARIUM }

data class CalendarUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val mode: CalendarMode = CalendarMode.MONTH,
    val filter: CalendarFilter = CalendarFilter.BOTH,
    val selected: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.now(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    /** Occurrences in the visible grid, grouped by their current date. */
    val byDate: Map<LocalDate, List<TaskOccurrence>> = emptyMap(),
    val overdue: List<TaskOccurrence> = emptyList(),
    val gardenEnabled: Boolean = true,
    val aquariumEnabled: Boolean = true,
    /** Shown for tasks without their own time (they fire at the default reminder time). */
    val defaultTime: java.time.LocalTime = java.time.LocalTime.of(7, 0),
) {
    val selectedTasks: List<TaskOccurrence> get() = byDate[selected].orEmpty()

    /** First cell of the month grid (may belong to the previous month). */
    val gridStart: LocalDate get() = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val weekStart: LocalDate get() = selected.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val tasks: TaskRepository,
    prefs: UserPreferencesRepository,
    settings: com.aquagrow.domain.repository.SettingsRepository,
    private val actions: OccurrenceActions,
    private val navigationRequests: NavigationRequests,
    private val time: TimeProvider,
) : ViewModel() {

    private val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    private val today = time.today()
    private val controls = MutableStateFlow(
        CalendarUiState(today = today, selected = today, month = YearMonth.from(today), firstDayOfWeek = firstDay),
    )

    init {
        // A notification tap may ask for a specific day.
        viewModelScope.launch {
            navigationRequests.calendarDate.collect { d ->
                if (d != null) {
                    select(d)
                    navigationRequests.calendarDateConsumed()
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = controls.flatMapLatest { c ->
        // Grid of 6 weeks always covers the month, the selected week and the selected day.
        val from = minOf(c.gridStart, c.weekStart)
        val to = maxOf(c.gridStart.plusDays(41), c.weekStart.plusDays(6))
        combine(tasks.observeOccurrences(from, to), tasks.observeOverdue(c.today), prefs.preferences, settings.notificationSettings) { occ, overdue, p, ns ->
            fun keep(o: TaskOccurrence) = p.isModuleOn(o.task.module) && when (c.filter) {
                CalendarFilter.BOTH -> true
                CalendarFilter.GARDEN -> o.task.module == CareModule.GARDEN
                CalendarFilter.AQUARIUM -> o.task.module == CareModule.AQUARIUM
            }
            c.copy(
                loading = false,
                byDate = occ.filter(::keep).groupBy { it.date }
                    .mapValues { (_, l) -> l.sortedWith(compareBy({ it.task.reminderTime == null }, { it.task.reminderTime }, { it.task.title })) },
                overdue = overdue.filter(::keep),
                gardenEnabled = p.gardenEnabled,
                aquariumEnabled = p.aquariumEnabled,
                defaultTime = ns.defaultReminderTime,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), controls.value)

    fun setMode(m: CalendarMode) = controls.update { it.copy(mode = m) }
    fun setFilter(f: CalendarFilter) = controls.update { it.copy(filter = f) }
    fun select(d: LocalDate) = controls.update { it.copy(selected = d, month = YearMonth.from(d)) }
    fun showMonth(ym: YearMonth) = controls.update {
        it.copy(month = ym, selected = if (YearMonth.from(it.today) == ym) it.today else ym.atDay(1))
    }

    /** Previous / next step in the current mode. */
    fun step(forward: Boolean) {
        val c = controls.value
        val n = if (forward) 1L else -1L
        when (c.mode) {
            CalendarMode.MONTH -> showMonth(c.month.plusMonths(n))
            CalendarMode.WEEK -> select(c.selected.plusWeeks(n))
            CalendarMode.DAY -> select(c.selected.plusDays(n))
        }
    }

    fun toggleDone(o: TaskOccurrence) = viewModelScope.launch {
        if (o.isHandled) actions.undo(o.task.id, o.originalDate) else actions.markDone(o.task.id, o.originalDate)
    }

    fun skip(o: TaskOccurrence) = viewModelScope.launch { actions.skip(o.task.id, o.originalDate) }
    fun move(o: TaskOccurrence, d: LocalDate) = viewModelScope.launch { actions.reschedule(o.task.id, o.originalDate, d) }
}
