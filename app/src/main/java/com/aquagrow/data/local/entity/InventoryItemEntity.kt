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
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.InventoryCategory
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * Garden inventory and aquarium supplies share one table, split by [module].
 * Items with an [aquariumId] are equipment installed in that tank (Tank detail → Equipment).
 */
@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(entity = AquariumEntity::class, parentColumns = ["id"], childColumns = ["aquariumId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("module"), Index("aquariumId"), Index("expiryDate")],
)
@Serializable
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val module: CareModule,
    val name: String,
    val category: InventoryCategory,
    val quantity: Double?,
    val unit: String?,
    val lowStockThreshold: Double?,
    val purchaseDate: LocalDate?,
    /** Purchase cost in minor units (paise for INR). */
    val costMinor: Long?,
    val currencyCode: String?,
    val expiryDate: LocalDate?,
    val aquariumId: Long?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
