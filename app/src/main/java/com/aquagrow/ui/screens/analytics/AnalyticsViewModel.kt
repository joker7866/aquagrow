package com.aquagrow.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquagrow.domain.analytics.AnalyticsCalc
import com.aquagrow.domain.analytics.Completion
import com.aquagrow.domain.analytics.ExpenseCalc
import com.aquagrow.domain.analytics.WeekStat
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.PlantCondition
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.InventoryRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.isModuleOn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class AnalyticsTab { OVERALL, GARDEN, AQUARIUM }

data class TankHistory(val tank: Aquarium, val tests: Int, val maintenance: Int, val feedings: Int, val nitrate: List<WaterTest>)

data class AnalyticsUiState(
    val loading: Boolean = true,
    val tab: AnalyticsTab = AnalyticsTab.OVERALL,
    val gardenEnabled: Boolean = true,
    val aquariumEnabled: Boolean = true,
    val currency: String = "INR",
    // Overall
    val weekly: List<WeekStat> = emptyList(),
    val streak: Int = 0,
    val upcoming: Int = 0,
    val monthlyExpenses: List<Pair<YearMonth, Long>> = emptyList(),
    // Garden
    val activePlants: Int = 0,
    val watering: Completion = Completion(0, 0),
    val fertilizerByType: List<Pair<FertilizerType, Int>> = emptyList(),
    val growthProgress: Int? = null,
    val harvestTotals: List<Pair<String, Double>> = emptyList(),
    val conditions: List<Pair<PlantCondition, Int>> = emptyList(),
    // Aquarium
    val fishTotal: Int = 0,
    val population: List<Pair<String, Int>> = emptyList(),
    val feeding: Completion = Completion(0, 0),
    val maintenance: Completion = Completion(0, 0),
    val tanks: List<TankHistory> = emptyList(),
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    tasks: TaskRepository,
    garden: GardenRepository,
    aquarium: AquariumRepository,
    inventory: InventoryRepository,
    prefs: UserPreferencesRepository,
    time: TimeProvider,
) : ViewModel() {
    private val tab = MutableStateFlow(AnalyticsTab.OVERALL)
    fun setTab(t: AnalyticsTab) {
        tab.value = t
    }

    private val gardenData = combine(garden.observePlants(), garden.observeAllFertilizerLogs(), garden.observeAllHarvestLogs(), garden.observeAllGrowthLogs()) { p, f, h, g -> GardenData(p, f, h, g) }
    private val aquaData = combine(aquarium.observeAquariums(), aquarium.observeActiveFish(), aquarium.observeAllWaterTests(), aquarium.observeAllMaintenanceLogs(), aquarium.observeRecentFeeding(10_000)) { a, f, w, m, fe -> AquaData(a, f, w, m, fe) }

    private data class GardenData(val plants: List<com.aquagrow.domain.model.Plant>, val fert: List<com.aquagrow.domain.model.FertilizerLog>, val harvests: List<com.aquagrow.domain.model.HarvestLog>, val growth: List<com.aquagrow.domain.model.GrowthLog>)
    private data class AquaData(val tanks: List<Aquarium>, val fish: List<Fish>, val tests: List<WaterTest>, val maint: List<com.aquagrow.domain.model.MaintenanceLog>, val feeding: List<com.aquagrow.domain.model.FeedingLog>)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AnalyticsUiState> = time.observeToday().flatMapLatest { today ->
        combine(
            tasks.observeOccurrences(today.minusDays(90), today.plusDays(7)),
            combine(gardenData, aquaData, ::Pair),
            inventory.observeExpenses(),
            prefs.preferences,
            tab,
        ) { occAll, (g, a), expenses, p, t ->
            val occ = occAll.filter { p.isModuleOn(it.task.module) }
            val past30 = occ.filter { !it.date.isBefore(today.minusDays(30)) }
            fun since(days: Long): Instant = today.minusDays(days).atStartOfDay(time.zone()).toInstant()
            AnalyticsUiState(
                loading = false, tab = t, gardenEnabled = p.gardenEnabled, aquariumEnabled = p.aquariumEnabled, currency = p.currencyCode,
                weekly = AnalyticsCalc.weekly(occ, today),
                streak = AnalyticsCalc.streak(occ, today),
                upcoming = occ.count { it.date.isAfter(today.minusDays(1)) && !it.date.isAfter(today.plusDays(7)) && !it.isHandled },
                monthlyExpenses = ExpenseCalc.monthly(expenses, p.currencyCode, YearMonth.from(today)),
                activePlants = g.plants.size,
                watering = AnalyticsCalc.completion(past30.filter { it.task.type == TaskType.WATERING }, today),
                fertilizerByType = g.fert.filter { it.appliedAt.isAfter(since(180)) }.groupingBy { it.type }.eachCount().toList().sortedByDescending { it.second },
                growthProgress = g.plants.filter { it.growthStages.size > 1 }.takeIf { it.isNotEmpty() }
                    ?.map { it.currentStageIndex * 100 / it.growthStages.lastIndex }?.average()?.toInt(),
                harvestTotals = g.harvests.groupBy { it.unit }.map { (u, l) -> u to l.sumOf { it.quantity } },
                conditions = g.growth.filter { it.condition != null && it.loggedAt.isAfter(since(90)) }.groupingBy { it.condition!! }.eachCount().toList().sortedBy { it.first.ordinal },
                fishTotal = a.fish.sumOf { it.count },
                population = a.fish.groupBy { it.speciesName }.map { (n, l) -> n to l.sumOf { it.count } }.sortedByDescending { it.second },
                feeding = AnalyticsCalc.completion(past30.filter { it.task.type == TaskType.FEEDING }, today),
                maintenance = AnalyticsCalc.completion(occ.filter { it.task.type == TaskType.MAINTENANCE || it.task.type == TaskType.WATER_TEST }, today),
                tanks = a.tanks.map { tank ->
                    TankHistory(
                        tank,
                        a.tests.count { it.aquariumId == tank.id },
                        a.maint.count { it.aquariumId == tank.id },
                        a.feeding.count { it.aquariumId == tank.id && !it.skipped },
                        a.tests.filter { it.aquariumId == tank.id && it.nitratePpm != null }.sortedBy { it.testedAt }.takeLast(12),
                    )
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())
}
