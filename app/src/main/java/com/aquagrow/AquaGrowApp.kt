package com.aquagrow

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aquagrow.data.seed.ReferenceDataSeeder
import com.aquagrow.di.ApplicationScope
import com.aquagrow.notifications.ReminderNotifier
import com.aquagrow.notifications.WorkManagerReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class AquaGrowApp :
    Application(),
    Configuration.Provider {

    @Inject lateinit var seeder: ReferenceDataSeeder

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var notifier: ReminderNotifier

    @Inject lateinit var reminderScheduler: WorkManagerReminderScheduler

    @Inject lateinit var backupManager: com.aquagrow.data.backup.BackupManager

    /** WorkManager is initialised on demand with Hilt's factory (default initializer removed in the manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        appScope.launch {
            seeder.seedIfNeeded()
            reminderScheduler.ensurePeriodicRefresh()
            reminderScheduler.requestReschedule()
            runCatching { backupManager.cleanOrphanPhotos() }
        }
    }
}
