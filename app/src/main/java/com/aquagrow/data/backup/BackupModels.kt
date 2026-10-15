@file:UseSerializers(InstantSerializer::class)

package com.aquagrow.data.backup

import com.aquagrow.data.local.entity.AquariumEntity
import com.aquagrow.data.local.entity.ExpenseEntity
import com.aquagrow.data.local.entity.FeedingLogEntity
import com.aquagrow.data.local.entity.FertilizerLogEntity
import com.aquagrow.data.local.entity.FishEntity
import com.aquagrow.data.local.entity.HarvestLogEntity
import com.aquagrow.data.local.entity.InventoryItemEntity
import com.aquagrow.data.local.entity.MaintenanceLogEntity
import com.aquagrow.data.local.entity.NotificationSettingsEntity
import com.aquagrow.data.local.entity.PhotoRecordEntity
import com.aquagrow.data.local.entity.PlantEntity
import com.aquagrow.data.local.entity.PlantGrowthLogEntity
import com.aquagrow.data.local.entity.ReminderLogEntity
import com.aquagrow.data.local.entity.SowingGuideEntity
import com.aquagrow.data.local.entity.TaskOccurrenceStateEntity
import com.aquagrow.data.local.entity.TaskScheduleEntity
import com.aquagrow.data.local.entity.WaterQualityLogEntity
import com.aquagrow.data.local.entity.WateringLogEntity
import com.aquagrow.domain.model.ThemeMode
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json

/**
 * Backup file. [schemaVersion] is the Room schema the rows were exported from; restores
 * from an older schema can be upgraded here in future, newer ones are refused.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val formatVersion: Int = FORMAT_VERSION,
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Instant,
    val preferences: BackupPreferences,
    val data: BackupData,
) {
    companion object {
        const val FORMAT = "aquagrow-backup"
        const val FORMAT_VERSION = 1

        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true // tolerate fields added by newer app versions
            encodeDefaults = true
        }
    }
}

@Serializable
data class BackupPreferences(
    val gardenEnabled: Boolean = true,
    val aquariumEnabled: Boolean = true,
    val aiAssistantEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyCode: String = "INR",
    val displayName: String = "",
)

@Serializable
data class BackupData(
    val plants: List<PlantEntity> = emptyList(),
    val growthLogs: List<PlantGrowthLogEntity> = emptyList(),
    val wateringLogs: List<WateringLogEntity> = emptyList(),
    val fertilizerLogs: List<FertilizerLogEntity> = emptyList(),
    val harvestLogs: List<HarvestLogEntity> = emptyList(),
    val sowingGuides: List<SowingGuideEntity> = emptyList(),
    val inventory: List<InventoryItemEntity> = emptyList(),
    val aquariums: List<AquariumEntity> = emptyList(),
    val fish: List<FishEntity> = emptyList(),
    val feedingLogs: List<FeedingLogEntity> = emptyList(),
    val waterTests: List<WaterQualityLogEntity> = emptyList(),
    val maintenanceLogs: List<MaintenanceLogEntity> = emptyList(),
    val tasks: List<TaskScheduleEntity> = emptyList(),
    val occurrenceStates: List<TaskOccurrenceStateEntity> = emptyList(),
    val reminderLogs: List<ReminderLogEntity> = emptyList(),
    val notificationSettings: List<NotificationSettingsEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val photos: List<PhotoRecordEntity> = emptyList(),
) {
    val counts: Map<String, Int>
        get() = mapOf(
            "plants" to plants.size,
            "aquariums" to aquariums.size,
            "fish" to fish.size,
            "tasks" to tasks.size,
            "logs" to (
                growthLogs.size + wateringLogs.size + fertilizerLogs.size + harvestLogs.size +
                    feedingLogs.size + waterTests.size + maintenanceLogs.size
                ),
            "inventory" to inventory.size,
            "expenses" to expenses.size,
            "photos" to photos.size,
        )
}

/**
 * Checks a parsed backup before anything is written: format, versions, duplicate ids and
 * every foreign key. A file that fails is rejected as a whole, so a bad backup can never
 * leave the database half-restored.
 */
object BackupValidator {
    fun validate(file: BackupFile, currentSchema: Int): List<String> = buildList {
        if (file.format != BackupFile.FORMAT) add("Not an AquaGrow backup file.")
        if (file.formatVersion > BackupFile.FORMAT_VERSION) add("This backup was made by a newer version of AquaGrow. Please update the app.")
        if (file.schemaVersion > currentSchema) add("This backup uses a newer database version (${file.schemaVersion}). Please update the app.")
        if (isNotEmpty()) return@buildList

        val d = file.data
        fun dupes(name: String, ids: List<Long>) {
            if (ids.size != ids.toSet().size) add("Duplicate $name ids.")
        }
        dupes("plant", d.plants.map { it.id })
        dupes("aquarium", d.aquariums.map { it.id })
        dupes("fish", d.fish.map { it.id })
        dupes("task", d.tasks.map { it.id })
        dupes("inventory", d.inventory.map { it.id })
        dupes("growth log", d.growthLogs.map { it.id })
        dupes("photo", d.photos.map { it.id })
        dupes("expense", d.expenses.map { it.id })

        val plants = d.plants.map { it.id }.toSet()
        val tanks = d.aquariums.map { it.id }.toSet()
        val fish = d.fish.map { it.id }.toSet()
        val tasks = d.tasks.map { it.id }.toSet()
        val items = d.inventory.map { it.id }.toSet()
        val growth = d.growthLogs.map { it.id }.toSet()
        fun ref(what: String, id: Long?, valid: Set<Long>) {
            if (id != null && id !in valid) add("$what refers to a missing record ($id).")
        }

        d.growthLogs.forEach { ref("Growth log", it.plantId, plants) }
        d.wateringLogs.forEach {
            ref("Watering log", it.plantId, plants)
            ref("Watering log task", it.taskId, tasks)
        }
        d.fertilizerLogs.forEach {
            ref("Fertilizer log", it.plantId, plants)
            ref("Fertilizer item", it.inventoryItemId, items)
            ref("Fertilizer task", it.taskId, tasks)
        }
        d.harvestLogs.forEach { ref("Harvest log", it.plantId, plants) }
        d.inventory.forEach { ref("Inventory item", it.aquariumId, tanks) }
        d.fish.forEach { ref("Fish", it.aquariumId, tanks) }
        d.feedingLogs.forEach {
            ref("Feeding log", it.aquariumId, tanks)
            ref("Feeding fish", it.fishId, fish)
            ref("Feeding task", it.taskId, tasks)
        }
        d.waterTests.forEach { ref("Water test", it.aquariumId, tanks) }
        d.maintenanceLogs.forEach {
            ref("Maintenance log", it.aquariumId, tanks)
            ref("Maintenance task", it.taskId, tasks)
        }
        d.tasks.forEach {
            ref("Task plant", it.plantId, plants)
            ref("Task tank", it.aquariumId, tanks)
            ref("Task fish", it.fishId, fish)
        }
        d.occurrenceStates.forEach { ref("Task history", it.taskId, tasks) }
        d.expenses.forEach {
            ref("Expense item", it.inventoryItemId, items)
            ref("Expense plant", it.plantId, plants)
            ref("Expense tank", it.aquariumId, tanks)
        }
        d.photos.forEach {
            ref("Photo plant", it.plantId, plants)
            ref("Photo tank", it.aquariumId, tanks)
            ref("Photo fish", it.fishId, fish)
            ref("Photo log", it.growthLogId, growth)
        }
        d.photos.forEach { if (it.filePath.contains("..") || it.filePath.startsWith("/")) add("Unsafe photo path.") }
    }.distinct().take(20)
}
