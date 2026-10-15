package com.aquagrow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

/** Whole-table reads and bulk inserts used only by backup / restore. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM plants")
    suspend fun plants(): List<PlantEntity>

    @Query("SELECT * FROM plant_growth_logs")
    suspend fun growthLogs(): List<PlantGrowthLogEntity>

    @Query("SELECT * FROM watering_logs")
    suspend fun wateringLogs(): List<WateringLogEntity>

    @Query("SELECT * FROM fertilizer_logs")
    suspend fun fertilizerLogs(): List<FertilizerLogEntity>

    @Query("SELECT * FROM harvest_logs")
    suspend fun harvestLogs(): List<HarvestLogEntity>

    @Query("SELECT * FROM sowing_guides")
    suspend fun sowingGuides(): List<SowingGuideEntity>

    @Query("SELECT * FROM inventory_items")
    suspend fun inventory(): List<InventoryItemEntity>

    @Query("SELECT * FROM aquariums")
    suspend fun aquariums(): List<AquariumEntity>

    @Query("SELECT * FROM fish")
    suspend fun fish(): List<FishEntity>

    @Query("SELECT * FROM feeding_logs")
    suspend fun feedingLogs(): List<FeedingLogEntity>

    @Query("SELECT * FROM water_quality_logs")
    suspend fun waterTests(): List<WaterQualityLogEntity>

    @Query("SELECT * FROM maintenance_logs")
    suspend fun maintenanceLogs(): List<MaintenanceLogEntity>

    @Query("SELECT * FROM task_schedules")
    suspend fun tasks(): List<TaskScheduleEntity>

    @Query("SELECT * FROM task_occurrence_states")
    suspend fun occurrenceStates(): List<TaskOccurrenceStateEntity>

    @Query("SELECT * FROM reminder_logs")
    suspend fun reminderLogs(): List<ReminderLogEntity>

    @Query("SELECT * FROM notification_settings")
    suspend fun notificationSettings(): List<NotificationSettingsEntity>

    @Query("SELECT * FROM expenses")
    suspend fun expenses(): List<ExpenseEntity>

    @Query("SELECT * FROM photo_records")
    suspend fun photos(): List<PhotoRecordEntity>

    // Inserts return new row ids in order (used to remap ids when merging).
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlants(rows: List<PlantEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGrowthLogs(rows: List<PlantGrowthLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWateringLogs(rows: List<WateringLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFertilizerLogs(rows: List<FertilizerLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHarvestLogs(rows: List<HarvestLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSowingGuides(rows: List<SowingGuideEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInventory(rows: List<InventoryItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAquariums(rows: List<AquariumEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFish(rows: List<FishEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFeedingLogs(rows: List<FeedingLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWaterTests(rows: List<WaterQualityLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMaintenanceLogs(rows: List<MaintenanceLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTasks(rows: List<TaskScheduleEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrenceStates(rows: List<TaskOccurrenceStateEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReminderLogs(rows: List<ReminderLogEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationSettings(rows: List<NotificationSettingsEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpenses(rows: List<ExpenseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhotos(rows: List<PhotoRecordEntity>): List<Long>
}
