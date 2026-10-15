package com.aquagrow.domain.analytics

import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.Expense
import com.aquagrow.domain.model.ExpenseCategory
import com.aquagrow.domain.model.InventoryCategory
import com.aquagrow.domain.model.InventoryItem
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.repository.InventoryRepository
import com.aquagrow.domain.usecase.SaveInventoryItemUseCase
import com.aquagrow.testing.FakeTime
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCalcTest {
    private fun e(amount: Long, on: LocalDate, cur: String = "INR", cat: ExpenseCategory = ExpenseCategory.SEEDS) = Expense(title = "x", category = cat, amountMinor = amount, currencyCode = cur, spentOn = on)

    private val oct = LocalDate.of(2026, 10, 1)

    @Test fun `total includes both ends of the range and ignores other currencies`() {
        val list = listOf(e(100_00, oct), e(50_50, oct.plusDays(30)), e(999_00, oct, "USD"), e(1_00, oct.minusDays(1)))
        assertEquals(150_50L, ExpenseCalc.total(list, "INR", oct, oct.plusDays(30)))
    }

    @Test fun `monthly returns oldest first with empty months kept`() {
        val list = listOf(e(10_00, LocalDate.of(2026, 8, 15)), e(20_00, LocalDate.of(2026, 10, 2)), e(5_00, LocalDate.of(2026, 10, 31)))
        val m = ExpenseCalc.monthly(list, "INR", YearMonth.of(2026, 10), months = 3)
        assertEquals(listOf(YearMonth.of(2026, 8) to 10_00L, YearMonth.of(2026, 9) to 0L, YearMonth.of(2026, 10) to 25_00L), m)
    }

    @Test fun `by category sorts largest first`() {
        val list = listOf(e(10_00, oct, cat = ExpenseCategory.SOIL), e(30_00, oct, cat = ExpenseCategory.FISH_FOOD), e(5_00, oct, cat = ExpenseCategory.SOIL))
        assertEquals(listOf(ExpenseCategory.FISH_FOOD to 30_00L, ExpenseCategory.SOIL to 15_00L), ExpenseCalc.byCategory(list, "INR", oct, oct))
    }

    @Test fun `other currencies are reported separately, never converted`() {
        val list = listOf(e(1_00, oct), e(2_00, oct, "USD"), e(3_00, oct, "USD"), e(4_00, oct, "EUR"))
        assertEquals(setOf(MoneyTotal(5_00, "USD"), MoneyTotal(4_00, "EUR")), ExpenseCalc.otherCurrencies(list, "INR").toSet())
    }

    @Test fun `parse amount handles commas, decimals and rounding`() {
        assertEquals(125050L, ExpenseCalc.parseAmount("1,250.50"))
        assertEquals(1000L, ExpenseCalc.parseAmount(" 10 "))
        assertEquals(1001L, ExpenseCalc.parseAmount("10.005"))
        assertEquals(0L, ExpenseCalc.parseAmount("0"))
    }

    @Test fun `parse amount rejects junk and negatives`() {
        assertNull(ExpenseCalc.parseAmount(""))
        assertNull(ExpenseCalc.parseAmount("abc"))
        assertNull(ExpenseCalc.parseAmount("-5"))
    }
}

class AnalyticsCalcTest {
    private val today = LocalDate.of(2026, 10, 8) // Thursday
    private val task = CareTask(id = 1, title = "Water", module = CareModule.GARDEN, type = TaskType.WATERING, startDate = today.minusDays(60))
    private fun occ(daysAgo: Long, status: OccurrenceStatus) = today.minusDays(daysAgo).let { TaskOccurrence(task, it, it, status) }

    @Test fun `completion is null with nothing due`() {
        assertNull(AnalyticsCalc.completion(emptyList(), today).percent)
        // Pending today is not due yet.
        assertNull(AnalyticsCalc.completion(listOf(occ(0, OccurrenceStatus.PENDING)), today).percent)
    }

    @Test fun `skipped counts in total but not done`() {
        val c = AnalyticsCalc.completion(
            listOf(occ(1, OccurrenceStatus.DONE), occ(2, OccurrenceStatus.SKIPPED), occ(3, OccurrenceStatus.PENDING), occ(0, OccurrenceStatus.DONE)),
            today,
        )
        assertEquals(Completion(2, 4), c)
        assertEquals(50, c.percent)
    }

    @Test fun `weekly buckets start on monday`() {
        val w = AnalyticsCalc.weekly(listOf(occ(1, OccurrenceStatus.DONE), occ(4, OccurrenceStatus.PENDING)), today, weeks = 2)
        assertEquals(WeekStat(LocalDate.of(2026, 10, 5), 1, 1), w.last())
        assertEquals(WeekStat(LocalDate.of(2026, 9, 28), 0, 1), w.first())
    }

    @Test fun `streak skips empty days and stops at a miss`() {
        val list = listOf(occ(1, OccurrenceStatus.DONE), occ(3, OccurrenceStatus.SKIPPED), occ(4, OccurrenceStatus.DONE), occ(5, OccurrenceStatus.PENDING))
        assertEquals(3, AnalyticsCalc.streak(list, today))
        // Today fully done extends it.
        assertEquals(4, AnalyticsCalc.streak(list + occ(0, OccurrenceStatus.DONE), today))
    }
}

/** In-memory inventory repository for the expense auto-sync use case. */
private class FakeInventoryRepository : InventoryRepository {
    val items = MutableStateFlow<List<InventoryItem>>(emptyList())
    val expenses = MutableStateFlow<List<Expense>>(emptyList())
    private var nextId = 1L

    override fun observeItems(): Flow<List<InventoryItem>> = items
    override fun observeItems(module: CareModule) = items.map { l -> l.filter { it.module == module } }
    override fun observeEquipment(aquariumId: Long) = items.map { l -> l.filter { it.aquariumId == aquariumId } }
    override suspend fun getItem(id: Long) = items.value.firstOrNull { it.id == id }
    override suspend fun getItems() = items.value
    override suspend fun saveItem(item: InventoryItem): Long {
        val id = if (item.id == 0L) nextId++ else item.id
        items.value = items.value.filterNot { it.id == id } + item.copy(id = id)
        return id
    }
    override suspend fun deleteItem(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }
    override fun observeExpenses(): Flow<List<Expense>> = expenses
    override suspend fun getExpensesForItem(itemId: Long) = expenses.value.filter { it.inventoryItemId == itemId }
    override suspend fun saveExpense(expense: Expense): Long {
        val id = if (expense.id == 0L) nextId++ else expense.id
        expenses.value = expenses.value.filterNot { it.id == id } + expense.copy(id = id)
        return id
    }
    override suspend fun deleteExpense(expense: Expense) {
        expenses.value = expenses.value.filterNot { it.id == expense.id }
    }
}

class SaveInventoryItemUseCaseTest {
    private val repo = FakeInventoryRepository()
    private val time = FakeTime()
    private val save = SaveInventoryItemUseCase(repo, time)
    private val item = InventoryItem(module = CareModule.AQUARIUM, name = " Flakes ", category = InventoryCategory.FISH_FOOD, costMinor = 250_00, currencyCode = "INR")

    @Test fun `bought item with cost creates one expense dated today`() = runTest {
        val id = save(item)
        val e = repo.expenses.value.single()
        assertEquals(id, e.inventoryItemId)
        assertEquals("Flakes", e.title)
        assertEquals(250_00L, e.amountMinor)
        assertEquals(ExpenseCategory.FISH_FOOD, e.category)
        assertEquals(time.today(), e.spentOn)
    }

    @Test fun `editing cost updates the same expense instead of adding another`() = runTest {
        val id = save(item)
        save(repo.getItem(id)!!.copy(costMinor = 300_00))
        assertEquals(300_00L, repo.expenses.value.single().amountMinor)
        assertEquals(300_00L, ExpenseCalc.total(repo.expenses.value, "INR", time.today(), time.today()))
    }

    @Test fun `removing the cost removes the linked expense`() = runTest {
        val id = save(item)
        save(repo.getItem(id)!!.copy(costMinor = null))
        assertTrue(repo.expenses.value.isEmpty())
    }

    @Test fun `item without cost creates no expense`() = runTest {
        save(item.copy(costMinor = null))
        assertTrue(repo.expenses.value.isEmpty())
    }
}
