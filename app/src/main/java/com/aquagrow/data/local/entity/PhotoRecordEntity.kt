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
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * A photo copied into app-private storage. It belongs to a plant, aquarium or fish group,
 * and may additionally be attached to a growth log.
 */
@Entity(
    tableName = "photo_records",
    foreignKeys = [
        ForeignKey(entity = PlantEntity::class, parentColumns = ["id"], childColumns = ["plantId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AquariumEntity::class, parentColumns = ["id"], childColumns = ["aquariumId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FishEntity::class, parentColumns = ["id"], childColumns = ["fishId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PlantGrowthLogEntity::class, parentColumns = ["id"], childColumns = ["growthLogId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("plantId"), Index("aquariumId"), Index("fishId"), Index("growthLogId"), Index("takenAt")],
)
@Serializable
data class PhotoRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Path relative to the app's files dir, so backups restore cleanly. */
    val filePath: String,
    val takenAt: Instant,
    val caption: String?,
    val plantId: Long?,
    val aquariumId: Long?,
    val fishId: Long?,
    val growthLogId: Long?,
)
