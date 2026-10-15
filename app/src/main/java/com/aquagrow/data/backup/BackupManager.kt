package com.aquagrow.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.aquagrow.BuildConfig
import com.aquagrow.data.local.dao.BackupDao
import com.aquagrow.data.local.db.AquaGrowDatabase
import com.aquagrow.data.photo.PhotoStorage
import com.aquagrow.domain.model.ModuleChoice
import com.aquagrow.domain.repository.BackupPreview
import com.aquagrow.domain.repository.BackupRepository
import com.aquagrow.domain.repository.ReminderScheduler
import com.aquagrow.domain.repository.RestoreMode
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.schedule.IcsWriter
import com.aquagrow.domain.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupException(message: String) : Exception(message)

@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AquaGrowDatabase,
    private val dao: BackupDao,
    private val prefs: UserPreferencesRepository,
    private val photos: PhotoStorage,
    private val tasks: TaskRepository,
    private val reminders: ReminderScheduler,
    private val time: TimeProvider,
) : BackupRepository {

    // ---------- Export ----------

    suspend fun snapshot(): BackupFile {
        val p = prefs.preferences.first()
        return BackupFile(
            schemaVersion = SCHEMA_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = time.now(),
            preferences = BackupPreferences(p.gardenEnabled, p.aquariumEnabled, p.aiAssistantEnabled, p.themeMode, p.currencyCode, p.displayName),
            data = BackupData(
                plants = dao.plants(), growthLogs = dao.growthLogs(), wateringLogs = dao.wateringLogs(),
                fertilizerLogs = dao.fertilizerLogs(), harvestLogs = dao.harvestLogs(), sowingGuides = dao.sowingGuides(),
                inventory = dao.inventory(), aquariums = dao.aquariums(), fish = dao.fish(), feedingLogs = dao.feedingLogs(),
                waterTests = dao.waterTests(), maintenanceLogs = dao.maintenanceLogs(), tasks = dao.tasks(),
                occurrenceStates = dao.occurrenceStates(), reminderLogs = dao.reminderLogs(),
                notificationSettings = dao.notificationSettings(), expenses = dao.expenses(), photos = dao.photos(),
            ),
        )
    }

    override suspend fun export(uri: String, includePhotos: Boolean): Map<String, Int> = withContext(Dispatchers.IO) {
        val file = snapshot()
        val json = BackupFile.json.encodeToString(BackupFile.serializer(), file)
        val out = context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: throw BackupException("Could not open the file for writing.")
        out.use { stream ->
            if (!includePhotos) {
                stream.write(json.toByteArray(Charsets.UTF_8))
            } else {
                ZipOutputStream(stream).use { zip ->
                    zip.putNextEntry(ZipEntry(JSON_ENTRY))
                    zip.write(json.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    file.data.photos.map { it.filePath }.distinct().forEach { path ->
                        val f = photos.resolve(path)
                        if (f.isFile) {
                            zip.putNextEntry(ZipEntry(path))
                            f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
        }
        file.data.counts
    }

    // ---------- Import ----------

    private class Parsed(val file: BackupFile, val photoFiles: Map<String, ByteArray>)

    private suspend fun read(uri: String): Parsed = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri.toUri()) ?: throw BackupException("Could not open the file.")
        input.use { parse(BufferedInputStream(it)) }
    }

    private fun parse(stream: InputStream): Parsed {
        stream.mark(4)
        val magic = ByteArray(2).also { stream.read(it) }
        stream.reset()
        val isZip = magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte()
        var jsonText: String? = null
        val photoFiles = mutableMapOf<String, ByteArray>()
        if (isZip) {
            ZipInputStream(stream).use { zip ->
                var total = 0L
                while (true) {
                    val e = zip.nextEntry ?: break
                    val bytes = zip.readBytes()
                    total += bytes.size
                    if (total > MAX_ZIP_BYTES) throw BackupException("Backup is too large.")
                    when {
                        e.name == JSON_ENTRY -> jsonText = bytes.toString(Charsets.UTF_8)
                        e.name.startsWith("${PhotoStorage.DIR}/") && !e.name.contains("..") -> photoFiles[e.name] = bytes
                    }
                }
            }
        } else {
            val bytes = stream.readBytes()
            if (bytes.size > MAX_JSON_BYTES) throw BackupException("Backup is too large.")
            jsonText = bytes.toString(Charsets.UTF_8)
        }
        val text = jsonText ?: throw BackupException("No backup data found in this file.")
        val file = try {
            BackupFile.json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw BackupException("This file isn't a valid AquaGrow backup.")
        }
        val errors = BackupValidator.validate(file, SCHEMA_VERSION)
        if (errors.isNotEmpty()) throw BackupException(errors.joinToString("\n"))
        return Parsed(file, photoFiles)
    }

    override suspend fun inspect(uri: String): BackupPreview {
        val p = read(uri)
        return BackupPreview(p.file.exportedAt, p.file.appVersion, p.file.data.counts, p.photoFiles.size)
    }

    override suspend fun restore(uri: String, mode: RestoreMode): Map<String, Int> {
        val p = read(uri)
        withContext(Dispatchers.IO) {
            p.photoFiles.forEach { (path, bytes) ->
                val target = photos.resolve(path)
                if (!target.exists()) {
                    target.parentFile?.mkdirs()
                    target.writeBytes(bytes)
                }
            }
        }
        when (mode) {
            RestoreMode.REPLACE -> replace(p.file)
            RestoreMode.MERGE -> merge(p.file.data)
        }
        // Restoring on a fresh install (from Welcome) counts as finishing setup.
        val now = prefs.preferences.first()
        if (!now.onboardingCompleted) {
            prefs.completeOnboarding(
                when {
                    now.gardenEnabled && !now.aquariumEnabled -> ModuleChoice.GARDEN
                    now.aquariumEnabled && !now.gardenEnabled -> ModuleChoice.AQUARIUM
                    else -> ModuleChoice.BOTH
                },
            )
        }
        reminders.requestReschedule()
        return p.file.data.counts
    }

    /** Wipes user data and restores exactly what is in the backup (same ids). */
    suspend fun replace(file: BackupFile) {
        val d = file.data
        db.withTransaction {
            val w = db.openHelper.writableDatabase
            // Children before parents (foreign keys are enforced).
            listOf(
                "reminder_logs", "task_occurrence_states", "photo_records", "expenses", "feeding_logs", "water_quality_logs",
                "maintenance_logs", "watering_logs", "fertilizer_logs", "harvest_logs", "plant_growth_logs", "task_schedules",
                "fish", "inventory_items", "aquariums", "plants", "sowing_guides", "notification_settings",
            ).forEach { w.execSQL("DELETE FROM $it") }
            dao.insertPlants(d.plants)
            dao.insertAquariums(d.aquariums)
            dao.insertSowingGuides(d.sowingGuides)
            dao.insertInventory(d.inventory)
            dao.insertFish(d.fish)
            dao.insertTasks(d.tasks)
            dao.insertGrowthLogs(d.growthLogs)
            dao.insertWateringLogs(d.wateringLogs)
            dao.insertFertilizerLogs(d.fertilizerLogs)
            dao.insertHarvestLogs(d.harvestLogs)
            dao.insertFeedingLogs(d.feedingLogs)
            dao.insertWaterTests(d.waterTests)
            dao.insertMaintenanceLogs(d.maintenanceLogs)
            dao.insertOccurrenceStates(d.occurrenceStates)
            dao.insertReminderLogs(d.reminderLogs)
            dao.insertExpenses(d.expenses)
            dao.insertPhotos(d.photos)
            dao.insertNotificationSettings(d.notificationSettings)
        }
        val bp = file.preferences
        prefs.setGardenEnabled(bp.gardenEnabled)
        prefs.setAquariumEnabled(bp.aquariumEnabled)
        prefs.setAiAssistantEnabled(bp.aiAssistantEnabled)
        prefs.setThemeMode(bp.themeMode)
        prefs.setCurrencyCode(bp.currencyCode)
        prefs.setDisplayName(bp.displayName)
    }

    /**
     * Adds everything from the backup as new rows, remapping ids so nothing collides with
     * existing data. Settings, reminder history and duplicate sowing-guide entries are kept as-is.
     */
    suspend fun merge(d: BackupData) {
        db.withTransaction {
            fun map(old: List<Long>, new: List<Long>) = old.zip(new).toMap()
            val plantIds = map(d.plants.map { it.id }, dao.insertPlants(d.plants.map { it.copy(id = 0) }))
            val tankIds = map(d.aquariums.map { it.id }, dao.insertAquariums(d.aquariums.map { it.copy(id = 0) }))
            val itemIds = map(d.inventory.map { it.id }, dao.insertInventory(d.inventory.map { it.copy(id = 0, aquariumId = it.aquariumId?.let(tankIds::get)) }))
            val fishIds = map(d.fish.map { it.id }, dao.insertFish(d.fish.map { it.copy(id = 0, aquariumId = tankIds.getValue(it.aquariumId)) }))
            val taskIds = map(
                d.tasks.map { it.id },
                dao.insertTasks(d.tasks.map { it.copy(id = 0, plantId = it.plantId?.let(plantIds::get), aquariumId = it.aquariumId?.let(tankIds::get), fishId = it.fishId?.let(fishIds::get)) }),
            )
            val growthIds = map(d.growthLogs.map { it.id }, dao.insertGrowthLogs(d.growthLogs.map { it.copy(id = 0, plantId = plantIds.getValue(it.plantId)) }))
            dao.insertWateringLogs(d.wateringLogs.map { it.copy(id = 0, plantId = plantIds.getValue(it.plantId), taskId = it.taskId?.let(taskIds::get)) })
            dao.insertFertilizerLogs(d.fertilizerLogs.map { it.copy(id = 0, plantId = plantIds.getValue(it.plantId), inventoryItemId = it.inventoryItemId?.let(itemIds::get), taskId = it.taskId?.let(taskIds::get)) })
            dao.insertHarvestLogs(d.harvestLogs.map { it.copy(id = 0, plantId = plantIds.getValue(it.plantId)) })
            dao.insertFeedingLogs(d.feedingLogs.map { it.copy(id = 0, aquariumId = tankIds.getValue(it.aquariumId), fishId = it.fishId?.let(fishIds::get), taskId = it.taskId?.let(taskIds::get)) })
            dao.insertWaterTests(d.waterTests.map { it.copy(id = 0, aquariumId = tankIds.getValue(it.aquariumId)) })
            dao.insertMaintenanceLogs(d.maintenanceLogs.map { it.copy(id = 0, aquariumId = tankIds.getValue(it.aquariumId), taskId = it.taskId?.let(taskIds::get)) })
            dao.insertOccurrenceStates(d.occurrenceStates.map { it.copy(id = 0, taskId = taskIds.getValue(it.taskId)) })
            dao.insertExpenses(d.expenses.map { it.copy(id = 0, inventoryItemId = it.inventoryItemId?.let(itemIds::get), plantId = it.plantId?.let(plantIds::get), aquariumId = it.aquariumId?.let(tankIds::get)) })
            dao.insertPhotos(
                d.photos.map {
                    it.copy(
                        id = 0,
                        plantId = it.plantId?.let(plantIds::get),
                        aquariumId = it.aquariumId?.let(tankIds::get),
                        fishId = it.fishId?.let(fishIds::get),
                        growthLogId = it.growthLogId?.let(growthIds::get),
                    )
                },
            )
            val existing = dao.sowingGuides().map { Triple(it.plantName.lowercase(), it.season, it.startMonth to it.endMonth) }.toSet()
            dao.insertSowingGuides(d.sowingGuides.filter { Triple(it.plantName.lowercase(), it.season, it.startMonth to it.endMonth) !in existing }.map { it.copy(id = 0) })
        }
    }

    override suspend fun exportIcs(uri: String): Int = withContext(Dispatchers.IO) {
        val list = tasks.getTasksForExport()
        val text = IcsWriter.write(list, time.zone(), time.now())
        val out = context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: throw BackupException("Could not open the file for writing.")
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        list.size
    }

    /** Deletes photo files no longer referenced anywhere (left by deleted plants or cancelled forms). */
    suspend fun cleanOrphanPhotos() = withContext(Dispatchers.IO) {
        val referenced = buildSet {
            dao.photos().forEach { add(it.filePath) }
            dao.plants().forEach { p -> p.coverPhotoPath?.let(::add) }
            dao.aquariums().forEach { a -> a.coverPhotoPath?.let(::add) }
            dao.fish().forEach { f -> f.photoPath?.let(::add) }
        }
        val dayAgo = time.now().toEpochMilli() - 24L * 3600 * 1000
        photos.allFiles()
            .filter { "${PhotoStorage.DIR}/${it.name}" !in referenced && it.lastModified() < dayAgo }
            .forEach(File::delete)
    }

    companion object {
        /** Keep in sync with @Database(version). */
        const val SCHEMA_VERSION = 1
        const val JSON_ENTRY = "backup.json"
        private const val MAX_JSON_BYTES = 50L * 1024 * 1024
        private const val MAX_ZIP_BYTES = 1024L * 1024 * 1024
    }
}
