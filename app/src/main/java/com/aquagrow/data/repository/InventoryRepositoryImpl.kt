package com.aquagrow.data.repository

import com.aquagrow.data.local.dao.ExpenseDao
import com.aquagrow.data.local.dao.InventoryDao
import com.aquagrow.data.mapper.toDomain
import com.aquagrow.data.mapper.toEntity
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.Expense
import com.aquagrow.domain.model.InventoryItem
import com.aquagrow.domain.repository.InventoryRepository
import com.aquagrow.domain.repository.ReminderScheduler
import com.aquagrow.domain.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val expenseDao: ExpenseDao,
    private val time: TimeProvider,
    private val reminders: ReminderScheduler,
) : InventoryRepository {
    override fun observeItems() = inventoryDao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun observeItems(module: CareModule) = inventoryDao.observeByModule(module).map { l -> l.map { it.toDomain() } }
    override fun observeEquipment(aquariumId: Long) = inventoryDao.observeEquipmentForAquarium(aquariumId).map { l -> l.map { it.toDomain() } }
    override suspend fun getItem(id: Long) = inventoryDao.getById(id)?.toDomain()
    override suspend fun getItems() = inventoryDao.getAll().map { it.toDomain() }

    override suspend fun saveItem(item: InventoryItem): Long {
        val now = time.now()
        val createdAt = if (item.id == 0L) now else inventoryDao.getById(item.id)?.createdAt ?: now
        val id = inventoryDao.upsert(item.toEntity(createdAt, now)).let { if (it == -1L) item.id else it }
        reminders.requestReschedule() // low-stock / expiry reminders may change
        return id
    }

    override suspend fun deleteItem(id: Long) {
        inventoryDao.getById(id)?.let { inventoryDao.delete(it) }
        reminders.requestReschedule()
    }

    override fun observeExpenses() = expenseDao.observeAll().map { l -> l.map { it.toDomain() } }
    override suspend fun getExpensesForItem(itemId: Long) = expenseDao.getForInventoryItem(itemId).map { it.toDomain() }
    override suspend fun saveExpense(expense: Expense): Long = expenseDao.upsert(expense.toEntity(time.now())).let { if (it == -1L) expense.id else it }
    override suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense.toEntity(time.now()))
}
