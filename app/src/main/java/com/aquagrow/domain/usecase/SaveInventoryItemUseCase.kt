package com.aquagrow.domain.usecase

import com.aquagrow.domain.model.Expense
import com.aquagrow.domain.model.ExpenseCategory
import com.aquagrow.domain.model.InventoryCategory
import com.aquagrow.domain.model.InventoryItem
import com.aquagrow.domain.repository.InventoryRepository
import com.aquagrow.domain.time.TimeProvider
import javax.inject.Inject

/**
 * Saves an inventory item. When it has a cost, the purchase is also recorded as an expense
 * (created once, then kept in sync if the cost, date or name changes).
 */
class SaveInventoryItemUseCase @Inject constructor(
    private val inventory: InventoryRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(item: InventoryItem): Long {
        val id = inventory.saveItem(item.copy(name = item.name.trim()))
        val cost = item.costMinor
        val linked = inventory.getExpensesForItem(id).firstOrNull()
        when {
            cost != null && cost > 0 -> inventory.saveExpense(
                Expense(
                    id = linked?.id ?: 0,
                    title = item.name.trim(),
                    module = item.module,
                    category = expenseCategoryFor(item.category),
                    amountMinor = cost,
                    currencyCode = item.currencyCode ?: linked?.currencyCode ?: "INR",
                    spentOn = item.purchaseDate ?: linked?.spentOn ?: time.today(),
                    inventoryItemId = id,
                    aquariumId = item.aquariumId,
                    note = linked?.note,
                ),
            )

            linked != null -> inventory.deleteExpense(linked) // cost removed
        }
        return id
    }

    companion object {
        fun expenseCategoryFor(c: InventoryCategory): ExpenseCategory = when (c) {
            InventoryCategory.SEEDS -> ExpenseCategory.SEEDS
            InventoryCategory.POTS, InventoryCategory.GROW_BAGS -> ExpenseCategory.POTS
            InventoryCategory.SOIL -> ExpenseCategory.SOIL
            InventoryCategory.FERTILIZER, InventoryCategory.PEST_CONTROL -> ExpenseCategory.FERTILIZER
            InventoryCategory.TOOLS -> ExpenseCategory.TOOLS
            InventoryCategory.FISH_FOOD -> ExpenseCategory.FISH_FOOD
            InventoryCategory.WATER_CONDITIONER, InventoryCategory.TEST_KIT -> ExpenseCategory.WATER_CARE
            InventoryCategory.FILTER_MEDIA, InventoryCategory.EQUIPMENT -> ExpenseCategory.EQUIPMENT
            InventoryCategory.OTHER_GARDEN, InventoryCategory.OTHER_AQUARIUM -> ExpenseCategory.OTHER
        }
    }
}
