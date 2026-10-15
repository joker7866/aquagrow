package com.aquagrow.di

import android.content.Context
import androidx.room.Room
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
import com.aquagrow.data.local.db.ALL_MIGRATIONS
import com.aquagrow.data.local.db.AquaGrowDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // No fallbackToDestructiveMigration: user data must never be wiped silently.
    // Every schema change ships with a Migration (schemas are exported to app/schemas).
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AquaGrowDatabase = Room.databaseBuilder(context, AquaGrowDatabase::class.java, AquaGrowDatabase.NAME)
        .addMigrations(*ALL_MIGRATIONS)
        .build()

    @Provides fun providePlantDao(db: AquaGrowDatabase): PlantDao = db.plantDao()

    @Provides fun provideGardenLogDao(db: AquaGrowDatabase): GardenLogDao = db.gardenLogDao()

    @Provides fun provideAquariumDao(db: AquaGrowDatabase): AquariumDao = db.aquariumDao()

    @Provides fun provideFishDao(db: AquaGrowDatabase): FishDao = db.fishDao()

    @Provides fun provideAquariumLogDao(db: AquaGrowDatabase): AquariumLogDao = db.aquariumLogDao()

    @Provides fun provideTaskDao(db: AquaGrowDatabase): TaskDao = db.taskDao()

    @Provides fun provideNotificationSettingsDao(db: AquaGrowDatabase): NotificationSettingsDao = db.notificationSettingsDao()

    @Provides fun provideInventoryDao(db: AquaGrowDatabase): InventoryDao = db.inventoryDao()

    @Provides fun provideExpenseDao(db: AquaGrowDatabase): ExpenseDao = db.expenseDao()

    @Provides fun providePhotoDao(db: AquaGrowDatabase): PhotoDao = db.photoDao()

    @Provides fun provideSowingGuideDao(db: AquaGrowDatabase): SowingGuideDao = db.sowingGuideDao()

    @Provides fun provideReminderLogDao(db: AquaGrowDatabase): ReminderLogDao = db.reminderLogDao()

    @Provides fun provideBackupDao(db: AquaGrowDatabase): BackupDao = db.backupDao()
}
