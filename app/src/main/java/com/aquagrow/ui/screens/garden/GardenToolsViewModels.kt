package com.aquagrow.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquagrow.domain.catalog.PlantCatalog
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.FertilizerLog
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.Season
import com.aquagrow.domain.model.SowingGuide
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WateringLog
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.AddFertilizerLogUseCase
import com.aquagrow.domain.usecase.OccurrenceActions
import com.aquagrow.domain.usecase.RecordWateringUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------- Watering ----------------

data class WateringUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val overdue: List<TaskOccurrence> = emptyList(),
    val dueToday: List<TaskOccurrence> = emptyList(),
    val upcoming: List<TaskOccurrence> = emptyList(),
    val history: List<Pair<WateringLog, String>> = emptyList(),
    val plants: List<Plant> = emptyList(),
)

@HiltViewModel
class WateringViewModel @Inject constructor(
    garden: GardenRepository,
    tasks: TaskRepository,
    private val recordWatering: RecordWateringUseCase,
    private val actions: OccurrenceActions,
    time: TimeProvider,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WateringUiState> = time.observeToday().flatMapLatest { today ->
        combine(
            tasks.observeOverdue(today),
            tasks.observeOccurrences(today, today.plusDays(7)),
            garden.observeRecentWatering(30),
            garden.observePlants(),
        ) { overdue, week, history, plants ->
            fun List<TaskOccurrence>.watering() = filter { it.task.type == TaskType.WATERING }
            val names = plants.associate { it.id to it.name }
            WateringUiState(
                loading = false,
                today = today,
                overdue = overdue.watering(),
                dueToday = week.watering().filter { it.date == today },
                upcoming = week.watering().filter { it.date.isAfter(today) && !it.isHandled },
                history = history.mapNotNull { log -> names[log.plantId]?.let { log to it } },
                plants = plants,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WateringUiState())

    fun record(plantId: Long, e: WateringEntry) = viewModelScope.launch {
        recordWatering(plantId, e.watered, e.moisture, e.amountMl, e.note)
    }

    fun undo(occ: TaskOccurrence) = viewModelScope.launch { actions.undo(occ.task.id, occ.originalDate) }

    fun move(occ: TaskOccurrence, date: LocalDate) = viewModelScope.launch {
        actions.reschedule(occ.task.id, occ.originalDate, date)
    }
}

// ---------------- Fertilizer ----------------

data class FertilizerUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val upcoming: List<TaskOccurrence> = emptyList(),
    val history: List<Pair<FertilizerLog, String>> = emptyList(),
    val plants: List<Plant> = emptyList(),
)

@HiltViewModel
class FertilizerViewModel @Inject constructor(
    garden: GardenRepository,
    tasks: TaskRepository,
    private val addFertilizer: AddFertilizerLogUseCase,
    private val actions: OccurrenceActions,
    private val time: TimeProvider,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FertilizerUiState> = time.observeToday().flatMapLatest { today ->
        combine(
            tasks.observeOverdue(today),
            tasks.observeOccurrences(today, today.plusDays(60)),
            garden.observeAllFertilizerLogs(),
            garden.observePlants(),
        ) { overdue, upcoming, logs, plants ->
            val names = plants.associate { it.id to it.name }
            FertilizerUiState(
                loading = false,
                today = today,
                upcoming = (overdue + upcoming.filter { !it.isHandled })
                    .filter { it.task.type == TaskType.FERTILIZING }
                    .distinctBy { it.task.id to it.originalDate },
                history = logs.mapNotNull { log -> names[log.plantId]?.let { log to it } },
                plants = plants,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FertilizerUiState())

    fun log(e: FertilizerEntry) = viewModelScope.launch {
        addFertilizer(
            FertilizerLog(
                plantId = e.plantId,
                appliedAt = time.now(),
                type = e.type,
                productName = e.product,
                quantity = e.quantity,
                unit = e.unit,
                method = e.method,
                note = e.note,
            ),
            e.nextDate,
        )
    }

    fun move(occ: TaskOccurrence, date: LocalDate) = viewModelScope.launch {
        actions.reschedule(occ.task.id, occ.originalDate, date)
    }
}

// ---------------- Planner ----------------

data class PlannerUiState(
    val loading: Boolean = true,
    val month: Int = 1,
    val season: Season = Season.KHARIF,
    val guides: List<SowingGuide> = emptyList(),
    val sowNow: List<SowingGuide> = emptyList(),
    val lastReminder: Pair<String, LocalDate>? = null,
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val garden: GardenRepository,
    private val tasks: TaskRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val season = MutableStateFlow(currentSeason(time.today().monthValue))
    private val lastReminder = MutableStateFlow<Pair<String, LocalDate>?>(null)

    val uiState: StateFlow<PlannerUiState> = combine(garden.observeSowingGuides(), season, lastReminder) { guides, s, reminded ->
        val month = time.today().monthValue
        PlannerUiState(
            loading = false,
            month = month,
            season = s,
            guides = guides.filter { it.season == s },
            sowNow = guides.filter { it.includesMonth(month) }.distinctBy { it.plantName },
            lastReminder = reminded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlannerUiState())

    fun selectSeason(s: Season) {
        season.value = s
    }

    fun save(guide: SowingGuide) = viewModelScope.launch {
        garden.saveSowingGuide(guide.copy(plantName = guide.plantName.trim(), notes = guide.notes?.trim()?.ifEmpty { null }, userEdited = true))
    }

    fun delete(guide: SowingGuide) = viewModelScope.launch { garden.deleteSowingGuide(guide) }

    /** Adds a one-off "Sow" task on the first day of the next window (or tomorrow if it is open now). */
    fun remind(guide: SowingGuide) = viewModelScope.launch {
        val today = time.today()
        val date = if (guide.includesMonth(today.monthValue)) {
            today.plusDays(1)
        } else {
            var ym = YearMonth.from(today).plusMonths(1)
            while (ym.monthValue != guide.startMonth) ym = ym.plusMonths(1)
            ym.atDay(1)
        }
        tasks.saveTask(
            CareTask(
                title = "Sow ${guide.plantName}",
                module = CareModule.GARDEN,
                type = TaskType.SOWING,
                startDate = date,
                recurrence = RecurrenceRule.Once,
                notes = PlantCatalog.find(guide.catalogKey)?.careTips?.firstOrNull(),
            ),
        )
        lastReminder.value = guide.plantName to date
    }

    companion object {
        /** Rough mapping of months to the Indian cropping season in progress. */
        fun currentSeason(month: Int): Season = when (month) {
            in 6..9 -> Season.KHARIF
            in 10..12, 1 -> Season.RABI
            else -> Season.ZAID
        }
    }
}
