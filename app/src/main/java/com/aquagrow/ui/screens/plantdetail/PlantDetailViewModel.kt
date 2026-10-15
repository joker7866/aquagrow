package com.aquagrow.ui.screens.plantdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aquagrow.domain.catalog.CatalogPlant
import com.aquagrow.domain.catalog.PlantCatalog
import com.aquagrow.domain.garden.HarvestEstimate
import com.aquagrow.domain.garden.HarvestEstimator
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.FertilizerLog
import com.aquagrow.domain.model.GrowthLog
import com.aquagrow.domain.model.HarvestLog
import com.aquagrow.domain.model.Photo
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.WateringLog
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.PhotoRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.AddFertilizerLogUseCase
import com.aquagrow.domain.usecase.AddGrowthLogUseCase
import com.aquagrow.domain.usecase.RecordWateringUseCase
import com.aquagrow.domain.usecase.SetGrowthStageUseCase
import com.aquagrow.ui.navigation.PlantDetailRoute
import com.aquagrow.ui.screens.garden.FertilizerEntry
import com.aquagrow.ui.screens.garden.GrowthEntry
import com.aquagrow.ui.screens.garden.HarvestEntry
import com.aquagrow.ui.screens.garden.WateringEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

sealed interface TimelineEntry {
    val at: Instant
    data class Growth(val log: GrowthLog) : TimelineEntry {
        override val at get() = log.loggedAt
    }
    data class Watering(val log: WateringLog) : TimelineEntry {
        override val at get() = log.loggedAt
    }
    data class Fertilizer(val log: FertilizerLog) : TimelineEntry {
        override val at get() = log.appliedAt
    }
    data class Harvest(val log: HarvestLog) : TimelineEntry {
        override val at get() = log.harvestedAt
    }
}

data class ScheduleUi(val task: CareTask, val next: TaskOccurrence?)

sealed interface PlantDetailUiState {
    data object Loading : PlantDetailUiState
    data object NotFound : PlantDetailUiState
    data class Ready(
        val plant: Plant,
        val today: LocalDate,
        val catalog: CatalogPlant?,
        val harvestEstimate: HarvestEstimate?,
        val schedules: List<ScheduleUi>,
        val timeline: List<TimelineEntry>,
        val photos: List<Photo>,
        val harvests: List<HarvestLog>,
        /** Total per unit, e.g. {"g": 850.0, "pcs": 12.0}. */
        val harvestTotals: Map<String, Double>,
    ) : PlantDetailUiState
}

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val garden: GardenRepository,
    private val tasks: TaskRepository,
    private val photos: PhotoRepository,
    private val recordWatering: RecordWateringUseCase,
    private val addGrowthLog: AddGrowthLogUseCase,
    private val setStage: SetGrowthStageUseCase,
    private val addFertilizer: AddFertilizerLogUseCase,
    private val time: TimeProvider,
) : ViewModel() {

    val plantId: Long = savedStateHandle.toRoute<PlantDetailRoute>().plantId

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    private val logs = combine(
        garden.observeGrowthLogs(plantId),
        garden.observeWateringLogs(plantId),
        garden.observeFertilizerLogs(plantId),
        garden.observeHarvestLogs(plantId),
    ) { g, w, f, h ->
        val timeline = (g.map(TimelineEntry::Growth) + w.map(TimelineEntry::Watering) + f.map(TimelineEntry::Fertilizer) + h.map(TimelineEntry::Harvest))
            .sortedByDescending { it.at }
        timeline to h
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val schedules = time.observeToday().flatMapLatest { today ->
        combine(
            tasks.observeTasksForPlant(plantId),
            tasks.observeOccurrences(today, today.plusDays(120)),
            tasks.observeOverdue(today),
        ) { list, upcoming, overdue ->
            today to list.filter { it.enabled }.map { task ->
                val next = overdue.firstOrNull { it.task.id == task.id }
                    ?: upcoming.firstOrNull { it.task.id == task.id && !it.isHandled }
                ScheduleUi(task, next)
            }
        }
    }

    val uiState: StateFlow<PlantDetailUiState> = combine(
        garden.observePlant(plantId),
        logs,
        schedules,
        photos.observeForPlant(plantId),
    ) { plant, (timeline, harvests), (today, sched), pics ->
        if (plant == null) {
            PlantDetailUiState.NotFound
        } else {
            PlantDetailUiState.Ready(
                plant = plant,
                today = today,
                catalog = PlantCatalog.find(plant.catalogKey),
                harvestEstimate = HarvestEstimator.estimate(plant.plantingDate, plant.harvestDaysMin, plant.harvestDaysMax, today),
                schedules = sched,
                timeline = timeline,
                photos = pics,
                harvests = harvests,
                harvestTotals = harvests.groupBy { it.unit }.mapValues { (_, l) -> l.sumOf { it.quantity } },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantDetailUiState.Loading)

    private val readyPlant: Plant? get() = (uiState.value as? PlantDetailUiState.Ready)?.plant

    fun onSetStage(index: Int) {
        val plant = readyPlant ?: return
        viewModelScope.launch { setStage(plant, index) }
    }

    fun onRecordWatering(e: WateringEntry) = viewModelScope.launch {
        recordWatering(plantId, e.watered, e.moisture, e.amountMl, e.note)
    }

    fun onAddGrowthLog(e: GrowthEntry) {
        val plant = readyPlant ?: return
        viewModelScope.launch { addGrowthLog(plant, e.stageIndex, e.heightCm, e.condition, e.note, e.photoPath) }
    }

    fun onAddFertilizer(e: FertilizerEntry) = viewModelScope.launch {
        addFertilizer(
            FertilizerLog(
                plantId = plantId,
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

    fun onAddHarvest(e: HarvestEntry) = viewModelScope.launch {
        // A past date is stored at midday so it never slips to a neighbouring day across zones.
        val at = if (e.date == time.today()) time.now() else e.date.atTime(LocalTime.NOON).atZone(time.zone()).toInstant()
        garden.addHarvestLog(HarvestLog(plantId = plantId, harvestedAt = at, quantity = e.quantity, unit = e.unit, note = e.note))
    }

    fun onAddPhoto(path: String) = viewModelScope.launch {
        photos.addPhoto(Photo(filePath = path, takenAt = time.now(), plantId = plantId))
        val plant = readyPlant
        if (plant != null && plant.coverPhotoPath == null) garden.savePlant(plant.copy(coverPhotoPath = path))
    }

    fun onSetCover(path: String) {
        val plant = readyPlant ?: return
        viewModelScope.launch { garden.savePlant(plant.copy(coverPhotoPath = path)) }
    }

    fun onDeletePhoto(photo: Photo) = viewModelScope.launch {
        val plant = readyPlant
        photos.deletePhoto(photo)
        if (plant != null && plant.coverPhotoPath == photo.filePath) garden.savePlant(plant.copy(coverPhotoPath = null))
    }

    fun onArchive() {
        val plant = readyPlant ?: return
        viewModelScope.launch {
            garden.savePlant(plant.copy(archived = true))
            _deleted.value = true
        }
    }

    fun onDelete() = viewModelScope.launch {
        garden.deletePlant(plantId)
        _deleted.value = true
    }
}
