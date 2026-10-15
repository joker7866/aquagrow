package com.aquagrow.data.repository

import com.aquagrow.data.local.dao.NotificationSettingsDao
import com.aquagrow.data.local.dao.ReminderLogDao
import com.aquagrow.data.local.entity.ReminderLogEntity
import com.aquagrow.data.mapper.toDomain
import com.aquagrow.data.mapper.toEntity
import com.aquagrow.domain.model.NotificationSettings
import com.aquagrow.domain.model.ReminderKind
import com.aquagrow.domain.repository.ReminderLogRepository
import com.aquagrow.domain.repository.ReminderScheduler
import com.aquagrow.domain.repository.SettingsRepository
import com.aquagrow.domain.time.TimeProvider
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: NotificationSettingsDao,
    private val reminders: ReminderScheduler,
) : SettingsRepository {
    private val mutex = Mutex()

    override val notificationSettings: Flow<NotificationSettings> =
        dao.observe().map { it?.toDomain() ?: NotificationSettings() }

    override suspend fun getNotificationSettings(): NotificationSettings = dao.get()?.toDomain() ?: NotificationSettings()

    override suspend fun updateNotificationSettings(transform: (NotificationSettings) -> NotificationSettings) {
        mutex.withLock { dao.upsert(transform(getNotificationSettings()).toEntity()) }
        reminders.requestReschedule()
    }
}

@Singleton
class ReminderLogRepositoryImpl @Inject constructor(
    private val dao: ReminderLogDao,
    private val time: TimeProvider,
) : ReminderLogRepository {
    override fun observeRecent(limit: Int) = dao.observeRecent(limit).map { l -> l.map { it.toDomain() } }

    override suspend fun recordIfNew(
        kind: ReminderKind,
        taskId: Long?,
        occurrenceDate: LocalDate?,
        inventoryItemId: Long?,
        title: String,
        text: String,
        dedupeKey: String,
    ): Boolean {
        // Keep history bounded: drop entries older than 90 days.
        dao.deleteOlderThan(time.now().minusSeconds(90L * 24 * 3600))
        return dao.insert(
            ReminderLogEntity(
                kind = kind,
                taskId = taskId,
                occurrenceDate = occurrenceDate,
                inventoryItemId = inventoryItemId,
                title = title,
                text = text,
                postedAt = time.now(),
                dedupeKey = dedupeKey,
            ),
        ) != -1L
    }

    override suspend fun clear() = dao.clear()
}
