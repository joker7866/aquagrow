package com.aquagrow.ui.screens.tankdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aquagrow.domain.aquarium.FeedingPlan
import com.aquagrow.domain.aquarium.ParameterCheck
import com.aquagrow.domain.aquarium.WaterRangeChecker
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.FeedingLog
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.InventoryCategory
import com.aquagrow.domain.model.InventoryItem
import com.aquagrow.domain.model.MaintenanceLog
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterParameter
import com.aquagrow.domain.model.WaterRanges
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.InventoryRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.LogFeedingUseCase
import com.aquagrow.domain.usecase.LogMaintenanceUseCase
import com.aquagrow.domain.usecase.LogWaterTestUseCase
import com.aquagrow.domain.usecase.OccurrenceActions
import com.aquagrow.domain.usecase.SaveFeedingScheduleUseCase
import com.aquagrow.domain.usecase.SaveTankScheduleUseCase
import com.aquagrow.ui.components.taskTitle
import com.aquagrow.ui.navigation.TankDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A schedule row (maintenance / water test / feeding) with its next due occurrence and last completion. */
data class TankScheduleUi(val task: CareTask, val next: TaskOccurrence?, val lastDone: Instant?)

sealed interface TankDetailUiState {
    data object Loading : TankDetailUiState
    data object NotFound : TankDetailUiState
    data class Ready(
        val aquarium: Aquarium,
        val today: LocalDate,
        val fish: List<Fish>,
        val latestTest: WaterTest?,
        val checks: List<ParameterCheck>,
        val tests: List<WaterTest>,
        val feeding: List<TankScheduleUi>,
        val feedingToday: List<TaskOccurrence>,
        val maintenance: List<TankScheduleUi>,
        val feedingLogs: List<FeedingLog>,
        val maintenanceLogs: List<MaintenanceLog>,
        val equipment: List<InventoryItem>,
    ) : TankDetailUiState
}

@HiltViewModel
class TankDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AquariumRepository,
    private val tasks: TaskRepository,
    private val inventory: InventoryRepository,
    private val logWaterTest: LogWaterTestUseCase,
    private val logMaintenance: LogMaintenanceUseCase,
    private val logFeeding: LogFeedingUseCase,
    private val saveFeeding: SaveFeedingScheduleUseCase,
    private val saveSchedule: SaveTankScheduleUseCase,
    private val actions: OccurrenceActions,
    private val time: TimeProvider,
) : ViewModel() {

    val aquariumId: Long = savedStateHandle.toRoute<TankDetailRoute>().aquariumId
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    private val logs = combine(
        repo.observeWaterTests(aquariumId),
        repo.observeFeedingLogs(aquariumId),
        repo.observeMaintenanceLogs(aquariumId),
        inventory.observeEquipment(aquariumId),
    ) { tests, feeding, maint, equipment -> Logs(tests, feeding, maint, equipment) }

    private data class Logs(val tests: List<WaterTest>, val feeding: List<FeedingLog>, val maintenance: List<MaintenanceLog>, val equipment: List<InventoryItem>)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val schedules = time.observeToday().flatMapLatest { today ->
        combine(
            tasks.observeTasksForAquarium(aquariumId),
            tasks.observeOccurrences(today, today.plusDays(120)),
            tasks.observeOverdue(today),
        ) { list, upcoming, overdue -> Triple(today, list.filter { it.enabled }, upcoming to overdue) }
    }

    val uiState: StateFlow<TankDetailUiState> = combine(
        repo.observeAquarium(aquariumId),
        repo.observeFish(aquariumId),
        logs,
        schedules,
    ) { tank, fish, l, (today, taskList, occ) ->
        if (tank == null) return@combine TankDetailUiState.NotFound
        val (upcoming, overdue) = occ
        fun next(t: CareTask) = overdue.firstOrNull { it.task.id == t.id } ?: upcoming.firstOrNull { it.task.id == t.id && !it.isHandled }
        val lastFeedByTask = l.feeding.filter { !it.skipped }.groupBy { it.taskId }.mapValues { (_, v) -> v.maxOf { it.fedAt } }
        val lastMaint = l.maintenance.groupBy { it.type }.mapValues { (_, v) -> v.maxOf { it.performedAt } }
        val lastTest = l.tests.maxOfOrNull { it.testedAt }
        val latest = l.tests.maxByOrNull { it.testedAt }
        TankDetailUiState.Ready(
            aquarium = tank,
            today = today,
            fish = fish,
            latestTest = latest,
            checks = latest?.let { WaterRangeChecker.checkAll(it, tank.ranges) }.orEmpty(),
            tests = l.tests.sortedByDescending { it.testedAt },
            feeding = taskList.filter { it.type == TaskType.FEEDING }
                .sortedWith(compareBy({ it.fishId ?: 0 }, { it.reminderTime }))
                .map { TankScheduleUi(it, next(it), lastFeedByTask[it.id]) },
            feedingToday = upcoming.filter { it.task.aquariumId == aquariumId && it.task.type == TaskType.FEEDING && it.date == today },
            maintenance = taskList.filter { it.type == TaskType.MAINTENANCE || it.type == TaskType.WATER_TEST }
                .map { t -> TankScheduleUi(t, next(t), if (t.type == TaskType.WATER_TEST) lastTest else lastMaint[t.maintenanceType]) },
            feedingLogs = l.feeding.sortedByDescending { it.fedAt },
            maintenanceLogs = l.maintenance.sortedByDescending { it.performedAt },
            equipment = l.equipment,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TankDetailUiState.Loading)

    private val ready get() = uiState.value as? TankDetailUiState.Ready

    fun onLogWaterTest(test: WaterTest) = viewModelScope.launch { logWaterTest(test.copy(aquariumId = aquariumId)) }

    fun onSaveRanges(ranges: WaterRanges) {
        val tank = ready?.aquarium ?: return
        viewModelScope.launch { repo.saveAquarium(tank.copy(ranges = ranges)) }
    }

    fun onSaveFeeding(plan: FeedingPlan) = viewModelScope.launch { saveFeeding(aquariumId, plan) }
    fun onRemoveFeeding(fishId: Long?) = viewModelScope.launch { saveFeeding.clear(aquariumId, fishId) }

    fun onLogFeeding(fishId: Long?, food: String?, amount: String?, note: String?) = viewModelScope.launch {
        logFeeding(FeedingLog(aquariumId = aquariumId, fishId = fishId, fedAt = time.now(), foodName = food, amount = amount, note = note))
    }

    fun onSkipFeeding(occ: TaskOccurrence) = viewModelScope.launch { actions.skip(occ.task.id, occ.originalDate) }
    fun onDoneOccurrence(occ: TaskOccurrence) = viewModelScope.launch { actions.markDone(occ.task.id, occ.originalDate) }
    fun onUndoOccurrence(occ: TaskOccurrence) = viewModelScope.launch { actions.undo(occ.task.id, occ.originalDate) }

    /** [maintenanceType] null + [waterTest] true = water-testing schedule. */
    fun onSaveMaintenanceSchedule(maintenanceType: MaintenanceType?, waterTest: Boolean, schedule: ScheduleInput?) = viewModelScope.launch {
        if (waterTest) {
            saveSchedule(aquariumId, TaskType.WATER_TEST, null, "Test water", schedule)
        } else {
            val type = maintenanceType ?: MaintenanceType.OTHER
            saveSchedule(aquariumId, TaskType.MAINTENANCE, type, type.taskTitle(), schedule)
        }
    }

    fun onLogMaintenance(type: MaintenanceType, percent: Int?, note: String?) = viewModelScope.launch {
        logMaintenance(MaintenanceLog(aquariumId = aquariumId, performedAt = time.now(), type = type, waterChangePercent = percent, note = note))
    }

    fun onAddEquipment(name: String, category: InventoryCategory, purchaseDate: LocalDate?, notes: String?) = viewModelScope.launch {
        inventory.saveItem(
            InventoryItem(module = CareModule.AQUARIUM, name = name, category = category, purchaseDate = purchaseDate, aquariumId = aquariumId, notes = notes),
        )
    }

    fun onRemoveEquipment(item: InventoryItem) = viewModelScope.launch { inventory.deleteItem(item.id) }

    fun onArchive() {
        val tank = ready?.aquarium ?: return
        viewModelScope.launch {
            repo.saveAquarium(tank.copy(archived = true))
            _closed.value = true
        }
    }

    fun onDelete() = viewModelScope.launch {
        repo.deleteAquarium(aquariumId)
        _closed.value = true
    }

    companion object {
        val CHART_PARAMS = listOf(WaterParameter.NITRATE, WaterParameter.AMMONIA, WaterParameter.NITRITE, WaterParameter.PH, WaterParameter.TEMPERATURE, WaterParameter.GH, WaterParameter.KH)
    }
}
