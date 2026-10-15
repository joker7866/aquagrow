package com.aquagrow.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class InventoryItem(
    val id: Long = 0,
    val module: CareModule,
    val name: String,
    val category: InventoryCategory,
    val quantity: Double? = null,
    val unit: String? = null,
    val lowStockThreshold: Double? = null,
    val purchaseDate: LocalDate? = null,
    val costMinor: Long? = null,
    val currencyCode: String? = null,
    val expiryDate: LocalDate? = null,
    val aquariumId: Long? = null,
    val notes: String? = null,
) {
    val isLowStock: Boolean
        get() = quantity != null && lowStockThreshold != null && quantity <= lowStockThreshold

    fun isExpiringWithin(today: LocalDate, days: Long): Boolean = expiryDate != null && !expiryDate.isAfter(today.plusDays(days))
}

data class Expense(
    val id: Long = 0,
    val title: String,
    val module: CareModule? = null,
    val category: ExpenseCategory,
    val amountMinor: Long,
    val currencyCode: String,
    val spentOn: LocalDate,
    val inventoryItemId: Long? = null,
    val plantId: Long? = null,
    val aquariumId: Long? = null,
    val note: String? = null,
)

data class Photo(
    val id: Long = 0,
    val filePath: String,
    val takenAt: Instant,
    val caption: String? = null,
    val plantId: Long? = null,
    val aquariumId: Long? = null,
    val fishId: Long? = null,
    val growthLogId: Long? = null,
)

data class NotificationSettings(
    val remindersEnabled: Boolean = true,
    val defaultReminderTime: LocalTime = LocalTime.of(7, 0),
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(6, 0),
    val disabledChannels: Set<String> = emptySet(),
    val missedRemindersEnabled: Boolean = true,
    val inventoryRemindersEnabled: Boolean = true,
)

data class ReminderLogEntry(
    val id: Long,
    val kind: ReminderKind,
    val taskId: Long?,
    val occurrenceDate: LocalDate?,
    val title: String,
    val text: String,
    val postedAt: Instant,
)
