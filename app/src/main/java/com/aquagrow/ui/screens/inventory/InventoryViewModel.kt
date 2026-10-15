package com.aquagrow.ui.screens.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.Expense
import com.aquagrow.domain.model.InventoryItem
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.InventoryRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.SaveInventoryItemUseCase
import com.aquagrow.ui.navigation.InventoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InventoryTab { ITEMS, EXPENSES }

data class InventoryUiState(
    val loading: Boolean = true,
    val tab: InventoryTab = InventoryTab.ITEMS,
    val module: CareModule? = null,
    val items: List<InventoryItem> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val tanks: List<Aquarium> = emptyList(),
    val currency: String = "INR",
    val today: LocalDate = LocalDate.now(),
    val gardenEnabled: Boolean = true,
    val aquariumEnabled: Boolean = true,
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inventory: InventoryRepository,
    aquarium: AquariumRepository,
    prefs: UserPreferencesRepository,
    private val saveItem: SaveInventoryItemUseCase,
    private val time: TimeProvider,
) : ViewModel() {
    private val tab = MutableStateFlow(InventoryTab.ITEMS)
    private val module = MutableStateFlow(savedStateHandle.toRoute<InventoryRoute>().module?.let { m -> CareModule.entries.firstOrNull { it.name == m } })

    val state: StateFlow<InventoryUiState> = combine(
        combine(tab, module, ::Pair),
        inventory.observeItems(),
        inventory.observeExpenses(),
        aquarium.observeAquariums(),
        prefs.preferences,
    ) { (t, m), items, expenses, tanks, p ->
        InventoryUiState(
            loading = false, tab = t, module = m,
            items = items.filter { m == null || it.module == m },
            expenses = expenses.filter { m == null || it.module == null || it.module == m },
            tanks = tanks, currency = p.currencyCode, today = time.today(),
            gardenEnabled = p.gardenEnabled, aquariumEnabled = p.aquariumEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun setTab(t: InventoryTab) {
        tab.value = t
    }
    fun setModule(m: CareModule?) {
        module.value = m
    }

    fun save(item: InventoryItem) = viewModelScope.launch { saveItem(item.copy(currencyCode = item.currencyCode ?: state.value.currency)) }

    fun adjust(item: InventoryItem, delta: Double) = viewModelScope.launch {
        val q = ((item.quantity ?: 0.0) + delta).coerceAtLeast(0.0)
        inventory.saveItem(item.copy(quantity = q))
    }

    fun delete(item: InventoryItem) = viewModelScope.launch { inventory.deleteItem(item.id) }
    fun saveExpense(e: Expense) = viewModelScope.launch { inventory.saveExpense(e) }
    fun deleteExpense(e: Expense) = viewModelScope.launch { inventory.deleteExpense(e) }
}
