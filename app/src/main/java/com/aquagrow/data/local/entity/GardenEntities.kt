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
import com.aquagrow.domain.model.FertilizerMethod
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.PlantCondition
import com.aquagrow.domain.model.PlantingMethod
import com.aquagrow.domain.model.Season
import com.aquagrow.domain.model.SoilMoisture
import com.aquagrow.domain.model.SunlightLevel
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Entity(
    tableName = "plants",
    indices = [Index("archived"), Index("category")],
)
@Serializable
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val variety: String?,
    /** Key into the built-in plant catalog (e.g. "tomato"); null for custom plants. */
    val catalogKey: String?,
    val category: PlantCategory,
    val plantingMethod: PlantingMethod?,
    val sowingDate: LocalDate?,
    val plantingDate: LocalDate,
    val potSize: String?,
    val soilMix: String?,
    val location: String?,
    val sunlight: SunlightLevel?,
    val condition: PlantCondition?,
    /** Ordered, user-customisable stage names, e.g. Seed → Seedling → … → Harvest. */
    val growthStages: List<String>,
    val currentStageIndex: Int = 0,
    /** Rough range of days from planting to first harvest/bloom. Always shown as an estimate. */
    val harvestDaysMin: Int?,
    val harvestDaysMax: Int?,
    val coverPhotoPath: String?,
    val notes: String?,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "plant_growth_logs",
    foreignKeys = [
        ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("plantId"), Index("loggedAt")],
)
@Serializable
data class PlantGrowthLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val loggedAt: Instant,
    /** Stage at the time of this log, if the user set one. */
    val stageIndex: Int?,
    val stageName: String?,
    val heightCm: Double?,
    val condition: PlantCondition?,
    val note: String?,
)

@Entity(
    tableName = "watering_logs",
    foreignKeys = [
        ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TaskScheduleEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("plantId"), Index("taskId"), Index("loggedAt")],
)
@Serializable
data class WateringLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val taskId: Long?,
    val loggedAt: Instant,
    /** False = the user checked the soil and decided not to water. */
    val watered: Boolean,
    val soilMoisture: SoilMoisture?,
    val amountMl: Int?,
    val note: String?,
)

@Entity(
    tableName = "fertilizer_logs",
    foreignKeys = [
        ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = InventoryItemEntity::class, parentColumns = ["id"], childColumns = ["inventoryItemId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = TaskScheduleEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("plantId"), Index("inventoryItemId"), Index("taskId"), Index("appliedAt")],
)
@Serializable
data class FertilizerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val inventoryItemId: Long?,
    val taskId: Long?,
    val appliedAt: Instant,
    val type: FertilizerType,
    val productName: String?,
    /** What the user measured (follow the product label). The app never suggests amounts. */
    val quantity: Double?,
    val unit: String?,
    val method: FertilizerMethod?,
    val note: String?,
)

@Entity(
    tableName = "harvest_logs",
    foreignKeys = [
        ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("plantId"), Index("harvestedAt")],
)
@Serializable
data class HarvestLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val harvestedAt: Instant,
    val quantity: Double,
    /** e.g. "g", "kg", "pcs", "bunch". */
    val unit: String,
    val note: String?,
)

/** Editable seasonal sowing guidance (seeded from the catalog on first launch). */
@Entity(tableName = "sowing_guides", indices = [Index("season")])
@Serializable
data class SowingGuideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catalogKey: String?,
    val plantName: String,
    val season: Season,
    /** 1..12; a window may wrap the year end (e.g. 11 → 1). */
    val startMonth: Int,
    val endMonth: Int,
    val notes: String?,
    val userEdited: Boolean = false,
)
