package com.aquagrow.di

import com.aquagrow.data.backup.BackupManager
import com.aquagrow.data.datastore.DataStoreUserPreferencesRepository
import com.aquagrow.data.repository.AquariumRepositoryImpl
import com.aquagrow.data.repository.GardenRepositoryImpl
import com.aquagrow.data.repository.InventoryRepositoryImpl
import com.aquagrow.data.repository.PhotoRepositoryImpl
import com.aquagrow.data.repository.ReminderLogRepositoryImpl
import com.aquagrow.data.repository.SettingsRepositoryImpl
import com.aquagrow.data.repository.TaskRepositoryImpl
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.BackupRepository
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.InventoryRepository
import com.aquagrow.domain.repository.PhotoRepository
import com.aquagrow.domain.repository.ReminderLogRepository
import com.aquagrow.domain.repository.ReminderScheduler
import com.aquagrow.domain.repository.SettingsRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.notifications.WorkManagerReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindUserPreferences(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository

    @Binds abstract fun bindGarden(impl: GardenRepositoryImpl): GardenRepository

    @Binds abstract fun bindAquarium(impl: AquariumRepositoryImpl): AquariumRepository

    @Binds abstract fun bindTasks(impl: TaskRepositoryImpl): TaskRepository

    @Binds abstract fun bindPhotos(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds abstract fun bindInventory(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler

    @Binds abstract fun bindSettings(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds abstract fun bindReminderLog(impl: ReminderLogRepositoryImpl): ReminderLogRepository

    @Binds abstract fun bindBackup(impl: BackupManager): BackupRepository

    @Binds abstract fun bindAssistant(impl: com.aquagrow.data.assistant.AssistantRepositoryImpl): com.aquagrow.domain.assistant.AssistantRepository

    @Binds abstract fun bindConnectivity(impl: com.aquagrow.data.assistant.AndroidConnectivityChecker): com.aquagrow.data.assistant.ConnectivityChecker

    @Binds abstract fun bindPhotoEncoder(impl: com.aquagrow.data.assistant.JpegPhotoEncoder): com.aquagrow.data.assistant.PhotoEncoder

    @Binds abstract fun bindSearch(impl: com.aquagrow.data.repository.SearchRepositoryImpl): com.aquagrow.domain.repository.SearchRepository
}
