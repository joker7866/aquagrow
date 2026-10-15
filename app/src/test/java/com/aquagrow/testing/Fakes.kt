package com.aquagrow.testing

import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.AquariumSummary
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.FeedingLog
import com.aquagrow.domain.model.FertilizerLog
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.GrowthLog
import com.aquagrow.domain.model.HarvestLog
import com.aquagrow.domain.model.MaintenanceLog
import com.aquagrow.domain.model.ModuleChoice
import com.aquagrow.domain.model.OccurrenceState
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.SowingGuide
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.ThemeMode
import com.aquagrow.domain.model.UserPreferences
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.model.WateringLog
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.schedule.OccurrenceResolver
import com.aquagrow.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Fixed clock for tests. observeToday emits once and stays (no midnight ticking). */
class FakeTime(var today: LocalDate = LocalDate.of(2026, 10, 8), var time: LocalTime = LocalTime.of(9, 0)) : TimeProvider {
    override fun now(): Instant = today.atTime(time).toInstant(ZoneOffset.UTC)
    override fun zone(): ZoneId = ZoneOffset.UTC
    override fun observeToday(): Flow<LocalDate> = flowOf(today)
}

class FakeTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<List<CareTask>>(emptyList())
    val states = MutableStateFlow<List<OccurrenceState>>(emptyList())
    private var nextId = 1L

    override fun observeTasks() = tasks
    override fun observeTasksForPlant(plantId: Long) = tasks.map { l -> l.filter { it.plantId == plantId } }
    override fun observeTasksForAquarium(aquariumId: Long) = tasks.map { l -> l.filter { it.aquariumId == aquariumId } }
    override fun observeOccurrences(from: LocalDate, to: LocalDate) = combine(tasks, states) { t, s -> OccurrenceResolver.resolve(t, s, from, to) }
    override fun observeOverdue(today: LocalDate) = combine(tasks, states) { t, s -> OccurrenceResolver.overdue(t, s, today) }
    override suspend fun getOccurrences(from: LocalDate, to: LocalDate): List<TaskOccurrence> = OccurrenceResolver.resolve(tasks.value, states.value, from, to)
    override suspend fun getOverdue(today: LocalDate) = OccurrenceResolver.overdue(tasks.value, states.value, today)
    override suspend fun getTask(id: Long) = tasks.value.firstOrNull { it.id == id }
    override suspend fun getTasksForPlant(plantId: Long) = tasks.value.filter { it.plantId == plantId }
    override suspend fun getTasksForExport() = tasks.value.filter { it.enabled }
    override suspend fun getTasksForAquarium(aquariumId: Long) = tasks.value.filter { it.aquariumId == aquariumId }
    override suspend fun saveTask(task: CareTask): Long {
        val id = if (task.id == 0L) nextId++ else task.id
        tasks.update { list -> list.filterNot { it.id == id } + task.copy(id = id) }
        return id
    }
    override suspend fun deleteTask(id: Long) = tasks.update { l -> l.filterNot { it.id == id } }
    override suspend fun getState(taskId: Long, originalDate: LocalDate) = states.value.firstOrNull { it.taskId == taskId && it.originalDate == originalDate }
    override suspend fun putState(state: OccurrenceState) = states.update { l ->
        l.filterNot { it.taskId == state.taskId && it.originalDate == state.originalDate } + state
    }
    override suspend fun clearState(taskId: Long, originalDate: LocalDate) = states.update { l ->
        l.filterNot { it.taskId == taskId && it.originalDate == originalDate }
    }
    override fun observeHistory(taskId: Long, limit: Int) = states.map { l -> l.filter { it.taskId == taskId } }
}

class FakeGardenRepository : GardenRepository {
    val plants = MutableStateFlow<List<Plant>>(emptyList())
    val watering = MutableStateFlow<List<WateringLog>>(emptyList())
    val fertilizer = MutableStateFlow<List<FertilizerLog>>(emptyList())
    val growth = MutableStateFlow<List<GrowthLog>>(emptyList())
    val harvests = MutableStateFlow<List<HarvestLog>>(emptyList())
    private var nextId = 1L

    override fun observePlants() = plants.map { l -> l.filterNot { it.archived } }
    override fun observePlant(id: Long) = plants.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun getPlant(id: Long) = plants.value.firstOrNull { it.id == id }
    override fun observeActivePlantCount() = plants.map { l -> l.count { !it.archived } }
    override suspend fun savePlant(plant: Plant): Long {
        val id = if (plant.id == 0L) nextId++ else plant.id
        plants.update { l -> l.filterNot { it.id == id } + plant.copy(id = id) }
        return id
    }
    override suspend fun deletePlant(id: Long) = plants.update { l -> l.filterNot { it.id == id } }
    override fun observeGrowthLogs(plantId: Long) = growth.map { l -> l.filter { it.plantId == plantId } }
    override fun observeAllGrowthLogs() = growth
    override suspend fun addGrowthLog(log: GrowthLog): Long {
        growth.update { it + log.copy(id = nextId) }
        return nextId++
    }
    override suspend fun deleteGrowthLog(log: GrowthLog) = growth.update { l -> l - log }
    override fun observeWateringLogs(plantId: Long) = watering.map { l -> l.filter { it.plantId == plantId } }
    override fun observeRecentWatering(limit: Int) = watering
    override fun observeLastWatered(plantId: Long) = watering.map { l -> l.filter { it.plantId == plantId && it.watered }.maxOfOrNull { it.loggedAt } }
    override suspend fun addWateringLog(log: WateringLog): Long {
        watering.update { it + log.copy(id = nextId) }
        return nextId++
    }
    override suspend fun deleteWateringLog(log: WateringLog) = watering.update { l -> l - log }
    override suspend fun deleteWateringForTask(taskId: Long, at: Instant) = watering.update { l -> l.filterNot { it.taskId == taskId && it.loggedAt == at } }
    override fun observeFertilizerLogs(plantId: Long) = fertilizer.map { l -> l.filter { it.plantId == plantId } }
    override fun observeAllFertilizerLogs() = fertilizer
    override suspend fun addFertilizerLog(log: FertilizerLog): Long {
        fertilizer.update { it + log.copy(id = nextId) }
        return nextId++
    }
    override suspend fun deleteFertilizerLog(log: FertilizerLog) = fertilizer.update { l -> l - log }
    override suspend fun deleteFertilizerForTask(taskId: Long, at: Instant) = fertilizer.update { l -> l.filterNot { it.taskId == taskId && it.appliedAt == at } }
    override fun observeHarvestLogs(plantId: Long) = harvests.map { l -> l.filter { it.plantId == plantId } }
    override fun observeAllHarvestLogs() = harvests
    override suspend fun addHarvestLog(log: HarvestLog): Long {
        harvests.update { it + log.copy(id = nextId) }
        return nextId++
    }
    override suspend fun deleteHarvestLog(log: HarvestLog) = harvests.update { l -> l - log }
    override fun observeSowingGuides() = MutableStateFlow(emptyList<SowingGuide>())
    override suspend fun saveSowingGuide(guide: SowingGuide) = Unit
    override suspend fun deleteSowingGuide(guide: SowingGuide) = Unit
}

class FakeAquariumRepository : AquariumRepository {
    val aquariums = MutableStateFlow<List<Aquarium>>(emptyList())
    val fish = MutableStateFlow<List<Fish>>(emptyList())
    val feeding = MutableStateFlow<List<FeedingLog>>(emptyList())
    val tests = MutableStateFlow<List<WaterTest>>(emptyList())
    val maintenance = MutableStateFlow<List<MaintenanceLog>>(emptyList())
    private var nextId = 1L

    override fun observeSummaries() = combine(aquariums, fish, tests) { a, f, t ->
        a.filterNot { it.archived }.map { tank ->
            AquariumSummary(tank, t.filter { it.aquariumId == tank.id }.maxByOrNull { it.testedAt }, f.filter { it.aquariumId == tank.id }.sumOf { it.count })
        }
    }
    override fun observeAquariums() = aquariums.map { l -> l.filterNot { it.archived } }
    override fun observeAquarium(id: Long) = aquariums.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun getAquarium(id: Long) = aquariums.value.firstOrNull { it.id == id }
    override fun observeActiveCount() = aquariums.map { l -> l.count { !it.archived } }
    override suspend fun saveAquarium(aquarium: Aquarium): Long {
        val id = if (aquarium.id == 0L) nextId++ else aquarium.id
        aquariums.update { l -> l.filterNot { it.id == id } + aquarium.copy(id = id) }
        return id
    }
    override suspend fun deleteAquarium(id: Long) = aquariums.update { l -> l.filterNot { it.id == id } }
    override fun observeFish(aquariumId: Long) = fish.map { l -> l.filter { it.aquariumId == aquariumId } }
    override fun observeActiveFish() = fish
    override fun observeFishById(id: Long) = fish.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun getFish(id: Long) = fish.value.firstOrNull { it.id == id }
    override suspend fun getFishForAquarium(aquariumId: Long) = fish.value.filter { it.aquariumId == aquariumId }
    override fun observeTotalFishCount() = fish.map { l -> l.sumOf { it.count } }
    override suspend fun saveFish(fish: Fish): Long {
        val id = if (fish.id == 0L) nextId++ else fish.id
        this.fish.update { l -> l.filterNot { it.id == id } + fish.copy(id = id) }
        return id
    }
    override suspend fun deleteFish(id: Long) = fish.update { l -> l.filterNot { it.id == id } }
    override fun observeFeedingLogs(aquariumId: Long) = feeding.map { l -> l.filter { it.aquariumId == aquariumId } }
    override fun observeRecentFeeding(limit: Int) = feeding
    override suspend fun addFeedingLog(log: FeedingLog): Long {
        feeding.update { it + log.copy(id = nextId) }
        return nextId++
    }
    override suspend fun deleteFeedingLog(log: FeedingLog) = feeding.update { l -> l - log }
    override suspend fun deleteFeedingForTask(taskId: Long, at: Instant) = feeding.update { l -> l.filterNot { it.taskId == taskId && it.fedAt == at } }
    override fun observeWaterTests(aquariumId: Long) = tests.map { l -> l.filter { it.aquariumId == aquariumId } }
    override fun observeAllWaterTests() = tests
    override suspend fun addWaterTest(test: WaterTest): Long {
        tests.update { it + test.copy(id = nextId) }
        return nextId++
    }
    override suspend fun deleteWaterTest(test: WaterTest) = tests.update { l -> l - test }
    override fun observeMaintenanceLogs(aquariumId: Long) = maintenance.map { l -> l.filter { it.aquariumId == aquariumId } }
    override fun observeAllMaintenanceLogs() = maintenance
    override suspend fun addMaintenanceLog(log: MaintenanceLog): Long {
        maintenance.update { it + log.copy(id = nextId) }
        return nextId++
    }
    override suspend fun deleteMaintenanceLog(log: MaintenanceLog) = maintenance.update { l -> l - log }
    override suspend fun deleteMaintenanceForTask(taskId: Long, at: Instant) = maintenance.update { l -> l.filterNot { it.taskId == taskId && it.performedAt == at } }
}

class FakePreferences(initial: UserPreferences = UserPreferences(onboardingCompleted = true)) : UserPreferencesRepository {
    val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state
    override suspend fun completeOnboarding(choice: ModuleChoice) = state.update { it.copy(onboardingCompleted = true, gardenEnabled = choice.garden, aquariumEnabled = choice.aquarium) }
    override suspend fun setGardenEnabled(enabled: Boolean) = state.update { it.copy(gardenEnabled = enabled) }
    override suspend fun setAquariumEnabled(enabled: Boolean) = state.update { it.copy(aquariumEnabled = enabled) }
    override suspend fun setAiAssistantEnabled(enabled: Boolean) = state.update { it.copy(aiAssistantEnabled = enabled) }
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setCurrencyCode(code: String) = state.update { it.copy(currencyCode = code) }
    override suspend fun setDisplayName(name: String) = state.update { it.copy(displayName = name) }
}
