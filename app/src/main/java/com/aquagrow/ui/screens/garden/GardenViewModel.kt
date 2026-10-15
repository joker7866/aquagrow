package com.aquagrow.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquagrow.domain.garden.CareDue
import com.aquagrow.domain.garden.DueStatus
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class PlantCardUi(
    val plant: Plant,
    val dayNumber: Long,
    val nextWatering: TaskOccurrence?,
    val wateringStatus: DueStatus?,
)

data class GardenUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val query: String = "",
    val category: PlantCategory? = null,
    val categories: List<PlantCategory> = emptyList(),
    val plants: List<PlantCardUi> = emptyList(),
    val totalPlants: Int = 0,
    /** Plants whose watering is due today or overdue. */
    val needsWaterCount: Int = 0,
)

@HiltViewModel
class GardenViewModel @Inject constructor(
    garden: GardenRepository,
    tasks: TaskRepository,
    time: TimeProvider,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<PlantCategory?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val cards = time.observeToday().flatMapLatest { today ->
        combine(
            garden.observePlants(),
            tasks.observeOccurrences(today, today.plusDays(60)),
            tasks.observeOverdue(today),
        ) { plants, upcoming, overdue ->
            today to plants.map { p ->
                val next = CareDue.nextForPlant(p.id, TaskType.WATERING, upcoming, overdue, today)
                PlantCardUi(p, p.dayNumber(today), next, next?.let { DueStatus.of(it.date, today) })
            }
        }
    }

    val uiState: StateFlow<GardenUiState> = combine(cards, query, category) { (today, all), q, cat ->
        val filtered = all
            .filter { cat == null || it.plant.category == cat }
            .filter {
                q.isBlank() || it.plant.name.contains(q.trim(), ignoreCase = true) ||
                    it.plant.variety?.contains(q.trim(), ignoreCase = true) == true ||
                    it.plant.location?.contains(q.trim(), ignoreCase = true) == true
            }
        GardenUiState(
            loading = false,
            today = today,
            query = q,
            category = cat,
            categories = all.map { it.plant.category }.distinct().sortedBy { it.ordinal },
            plants = filtered,
            totalPlants = all.size,
            needsWaterCount = all.count { it.wateringStatus is DueStatus.Today || it.wateringStatus is DueStatus.Overdue },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GardenUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }
    fun onCategoryChange(value: PlantCategory?) {
        category.value = value
    }
}
