@file:UseSerializers(
    com.aquagrow.data.backup.InstantSerializer::class,
    com.aquagrow.data.backup.LocalDateSerializer::class,
    com.aquagrow.data.backup.LocalTimeSerializer::class,
)

package com.aquagrow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.TankType
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Entity(tableName = "aquariums", indices = [Index("archived")])
@Serializable
data class AquariumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lengthCm: Double?,
    val widthCm: Double?,
    val heightCm: Double?,
    val volumeLiters: Double?,
    /** True when the user typed the volume instead of using the dimension calculation. */
    val volumeIsManual: Boolean = false,
    val tankType: TankType,
    val setupDate: LocalDate?,
    val location: String?,
    val filterType: String?,
    val heater: String?,
    val lighting: String?,
    val substrate: String?,
    val hasLivePlants: Boolean = false,
    val livePlantsNote: String?,
    val otherEquipment: String?,
    val coverPhotoPath: String?,
    val notes: String?,
    // The user's own reference ranges. Null = not tracked.
    val tempMinC: Double?,
    val tempMaxC: Double?,
    val phMin: Double?,
    val phMax: Double?,
    val ammoniaMaxPpm: Double?,
    val nitriteMaxPpm: Double?,
    val nitrateMaxPpm: Double?,
    val ghMin: Double?,
    val ghMax: Double?,
    val khMin: Double?,
    val khMax: Double?,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "fish",
    foreignKeys = [
        ForeignKey(entity = AquariumEntity::class, parentColumns = ["id"], childColumns = ["aquariumId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("aquariumId"), Index("speciesKey")],
)
@Serializable
data class FishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aquariumId: Long,
    /** Catalog key (e.g. "guppy"); null for custom species. */
    val speciesKey: String?,
    val speciesName: String,
    val nickname: String?,
    val count: Int,
    val addedDate: LocalDate?,
    val ageText: String?,
    val sizeCm: Double?,
    val photoPath: String?,
    val feedingNeeds: String?,
    val compatibilityNotes: String?,
    /** What the user observed. Never interpreted as a diagnosis. */
    val healthObservations: String?,
    val careNotes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "feeding_logs",
    foreignKeys = [
        ForeignKey(entity = AquariumEntity::class, parentColumns = ["id"], childColumns = ["aquariumId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FishEntity::class, parentColumns = ["id"], childColumns = ["fishId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = TaskScheduleEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("aquariumId"), Index("fishId"), Index("taskId"), Index("fedAt")],
)
@Serializable
data class FeedingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aquariumId: Long,
    val fishId: Long?,
    val taskId: Long?,
    val fedAt: Instant,
    val foodName: String?,
    val amount: String?,
    val skipped: Boolean = false,
    val note: String?,
)

@Entity(
    tableName = "water_quality_logs",
    foreignKeys = [
        ForeignKey(entity = AquariumEntity::class, parentColumns = ["id"], childColumns = ["aquariumId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("aquariumId"), Index("testedAt")],
)
@Serializable
data class WaterQualityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aquariumId: Long,
    val testedAt: Instant,
    // Every reading is optional: people rarely test all parameters at once.
    val temperatureC: Double?,
    val ph: Double?,
    val ammoniaPpm: Double?,
    val nitritePpm: Double?,
    val nitratePpm: Double?,
    val ghDgh: Double?,
    val khDkh: Double?,
    val note: String?,
)

@Entity(
    tableName = "maintenance_logs",
    foreignKeys = [
        ForeignKey(entity = AquariumEntity::class, parentColumns = ["id"], childColumns = ["aquariumId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TaskScheduleEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("aquariumId"), Index("taskId"), Index("performedAt")],
)
@Serializable
data class MaintenanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aquariumId: Long,
    val taskId: Long?,
    val performedAt: Instant,
    val type: MaintenanceType,
    val waterChangePercent: Int?,
    val note: String?,
)
