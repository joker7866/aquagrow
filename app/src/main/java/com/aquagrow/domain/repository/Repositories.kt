package com.aquagrow.domain.repository

import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.AquariumSummary
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.Expense
import com.aquagrow.domain.model.FeedingLog
import com.aquagrow.domain.model.FertilizerLog
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.GrowthLog
import com.aquagrow.domain.model.HarvestLog
import com.aquagrow.domain.model.InventoryItem
import com.aquagrow.domain.model.MaintenanceLog
import com.aquagrow.domain.model.NotificationSettings
import com.aquagrow.domain.model.OccurrenceState
import com.aquagrow.domain.model.Photo
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.ReminderKind
import com.aquagrow.domain.model.ReminderLogEntry
import com.aquagrow.domain.model.SowingGuide
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.model.WateringLog
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface GardenRepository {
    fun observePlants(): Flow<List<Plant>>
    fun observePlant(id: Long): Flow<Plant?>
    suspend fun getPlant(id: Long): Plant?
    fun observeActivePlantCount(): Flow<Int>

    /** Inserts when [Plant.id] is 0, otherwise updates. Returns the id. */
    suspend fun savePlant(plant: Plant): Long
    suspend fun deletePlant(id: Long)

    fun observeGrowthLogs(plantId: Long): Flow<List<GrowthLog>>
    fun observeAllGrowthLogs(): Flow<List<GrowthLog>>
    suspend fun addGrowthLog(log: GrowthLog): Long
    suspend fun deleteGrowthLog(log: GrowthLog)

    fun observeWateringLogs(plantId: Long): Flow<List<WateringLog>>
    fun observeRecentWatering(limit: Int): Flow<List<WateringLog>>
    fun observeLastWatered(plantId: Long): Flow<Instant?>
    suspend fun addWateringLog(log: WateringLog): Long
    suspend fun deleteWateringLog(log: WateringLog)
    suspend fun deleteWateringForTask(taskId: Long, at: Instant)

    fun observeFertilizerLogs(plantId: Long): Flow<List<FertilizerLog>>
    fun observeAllFertilizerLogs(): Flow<List<FertilizerLog>>
    suspend fun addFertilizerLog(log: FertilizerLog): Long
    suspend fun deleteFertilizerLog(log: FertilizerLog)
    suspend fun deleteFertilizerForTask(taskId: Long, at: Instant)

    fun observeHarvestLogs(plantId: Long): Flow<List<HarvestLog>>
    fun observeAllHarvestLogs(): Flow<List<HarvestLog>>
    suspend fun addHarvestLog(log: HarvestLog): Long
    suspend fun deleteHarvestLog(log: HarvestLog)

    fun observeSowingGuides(): Flow<List<SowingGuide>>
    suspend fun saveSowingGuide(guide: SowingGuide)
    suspend fun deleteSowingGuide(guide: SowingGuide)
}

interface AquariumRepository {
    fun observeSummaries(): Flow<List<AquariumSummary>>
    fun observeAquariums(): Flow<List<Aquarium>>
    fun observeAquarium(id: Long): Flow<Aquarium?>
    suspend fun getAquarium(id: Long): Aquarium?
    fun observeActiveCount(): Flow<Int>
    suspend fun saveAquarium(aquarium: Aquarium): Long
    suspend fun deleteAquarium(id: Long)

    fun observeFish(aquariumId: Long): Flow<List<Fish>>
    fun observeActiveFish(): Flow<List<Fish>>
    fun observeFishById(id: Long): Flow<Fish?>
    suspend fun getFish(id: Long): Fish?
    suspend fun getFishForAquarium(aquariumId: Long): List<Fish>
    fun observeTotalFishCount(): Flow<Int>
    suspend fun saveFish(fish: Fish): Long
    suspend fun deleteFish(id: Long)

    fun observeFeedingLogs(aquariumId: Long): Flow<List<FeedingLog>>
    fun observeRecentFeeding(limit: Int): Flow<List<FeedingLog>>
    suspend fun addFeedingLog(log: FeedingLog): Long
    suspend fun deleteFeedingLog(log: FeedingLog)
    suspend fun deleteFeedingForTask(taskId: Long, at: Instant)

    fun observeWaterTests(aquariumId: Long): Flow<List<WaterTest>>
    fun observeAllWaterTests(): Flow<List<WaterTest>>
    suspend fun addWaterTest(test: WaterTest): Long
    suspend fun deleteWaterTest(test: WaterTest)

    fun observeMaintenanceLogs(aquariumId: Long): Flow<List<MaintenanceLog>>
    fun observeAllMaintenanceLogs(): Flow<List<MaintenanceLog>>
    suspend fun addMaintenanceLog(log: MaintenanceLog): Long
    suspend fun deleteMaintenanceLog(log: MaintenanceLog)
    suspend fun deleteMaintenanceForTask(taskId: Long, at: Instant)
}

interface TaskRepository {
    fun observeTasks(): Flow<List<CareTask>>
    fun observeTasksForPlant(plantId: Long): Flow<List<CareTask>>
    fun observeTasksForAquarium(aquariumId: Long): Flow<List<CareTask>>

    /** Occurrences currently dated within [from]..[to]. */
    fun observeOccurrences(from: LocalDate, to: LocalDate): Flow<List<TaskOccurrence>>
    fun observeOverdue(today: LocalDate): Flow<List<TaskOccurrence>>
    suspend fun getOccurrences(from: LocalDate, to: LocalDate): List<TaskOccurrence>
    suspend fun getOverdue(today: LocalDate): List<TaskOccurrence>
    suspend fun getTask(id: Long): CareTask?
    suspend fun getTasksForPlant(plantId: Long): List<CareTask>
    suspend fun getTasksForAquarium(aquariumId: Long): List<CareTask>

    /** Enabled, active tasks with subject names (calendar export). */
    suspend fun getTasksForExport(): List<CareTask>
    suspend fun saveTask(task: CareTask): Long
    suspend fun deleteTask(id: Long)
    suspend fun getState(taskId: Long, originalDate: LocalDate): OccurrenceState?
    suspend fun putState(state: OccurrenceState)
    suspend fun clearState(taskId: Long, originalDate: LocalDate)
    fun observeHistory(taskId: Long, limit: Int): Flow<List<OccurrenceState>>
}

interface InventoryRepository {
    fun observeItems(): Flow<List<InventoryItem>>
    fun observeItems(module: CareModule): Flow<List<InventoryItem>>
    fun observeEquipment(aquariumId: Long): Flow<List<InventoryItem>>
    suspend fun getItem(id: Long): InventoryItem?
    suspend fun getItems(): List<InventoryItem>
    suspend fun saveItem(item: InventoryItem): Long
    suspend fun deleteItem(id: Long)
    fun observeExpenses(): Flow<List<Expense>>
    suspend fun getExpensesForItem(itemId: Long): List<Expense>
    suspend fun saveExpense(expense: Expense): Long
    suspend fun deleteExpense(expense: Expense)
}

data class CaptureTarget(val absolutePath: String, val relativePath: String)

interface PhotoRepository {
    fun observeAll(): Flow<List<Photo>>
    fun observeForPlant(plantId: Long): Flow<List<Photo>>
    fun observeForAquarium(aquariumId: Long): Flow<List<Photo>>
    fun observeForFish(fishId: Long): Flow<List<Photo>>

    /** Copies a picked content Uri into app storage; returns the relative path. */
    suspend fun importPhoto(uri: String): String

    /** A file for the in-app camera to write into. */
    fun newCaptureTarget(): CaptureTarget
    suspend fun addPhoto(photo: Photo): Long
    suspend fun deletePhoto(photo: Photo)

    /** Deletes an imported/captured file that was never saved (e.g. form cancelled). */
    suspend fun discardFile(relativePath: String)
}

enum class RestoreMode { REPLACE, MERGE }

data class BackupPreview(
    val exportedAt: Instant,
    val appVersion: String,
    val counts: Map<String, Int>,
    val photoFiles: Int,
)

interface BackupRepository {
    /** Writes all data + settings to [uri] (JSON, or ZIP with photos). Returns counts per type. */
    suspend fun export(uri: String, includePhotos: Boolean): Map<String, Int>

    /** Parses and validates without changing anything. Throws with a readable message if invalid. */
    suspend fun inspect(uri: String): BackupPreview
    suspend fun restore(uri: String, mode: RestoreMode): Map<String, Int>

    /** Writes enabled tasks as an iCalendar (.ics) file. Returns the number of events. */
    suspend fun exportIcs(uri: String): Int
}

data class SearchResults(
    val plants: List<Plant> = emptyList(),
    val fish: List<Fish> = emptyList(),
    val tanks: List<Aquarium> = emptyList(),
    val tasks: List<CareTask> = emptyList(),
    val notes: List<GrowthLog> = emptyList(),
) {
    val isEmpty: Boolean get() = plants.isEmpty() && fish.isEmpty() && tanks.isEmpty() && tasks.isEmpty() && notes.isEmpty()
}

interface SearchRepository {
    suspend fun search(query: String): SearchResults
}

interface SettingsRepository {
    val notificationSettings: Flow<NotificationSettings>
    suspend fun getNotificationSettings(): NotificationSettings
    suspend fun updateNotificationSettings(transform: (NotificationSettings) -> NotificationSettings)
}

interface ReminderLogRepository {
    fun observeRecent(limit: Int): Flow<List<ReminderLogEntry>>

    /** Records a posted reminder. Returns false if [dedupeKey] was already logged (i.e. a duplicate). */
    suspend fun recordIfNew(
        kind: ReminderKind,
        taskId: Long?,
        occurrenceDate: LocalDate?,
        inventoryItemId: Long?,
        title: String,
        text: String,
        dedupeKey: String,
    ): Boolean
    suspend fun clear()
}

/** Implemented by the notification layer; called whenever tasks or their states change. */
interface ReminderScheduler {
    fun requestReschedule()
}
