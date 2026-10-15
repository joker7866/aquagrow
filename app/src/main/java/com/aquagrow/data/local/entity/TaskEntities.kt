@file:UseSerializers(
    com.aquagrow.data.backup.InstantSerializer::class,
    com.aquagrow.data.backup.LocalDateSerializer::class,
    com.aquagrow.data.backup.LocalTimeSerializer::class,
)

package com.aquagrow.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.RecurrenceType
import com.aquagrow.domain.model.ReminderKind
import com.aquagrow.domain.model.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * A one-off or repeating care task, optionally tied to a plant, aquarium or fish group.
 * Occurrences are computed from the recurrence columns (see RecurrenceEngine), never stored.
 */
@Entity(
    tableName = "task_schedules",
    foreignKeys = [
        ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AquariumEntity::class, parentColumns = ["id"], childColumns = ["aquariumId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FishEntity::class, parentColumns = ["id"], childColumns = ["fishId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("plantId"), Index("aquariumId"), Index("fishId"), Index("module"), Index("type")],
)
@Serializable
data class TaskScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val module: CareModule,
    val type: TaskType,
    val plantId: Long?,
    val aquariumId: Long?,
    val fishId: Long?,
    val maintenanceType: MaintenanceType?,
    val fertilizerType: FertilizerType?,
    val itemName: String?,
    val amountText: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    /** Null = use the default reminder time from notification settings. */
    val reminderTime: LocalTime?,
    val recurrenceType: RecurrenceType,
    val recurrenceInterval: Int,
    /** Bit i set = DayOfWeek.of(i + 1) (bit 0 = Monday). */
    val recurrenceDaysOfWeek: Int,
    val recurrenceDayOfMonth: Int?,
    val enabled: Boolean = true,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * What the user did to one occurrence. The unique (taskId, occurrenceDate) index means an
 * occurrence can never be recorded twice, which is also what keeps reminders from duplicating.
 */
@Entity(
    tableName = "task_occurrence_states",
    foreignKeys = [
        ForeignKey(entity = TaskScheduleEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["taskId", "occurrenceDate"], unique = true),
        Index("rescheduledDate"),
        Index("handledAt"),
    ],
)
@Serializable
data class TaskOccurrenceStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    /** The date the recurrence rule produced (stable key). */
    val occurrenceDate: LocalDate,
    val status: OccurrenceStatus,
    val rescheduledDate: LocalDate?,
    val snoozedUntil: Instant?,
    val handledAt: Instant?,
)

/** Task row joined with the display name of whatever it is linked to. */
data class TaskWithSubject(
    @Embedded val task: TaskScheduleEntity,
    val plantName: String?,
    val aquariumName: String?,
    val fishName: String?,
)

/** Reminder history (Profile → Reminders → History). [dedupeKey] stops repeat posts. */
@Entity(
    tableName = "reminder_logs",
    foreignKeys = [
        ForeignKey(entity = TaskScheduleEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = InventoryItemEntity::class, parentColumns = ["id"], childColumns = ["inventoryItemId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["dedupeKey"], unique = true), Index("taskId"), Index("inventoryItemId"), Index("postedAt")],
)
@Serializable
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: ReminderKind,
    val taskId: Long?,
    val occurrenceDate: LocalDate?,
    val inventoryItemId: Long?,
    val title: String,
    val text: String,
    val postedAt: Instant,
    val dedupeKey: String,
)
