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
import com.aquagrow.domain.model.ExpenseCategory
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryItemId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AquariumEntity::class,
            parentColumns = ["id"],
            childColumns = ["aquariumId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("inventoryItemId"), Index("plantId"), Index("aquariumId"), Index("spentOn")],
)
@Serializable
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Null = shared / not tied to one module. */
    val module: CareModule?,
    val category: ExpenseCategory,
    /** Amount in minor units (paise for INR) to avoid floating-point money errors. */
    val amountMinor: Long,
    /** ISO 4217 code, e.g. "INR". */
    val currencyCode: String,
    val spentOn: LocalDate,
    val inventoryItemId: Long?,
    val plantId: Long?,
    val aquariumId: Long?,
    val note: String?,
    val createdAt: Instant,
)
