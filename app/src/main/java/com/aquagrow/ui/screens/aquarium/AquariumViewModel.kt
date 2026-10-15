package com.aquagrow.ui.screens.aquarium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquagrow.domain.aquarium.ParameterCheck
import com.aquagrow.domain.aquarium.WaterRangeChecker
import com.aquagrow.domain.model.AquariumSummary
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterRanges
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class TankStatus { OK, CHECK, NO_TEST, NO_RANGES }

data class TankCardUi(
    val summary: AquariumSummary,
    val status: TankStatus,
    val outOfRange: List<ParameterCheck>,
    /** Next pending feeding time today, if any. */
    val nextFeeding: LocalTime? = null,
    /** Overdue maintenance / water-test occurrences for this tank. */
    val overdue: List<TaskOccurrence> = emptyList(),
)

data class AquariumUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val tanks: List<TankCardUi> = emptyList(),
    val fish: List<Fish> = emptyList(),
)

fun tankStatus(summary: AquariumSummary): Pair<TankStatus, List<ParameterCheck>> {
    val test = summary.latestTest ?: return TankStatus.NO_TEST to emptyList()
    if (summary.aquarium.ranges == WaterRanges()) return TankStatus.NO_RANGES to emptyList()
    val out = WaterRangeChecker.outOfRange(test, summary.aquarium.ranges)
    return (if (out.isEmpty()) TankStatus.OK else TankStatus.CHECK) to out
}

@HiltViewModel
class AquariumViewModel @Inject constructor(
    aquarium: AquariumRepository,
    tasks: TaskRepository,
    time: TimeProvider,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AquariumUiState> = time.observeToday().flatMapLatest { today ->
        combine(
            aquarium.observeSummaries(),
            aquarium.observeActiveFish(),
            tasks.observeOverdue(today),
            tasks.observeOccurrences(today, today),
        ) { tanks, fish, overdue, todayOcc ->
            val nowTime = time.now().atZone(time.zone()).toLocalTime()
            AquariumUiState(
                loading = false,
                today = today,
                tanks = tanks.map { s ->
                    val id = s.aquarium.id
                    val (st, out) = tankStatus(s)
                    TankCardUi(
                        summary = s,
                        status = st,
                        outOfRange = out,
                        nextFeeding = todayOcc
                            .filter { it.task.aquariumId == id && it.task.type == TaskType.FEEDING && !it.isHandled }
                            .mapNotNull { it.task.reminderTime }
                            .filter { !it.isBefore(nowTime) }
                            .minOrNull(),
                        overdue = overdue.filter { it.task.aquariumId == id && it.task.type != TaskType.FEEDING },
                    )
                },
                fish = fish,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AquariumUiState())
}
