package com.aquagrow.notifications

import android.content.Context
import com.aquagrow.R
import com.aquagrow.domain.model.ReminderKind
import com.aquagrow.domain.repository.InventoryRepository
import com.aquagrow.domain.repository.ReminderLogRepository
import com.aquagrow.domain.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Low-stock and expiry reminders. Each condition notifies once (dedupe key includes the
 * quantity / expiry date), so a still-low item does not nag every few hours.
 */
class InventoryReminderCheck @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val inventory: InventoryRepository,
    private val log: ReminderLogRepository,
    private val notifier: ReminderNotifier,
    private val time: TimeProvider,
) {
    suspend fun run() {
        val today = time.today()
        inventory.getItems().forEach { item ->
            if (item.isLowStock) {
                val title = context.getString(R.string.notif_low_stock_title, item.name)
                val text = context.getString(R.string.notif_low_stock_text)
                if (log.recordIfNew(ReminderKind.LOW_STOCK, null, null, item.id, title, text, "stock-${item.id}-${item.quantity}")) {
                    notifier.showInventory(item.id, title, text)
                }
            }
            val expiry = item.expiryDate
            if (expiry != null && item.isExpiringWithin(today, EXPIRY_WARNING_DAYS)) {
                val title = context.getString(R.string.notif_expiry_title, item.name)
                val text = context.getString(if (expiry.isBefore(today)) R.string.notif_expired_text else R.string.notif_expiry_text)
                if (log.recordIfNew(ReminderKind.EXPIRY, null, null, item.id, title, text, "expiry-${item.id}-$expiry")) {
                    notifier.showInventory(item.id, title, text)
                }
            }
        }
    }

    companion object {
        const val EXPIRY_WARNING_DAYS = 7L
    }
}
