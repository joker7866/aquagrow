package com.aquagrow.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.DayCare
import com.aquagrow.domain.usecase.ObserveTodayTasksUseCase
import com.aquagrow.domain.usecase.ObserveWeeklyCareUseCase
import com.aquagrow.domain.usecase.OccurrenceActions
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Greeting { MORNING, AFTERNOON, EVENING }

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** No plants, no tanks and no tasks yet. */
    data class Empty(val gardenEnabled: Boolean, val aquariumEnabled: Boolean) : HomeUiState

    data class Dashboard(
        val today: LocalDate,
        val greeting: Greeting,
        val gardenEnabled: Boolean,
        val aquariumEnabled: Boolean,
        val plantCount: Int,
        /** Plants not yet at their last growth stage. */
        val growingCount: Int = 0,
        val fishCount: Int,
        val tankCount: Int,
        val tasks: List<TaskOccurrence>,
        val week: List<DayCare>,
    ) : HomeUiState {
        val tasksLeft: Int get() = tasks.count { !it.isHandled }
        val weekDone: Int get() = week.sumOf { it.done }
        val weekScheduled: Int get() = week.sumOf { it.scheduled }
    }
}

fun greetingFor(time: LocalTime): Greeting = when (time.hour) {
    in 5..11 -> Greeting.MORNING
    in 12..16 -> Greeting.AFTERNOON
    else -> Greeting.EVENING
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    garden: GardenRepository,
    aquarium: AquariumRepository,
    prefs: UserPreferencesRepository,
    observeToday: ObserveTodayTasksUseCase,
    observeWeek: ObserveWeeklyCareUseCase,
    private val actions: OccurrenceActions,
    private val time: TimeProvider,
) : ViewModel() {

    private val counts = combine(
        garden.observePlants(),
        aquarium.observeActiveCount(),
        aquarium.observeTotalFishCount(),
    ) { plants, tanks, fish -> Counts(plants.size, plants.count { it.currentStageIndex < it.growthStages.lastIndex }, tanks, fish) }

    private data class Counts(val plants: Int, val growing: Int, val tanks: Int, val fish: Int)

    val uiState: StateFlow<HomeUiState> = combine(
        prefs.preferences,
        counts,
        observeToday(),
        observeWeek(),
    ) { p, c, today, week ->
        val plants = c.plants
        val tanks = c.tanks
        val fish = c.fish
        val nothingYet = plants == 0 && tanks == 0 && week.all { it.scheduled == 0 } && today.items.isEmpty()
        if (nothingYet) {
            HomeUiState.Empty(p.gardenEnabled, p.aquariumEnabled)
        } else {
            HomeUiState.Dashboard(
                today = today.today,
                greeting = greetingFor(time.now().atZone(time.zone()).toLocalTime()),
                gardenEnabled = p.gardenEnabled,
                aquariumEnabled = p.aquariumEnabled,
                plantCount = plants,
                growingCount = c.growing,
                fishCount = fish,
                tankCount = tanks,
                tasks = today.items,
                week = week,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun onToggleTask(occurrence: TaskOccurrence, done: Boolean) {
        viewModelScope.launch {
            if (done) {
                actions.markDone(occurrence.task.id, occurrence.originalDate)
            } else {
                actions.undo(occurrence.task.id, occurrence.originalDate)
            }
        }
    }
}
