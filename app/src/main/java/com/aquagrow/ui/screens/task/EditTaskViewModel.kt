package com.aquagrow.ui.screens.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.schedule.ScheduleKind
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.ui.navigation.EditTaskRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TaskField { TITLE, SCHEDULE, END }

data class TaskForm(
    val title: String = "",
    val module: CareModule = CareModule.GARDEN,
    val type: TaskType = TaskType.CUSTOM,
    val plantId: Long? = null,
    val aquariumId: Long? = null,
    val schedule: ScheduleInput,
    val endDate: LocalDate? = null,
    val notes: String = "",
) {
    fun errors(): Set<TaskField> = buildSet {
        if (title.isBlank()) add(TaskField.TITLE)
        if (schedule.validate() != null) add(TaskField.SCHEDULE)
        if (endDate != null && endDate.isBefore(schedule.startDate)) add(TaskField.END)
    }
}

data class EditTaskUiState(
    val isEdit: Boolean,
    val form: TaskForm,
    val plants: List<Plant> = emptyList(),
    val tanks: List<Aquarium> = emptyList(),
    val modules: List<CareModule> = CareModule.entries,
    val showErrors: Boolean = false,
    val dirty: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class EditTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    garden: GardenRepository,
    aquarium: AquariumRepository,
    prefs: UserPreferencesRepository,
    time: TimeProvider,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<EditTaskRoute>()
    private var original: CareTask? = null
    private val start = route.dateEpochDay?.let(LocalDate::ofEpochDay) ?: time.today()
    private val local = MutableStateFlow(
        EditTaskUiState(isEdit = route.taskId != null, form = TaskForm(schedule = ScheduleInput(kind = ScheduleKind.ONCE, startDate = start))),
    )

    init {
        route.taskId?.let { id ->
            viewModelScope.launch {
                val t = tasks.getTask(id) ?: return@launch
                original = t
                local.update {
                    it.copy(
                        form = TaskForm(
                            title = t.title,
                            module = t.module,
                            type = t.type,
                            plantId = t.plantId,
                            aquariumId = t.aquariumId,
                            schedule = ScheduleInput.fromTask(t),
                            endDate = t.endDate,
                            notes = t.notes.orEmpty(),
                        ),
                    )
                }
            }
        }
    }

    val state: StateFlow<EditTaskUiState> = combine(local, garden.observePlants(), aquarium.observeAquariums(), prefs.preferences) { s, plants, tanks, p ->
        val modules = CareModule.entries.filter { m -> if (m == CareModule.GARDEN) p.gardenEnabled else p.aquariumEnabled }
        val form = if (s.form.module !in modules && modules.isNotEmpty() && !s.isEdit) s.form.copy(module = modules.first()) else s.form
        s.copy(form = form, plants = plants, tanks = tanks, modules = modules)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    fun update(transform: (TaskForm) -> TaskForm) = local.update { it.copy(form = transform(state.value.form), dirty = true) }

    fun save() {
        val s = state.value
        if (s.form.errors().isNotEmpty()) {
            local.update { it.copy(showErrors = true) }
            return
        }
        val f = s.form
        viewModelScope.launch {
            tasks.saveTask(
                CareTask(
                    id = original?.id ?: 0,
                    title = f.title.trim(),
                    module = f.module,
                    type = f.type,
                    plantId = f.plantId.takeIf { f.module == CareModule.GARDEN },
                    aquariumId = f.aquariumId.takeIf { f.module == CareModule.AQUARIUM },
                    fishId = original?.fishId,
                    maintenanceType = original?.maintenanceType,
                    fertilizerType = original?.fertilizerType,
                    itemName = original?.itemName,
                    amountText = original?.amountText,
                    startDate = f.schedule.startDate,
                    endDate = f.endDate,
                    reminderTime = f.schedule.time,
                    recurrence = f.schedule.toRule(),
                    notes = f.notes.trim().ifEmpty { null },
                ),
            )
            local.update { it.copy(done = true, dirty = false) }
        }
    }

    fun delete() {
        val id = original?.id ?: return
        viewModelScope.launch {
            tasks.deleteTask(id)
            local.update { it.copy(done = true, dirty = false) }
        }
    }
}
