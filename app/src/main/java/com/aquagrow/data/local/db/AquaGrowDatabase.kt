package com.aquagrow.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aquagrow.data.local.dao.AquariumDao
import com.aquagrow.data.local.dao.AquariumLogDao
import com.aquagrow.data.local.dao.BackupDao
import com.aquagrow.data.local.dao.ExpenseDao
import com.aquagrow.data.local.dao.FishDao
import com.aquagrow.data.local.dao.GardenLogDao
import com.aquagrow.data.local.dao.InventoryDao
import com.aquagrow.data.local.dao.NotificationSettingsDao
import com.aquagrow.data.local.dao.PhotoDao
import com.aquagrow.data.local.dao.PlantDao
import com.aquagrow.data.local.dao.ReminderLogDao
import com.aquagrow.data.local.dao.SowingGuideDao
import com.aquagrow.data.local.dao.TaskDao
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

@Database(
    entities = [
        PlantEntity::class,
        PlantGrowthLogEntity::class,
        WateringLogEntity::class,
        FertilizerLogEntity::class,
        HarvestLogEntity::class,
        InventoryItemEntity::class,
        AquariumEntity::class,
        FishEntity::class,
        FeedingLogEntity::class,
        WaterQualityLogEntity::class,
        MaintenanceLogEntity::class,
        TaskScheduleEntity::class,
        TaskOccurrenceStateEntity::class,
        ReminderLogEntity::class,
        SowingGuideEntity::class,
        NotificationSettingsEntity::class,
        ExpenseEntity::class,
        PhotoRecordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AquaGrowDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun gardenLogDao(): GardenLogDao
    abstract fun aquariumDao(): AquariumDao
    abstract fun fishDao(): FishDao
    abstract fun aquariumLogDao(): AquariumLogDao
    abstract fun taskDao(): TaskDao
    abstract fun notificationSettingsDao(): NotificationSettingsDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun photoDao(): PhotoDao
    abstract fun sowingGuideDao(): SowingGuideDao
    abstract fun reminderLogDao(): ReminderLogDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "aquagrow.db"
    }
}
