@file:UseSerializers(
    com.aquagrow.data.backup.InstantSerializer::class,
    com.aquagrow.data.backup.LocalDateSerializer::class,
    com.aquagrow.data.backup.LocalTimeSerializer::class,
)

package com.aquagrow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/** Single-row table (id is always [SINGLETON_ID]). */
@Entity(tableName = "notification_settings")
@Serializable
data class NotificationSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val remindersEnabled: Boolean = true,
    val defaultReminderTime: LocalTime = LocalTime.of(7, 0),
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(6, 0),
    /** Names of reminder channels the user switched off (see ReminderChannel). */
    val disabledChannels: List<String> = emptyList(),
    val missedRemindersEnabled: Boolean = true,
    val inventoryRemindersEnabled: Boolean = true,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
