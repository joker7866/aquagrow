package com.aquagrow.ui.screens.addplant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aquagrow.domain.catalog.CatalogPlant
import com.aquagrow.domain.catalog.PlantCatalog
import com.aquagrow.domain.garden.PlantFieldError
import com.aquagrow.domain.garden.PlantFormInput
import com.aquagrow.domain.garden.PlantValidator
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.PlantCondition
import com.aquagrow.domain.model.PlantingMethod
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.SunlightLevel
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.schedule.ScheduleKind
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.FertilizerPlan
import com.aquagrow.domain.usecase.SavePlantUseCase
import com.aquagrow.ui.navigation.AddPlantRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlantForm(
    val name: String = "",
    val variety: String = "",
    val catalogKey: String? = null,
    val category: PlantCategory = PlantCategory.VEGETABLE,
    val method: PlantingMethod? = null,
    val sowingDate: LocalDate? = null,
    val plantingDate: LocalDate,
    val potSize: String = "",
    val soilMix: String = "",
    val location: String = "",
    val sunlight: SunlightLevel? = null,
    val condition: PlantCondition? = null,
    val stages: List<String> = PlantCatalog.defaultStages,
    val currentStage: Int = 0,
    val harvestMin: String = "",
    val harvestMax: String = "",
    val notes: String = "",
    val photoPath: String? = null,
    val wateringOn: Boolean = true,
    val watering: ScheduleInput,
    val fertilizerOn: Boolean = false,
    val fertilizer: ScheduleInput,
    val fertType: FertilizerType = FertilizerType.COMPOST,
    val fertProduct: String = "",
    val fertAmount: String = "",
) {
    fun toInput() = PlantFormInput(
        name = name,
        plantingDate = plantingDate,
        sowingDate = sowingDate,
        harvestDaysMin = harvestMin.toIntOrNull(),
        harvestDaysMax = harvestMax.toIntOrNull(),
        stages = stages,
        watering = if (wateringOn) watering else null,
        fertilizer = if (fertilizerOn) fertilizer else null,
    )
}

data class AddPlantUiState(
    val isEdit: Boolean = false,
    val loading: Boolean = false,
    val form: PlantForm,
    val errors: Set<PlantFieldError> = emptySet(),
    /** Errors are only shown after the first save attempt. */
    val showErrors: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val savedId: Long? = null,
    val moreExpanded: Boolean = false,
    val notFound: Boolean = false,
)

@HiltViewModel
class AddPlantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val garden: GardenRepository,
    private val tasks: TaskRepository,
    private val savePlant: SavePlantUseCase,
    private val time: TimeProvider,
) : ViewModel() {

    private val plantId: Long? = savedStateHandle.toRoute<AddPlantRoute>().plantId
    private var original: Plant? = null

    private val today = time.today()
    private val _state = MutableStateFlow(
        AddPlantUiState(
            isEdit = plantId != null,
            loading = plantId != null,
            form = PlantForm(
                plantingDate = today,
                watering = ScheduleInput(kind = ScheduleKind.EVERY_N_DAYS, startDate = today),
                fertilizer = ScheduleInput(kind = ScheduleKind.EVERY_N_WEEKS, startDate = today.plusDays(7)),
            ),
        ),
    )
    val state: StateFlow<AddPlantUiState> = _state.asStateFlow()

    init {
        if (plantId != null) viewModelScope.launch { load(plantId) }
    }

    private suspend fun load(id: Long) {
        val plant = garden.getPlant(id)
        if (plant == null) {
            _state.update { it.copy(loading = false, notFound = true) }
            return
        }
        original = plant
        val plantTasks = tasks.getTasksForPlant(id)
        val water = plantTasks.firstOrNull { it.type == TaskType.WATERING }
        val fert = plantTasks.firstOrNull { it.type == TaskType.FERTILIZING && it.recurrence != RecurrenceRule.Once }
        _state.update { s ->
            s.copy(
                loading = false,
                form = s.form.copy(
                    name = plant.name,
                    variety = plant.variety.orEmpty(),
                    catalogKey = plant.catalogKey,
                    category = plant.category,
                    method = plant.plantingMethod,
                    sowingDate = plant.sowingDate,
                    plantingDate = plant.plantingDate,
                    potSize = plant.potSize.orEmpty(),
                    soilMix = plant.soilMix.orEmpty(),
                    location = plant.location.orEmpty(),
                    sunlight = plant.sunlight,
                    condition = plant.condition,
                    stages = plant.growthStages,
                    currentStage = plant.currentStageIndex,
                    harvestMin = plant.harvestDaysMin?.toString().orEmpty(),
                    harvestMax = plant.harvestDaysMax?.toString().orEmpty(),
                    notes = plant.notes.orEmpty(),
                    photoPath = plant.coverPhotoPath,
                    wateringOn = water != null,
                    watering = water?.let(ScheduleInput::fromTask) ?: s.form.watering,
                    fertilizerOn = fert != null,
                    fertilizer = fert?.let(ScheduleInput::fromTask) ?: s.form.fertilizer,
                    fertType = fert?.fertilizerType ?: FertilizerType.COMPOST,
                    fertProduct = fert?.itemName.orEmpty(),
                    fertAmount = fert?.amountText.orEmpty(),
                ),
                moreExpanded = plant.sowingDate != null || plant.soilMix != null || fert != null,
            )
        }
    }

    fun update(transform: (PlantForm) -> PlantForm) {
        _state.update { s ->
            val form = transform(s.form)
            s.copy(form = form, dirty = true, errors = if (s.showErrors) PlantValidator.validate(form.toInput(), today) else s.errors)
        }
    }

    /** Fills in typical values from the catalog; never overwrites a name the user typed. */
    fun pickCatalog(entry: CatalogPlant?) = update { f ->
        if (entry == null) {
            f.copy(catalogKey = null, stages = if (f.catalogKey != null) PlantCatalog.defaultStages else f.stages, harvestMin = "", harvestMax = "")
        } else {
            val previousName = PlantCatalog.find(f.catalogKey)?.name
            f.copy(
                catalogKey = entry.key,
                name = if (f.name.isBlank() || f.name == previousName) entry.name else f.name,
                category = entry.category,
                sunlight = entry.sunlight,
                method = entry.usualMethod,
                stages = entry.stages,
                currentStage = 0,
                harvestMin = entry.harvestDaysMin?.toString().orEmpty(),
                harvestMax = entry.harvestDaysMax?.toString().orEmpty(),
            )
        }
    }

    fun toggleMore() = _state.update { it.copy(moreExpanded = !it.moreExpanded) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        val errors = PlantValidator.validate(s.form.toInput(), today)
        if (errors.isNotEmpty()) {
            _state.update {
                it.copy(
                    errors = errors,
                    showErrors = true,
                    // Reveal hidden fields that have problems.
                    moreExpanded = it.moreExpanded || errors.any { e ->
                        e == PlantFieldError.SOWING_AFTER_PLANTING || e == PlantFieldError.HARVEST_RANGE_INVALID ||
                            e == PlantFieldError.STAGES_EMPTY || e == PlantFieldError.FERTILIZER_SCHEDULE_INVALID
                    },
                )
            }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val f = s.form
            val stages = f.stages.map { it.trim() }.filter { it.isNotEmpty() }
            val plant = Plant(
                id = plantId ?: 0,
                name = f.name.trim(),
                variety = f.variety.trim().ifEmpty { null },
                catalogKey = f.catalogKey,
                category = f.category,
                plantingMethod = f.method,
                sowingDate = f.sowingDate,
                plantingDate = f.plantingDate,
                potSize = f.potSize.trim().ifEmpty { null },
                soilMix = f.soilMix.trim().ifEmpty { null },
                location = f.location.trim().ifEmpty { null },
                sunlight = f.sunlight,
                condition = f.condition,
                growthStages = stages,
                currentStageIndex = f.currentStage.coerceIn(0, stages.lastIndex),
                harvestDaysMin = f.harvestMin.toIntOrNull(),
                harvestDaysMax = f.harvestMax.toIntOrNull() ?: f.harvestMin.toIntOrNull(),
                coverPhotoPath = f.photoPath,
                notes = f.notes.trim().ifEmpty { null },
                archived = original?.archived ?: false,
            )
            val id = savePlant(
                plant = plant,
                watering = if (f.wateringOn) f.watering else null,
                fertilizer = if (f.fertilizerOn) FertilizerPlan(f.fertilizer, f.fertType, f.fertProduct, f.fertAmount) else null,
            )
            _state.update { it.copy(saving = false, savedId = id, dirty = false) }
        }
    }
}
