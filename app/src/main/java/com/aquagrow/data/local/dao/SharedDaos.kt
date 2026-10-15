package com.aquagrow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aquagrow.data.local.entity.ExpenseEntity
import com.aquagrow.data.local.entity.InventoryItemEntity
import com.aquagrow.data.local.entity.NotificationSettingsEntity
import com.aquagrow.data.local.entity.PhotoRecordEntity
import com.aquagrow.data.local.entity.ReminderLogEntity
import com.aquagrow.data.local.entity.TaskOccurrenceStateEntity
import com.aquagrow.data.local.entity.TaskScheduleEntity
import com.aquagrow.data.local.entity.TaskWithSubject
import com.aquagrow.domain.model.CareModule
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

private const val TASK_WITH_SUBJECT = """
    SELECT t.*, p.name AS plantName, a.name AS aquariumName,
           COALESCE(f.nickname, f.speciesName) AS fishName
    FROM task_schedules t
    LEFT JOIN plants p ON p.id = t.plantId
    LEFT JOIN aquariums a ON a.id = t.aquariumId
    LEFT JOIN fish f ON f.id = t.fishId
"""

// Tasks of archived plants/tanks are hidden everywhere.
private const val ACTIVE_FILTER = """
    t.enabled = 1 AND (p.id IS NULL OR p.archived = 0) AND (a.id IS NULL OR a.archived = 0)
"""

@Dao
interface TaskDao {
    @Query("$TASK_WITH_SUBJECT WHERE $ACTIVE_FILTER")
    fun observeActive(): Flow<List<TaskWithSubject>>

    @Query("$TASK_WITH_SUBJECT WHERE $ACTIVE_FILTER")
    suspend fun getActive(): List<TaskWithSubject>

    @Query("$TASK_WITH_SUBJECT WHERE t.id = :id")
    suspend fun getWithSubject(id: Long): TaskWithSubject?

    @Query("$TASK_WITH_SUBJECT WHERE t.plantId = :plantId ORDER BY t.type")
    fun observeForPlant(plantId: Long): Flow<List<TaskWithSubject>>

    @Query("$TASK_WITH_SUBJECT WHERE t.aquariumId = :aquariumId ORDER BY t.type, t.reminderTime")
    fun observeForAquarium(aquariumId: Long): Flow<List<TaskWithSubject>>

    @Query("$TASK_WITH_SUBJECT WHERE t.title LIKE '%' || :q || '%' OR t.notes LIKE '%' || :q || '%' LIMIT 50")
    suspend fun search(q: String): List<TaskWithSubject>

    @Query("SELECT * FROM task_schedules WHERE id = :id")
    suspend fun getById(id: Long): TaskScheduleEntity?

    @Query("$TASK_WITH_SUBJECT WHERE t.plantId = :plantId")
    suspend fun getForPlant(plantId: Long): List<TaskWithSubject>

    @Query("$TASK_WITH_SUBJECT WHERE t.aquariumId = :aquariumId")
    suspend fun getForAquarium(aquariumId: Long): List<TaskWithSubject>

    @Insert
    suspend fun insert(task: TaskScheduleEntity): Long

    @Upsert
    suspend fun upsert(task: TaskScheduleEntity): Long

    @Delete
    suspend fun delete(task: TaskScheduleEntity)

    @Query("DELETE FROM task_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ---- Occurrence states ----

    @Query(
        """
        SELECT * FROM task_occurrence_states
        WHERE occurrenceDate BETWEEN :from AND :to OR rescheduledDate BETWEEN :from AND :to
        """,
    )
    fun observeStates(from: LocalDate, to: LocalDate): Flow<List<TaskOccurrenceStateEntity>>

    @Query(
        """
        SELECT * FROM task_occurrence_states
        WHERE occurrenceDate BETWEEN :from AND :to OR rescheduledDate BETWEEN :from AND :to
        """,
    )
    suspend fun getStates(from: LocalDate, to: LocalDate): List<TaskOccurrenceStateEntity>

    @Query("SELECT * FROM task_occurrence_states WHERE taskId = :taskId AND occurrenceDate = :date")
    suspend fun getState(taskId: Long, date: LocalDate): TaskOccurrenceStateEntity?

    @Query("SELECT * FROM task_occurrence_states WHERE taskId = :taskId ORDER BY occurrenceDate DESC LIMIT :limit")
    fun observeStatesForTask(taskId: Long, limit: Int): Flow<List<TaskOccurrenceStateEntity>>

    /** Insert or replace by the unique (taskId, occurrenceDate) key: never two rows per occurrence. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putState(state: TaskOccurrenceStateEntity)

    @Query("DELETE FROM task_occurrence_states WHERE taskId = :taskId AND occurrenceDate = :date")
    suspend fun deleteState(taskId: Long, date: LocalDate)
}

@Dao
interface ReminderLogDao {
    @Query("SELECT * FROM reminder_logs ORDER BY postedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ReminderLogEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM reminder_logs WHERE dedupeKey = :key)")
    suspend fun exists(key: String): Boolean

    /** Returns -1 when [ReminderLogEntity.dedupeKey] already exists, i.e. this was a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: ReminderLogEntity): Long

    @Query("DELETE FROM reminder_logs WHERE postedAt < :before")
    suspend fun deleteOlderThan(before: Instant)

    @Query("DELETE FROM reminder_logs")
    suspend fun clear()
}

@Dao
interface NotificationSettingsDao {
    @Query("SELECT * FROM notification_settings WHERE id = ${NotificationSettingsEntity.SINGLETON_ID}")
    fun observe(): Flow<NotificationSettingsEntity?>

    @Query("SELECT * FROM notification_settings WHERE id = ${NotificationSettingsEntity.SINGLETON_ID}")
    suspend fun get(): NotificationSettingsEntity?

    @Upsert
    suspend fun upsert(settings: NotificationSettingsEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE module = :module ORDER BY name COLLATE NOCASE")
    fun observeByModule(module: CareModule): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE aquariumId = :aquariumId ORDER BY name COLLATE NOCASE")
    fun observeEquipmentForAquarium(aquariumId: Long): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items")
    suspend fun getAll(): List<InventoryItemEntity>

    @Upsert
    suspend fun upsert(item: InventoryItemEntity): Long

    @Delete
    suspend fun delete(item: InventoryItemEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY spentOn DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE inventoryItemId = :itemId")
    suspend fun getForInventoryItem(itemId: Long): List<ExpenseEntity>

    @Upsert
    suspend fun upsert(expense: ExpenseEntity): Long

    @Delete
    suspend fun delete(expense: ExpenseEntity)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo_records ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<PhotoRecordEntity>>

    @Query("SELECT * FROM photo_records WHERE plantId = :plantId ORDER BY takenAt DESC")
    fun observeForPlant(plantId: Long): Flow<List<PhotoRecordEntity>>

    @Query("SELECT * FROM photo_records WHERE aquariumId = :aquariumId ORDER BY takenAt DESC")
    fun observeForAquarium(aquariumId: Long): Flow<List<PhotoRecordEntity>>

    @Query("SELECT * FROM photo_records WHERE fishId = :fishId ORDER BY takenAt DESC")
    fun observeForFish(fishId: Long): Flow<List<PhotoRecordEntity>>

    @Query("SELECT filePath FROM photo_records")
    suspend fun allPaths(): List<String>

    @Insert
    suspend fun insert(photo: PhotoRecordEntity): Long

    @Delete
    suspend fun delete(photo: PhotoRecordEntity)
}
