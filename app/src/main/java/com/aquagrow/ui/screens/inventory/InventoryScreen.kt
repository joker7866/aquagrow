package com.aquagrow.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.analytics.ExpenseCalc
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.Expense
import com.aquagrow.domain.model.ExpenseCategory
import com.aquagrow.domain.model.InventoryCategory
import com.aquagrow.domain.model.InventoryItem
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppFilterChip
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.ChipTone
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.ConfirmDialog
import com.aquagrow.ui.components.DateField
import com.aquagrow.ui.components.FormSection
import com.aquagrow.ui.components.IconTile
import com.aquagrow.ui.components.NumberField
import com.aquagrow.ui.components.PillSegmented
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.StatusChip
import com.aquagrow.ui.components.currencySymbol
import com.aquagrow.ui.components.currentLocale
import com.aquagrow.ui.components.formatMoney
import com.aquagrow.ui.components.formatReading
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.screens.analytics.BarChart
import com.aquagrow.ui.screens.garden.FormSheet
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.forModule
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun InventoryScreen(onBack: () -> Unit, vm: InventoryViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<InventoryItem?>(null) }
    var addingExpense by remember { mutableStateOf<Expense?>(null) }
    var deleting by remember { mutableStateOf<InventoryItem?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(stringResource(R.string.inventory_title), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (s.tab == InventoryTab.ITEMS) {
                        val m = s.module ?: if (s.gardenEnabled) CareModule.GARDEN else CareModule.AQUARIUM
                        editing = InventoryItem(module = m, name = "", category = InventoryCategory.entries.first { it.module == m })
                    } else {
                        addingExpense = Expense(title = "", module = s.module, category = ExpenseCategory.OTHER, amountMinor = 0, currencyCode = s.currency, spentOn = s.today)
                    }
                },
                icon = { Icon(painterResource(R.drawable.ic_add), null) },
                text = { Text(stringResource(if (s.tab == InventoryTab.ITEMS) R.string.inventory_add else R.string.expenses_add)) },
                containerColor = AquaGrowTheme.module.primary,
                contentColor = AquaGrowTheme.module.onPrimary,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                PillSegmented(InventoryTab.entries, s.tab, { stringResource(if (it == InventoryTab.ITEMS) R.string.inventory_tab_items else R.string.inventory_tab_expenses) }, vm::setTab)
            }
            if (s.gardenEnabled && s.aquariumEnabled) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppFilterChip(s.module == null, stringResource(R.string.inventory_all), { vm.setModule(null) }, selectedColor = MaterialTheme.colorScheme.onSurface)
                        AppFilterChip(s.module == CareModule.GARDEN, stringResource(R.string.calendar_garden), { vm.setModule(CareModule.GARDEN) }, selectedColor = MaterialTheme.colorScheme.onSurface)
                        AppFilterChip(s.module == CareModule.AQUARIUM, stringResource(R.string.calendar_aquarium), { vm.setModule(CareModule.AQUARIUM) }, selectedColor = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            if (s.tab == InventoryTab.ITEMS) {
                if (s.items.isEmpty() && !s.loading) item { Text(stringResource(R.string.inventory_empty), color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(vertical = 8.dp)) }
                items(s.items, key = { it.id }) { item -> ItemCard(item, s, { editing = item }, { vm.adjust(item, it) }) }
            } else {
                expensesContent(s) { addingExpense = it }
            }
        }
    }

    editing?.let { item ->
        ItemSheet(
            item = item,
            currency = s.currency,
            tanks = s.tanks.map { it.id to it.name },
            onSave = {
                vm.save(it)
                editing = null
            },
            onDelete = if (item.id != 0L) {
                (
                    {
                        deleting = item
                        editing = null
                    }
                    )
            } else {
                null
            },
            onDismiss = { editing = null },
        )
    }
    addingExpense?.let { e ->
        ExpenseSheet(
            e,
            s.currency,
            {
                vm.saveExpense(it)
                addingExpense = null
            },
            if (e.id != 0L) {
                (
                    {
                        vm.deleteExpense(e)
                        addingExpense = null
                    }
                    )
            } else {
                null
            },
            { addingExpense = null },
        )
    }
    deleting?.let { item ->
        ConfirmDialog(stringResource(R.string.inventory_delete), item.name, stringResource(R.string.action_delete), {
            vm.delete(item)
            deleting = null
        }, { deleting = null }, destructive = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemCard(item: InventoryItem, s: InventoryUiState, onEdit: () -> Unit, onAdjust: (Double) -> Unit) {
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors
    AppCard(Modifier.fillMaxWidth(), onClick = onEdit, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(if (item.module == CareModule.GARDEN) R.drawable.ic_sprout else R.drawable.ic_fish, ext.forModule(item.module), size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(
                        item.category.label(),
                        item.quantity?.let { "${formatReading(it)} ${item.unit.orEmpty()}".trim() },
                        item.costMinor?.let { formatMoney(it, item.currencyCode ?: s.currency) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.mutedText,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.isLowStock) StatusChip(stringResource(R.string.inventory_low), ChipTone.WARNING)
                    item.expiryDate?.let { exp ->
                        when {
                            exp.isBefore(s.today) -> StatusChip(stringResource(R.string.inventory_expired), ChipTone.WARNING)
                            item.isExpiringWithin(s.today, 30) -> StatusChip(stringResource(R.string.inventory_expiring, f.dayMonth.format(exp)), ChipTone.WARNING)
                            else -> Unit
                        }
                    }
                    item.aquariumId?.let { id -> s.tanks.firstOrNull { it.id == id }?.let { StatusChip(stringResource(R.string.inventory_in_tank, it.name), ChipTone.AQUA) } }
                }
            }
            if (item.quantity != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onAdjust(1.0) }) { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.inventory_increase, item.name)) }
                    IconButton(onClick = { onAdjust(-1.0) }, enabled = item.quantity > 0) { Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.inventory_decrease, item.name)) }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.expensesContent(s: InventoryUiState, onEdit: (Expense) -> Unit) {
    item {
        val ym = YearMonth.from(s.today)
        val month = ExpenseCalc.total(s.expenses, s.currency, ym.atDay(1), ym.atEndOfMonth())
        val series = ExpenseCalc.monthly(s.expenses, s.currency, ym)
        AppCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
            Text(stringResource(R.string.expenses_this_month), style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText)
            Text(formatMoney(month, s.currency), style = MaterialTheme.typography.headlineMedium)
            if (series.any { it.second > 0 }) {
                Text(stringResource(R.string.expenses_last_6), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                val f = rememberFormatters()
                BarChart(
                    values = series.map { it.second.toFloat() },
                    labels = series.map { it.first.month.getDisplayName(java.time.format.TextStyle.SHORT, currentLocale()) },
                    description = series.joinToString("; ") { "${it.first}: ${formatMoney(it.second, s.currency)}" },
                    modifier = Modifier.height(120.dp),
                )
            }
            val others = ExpenseCalc.otherCurrencies(s.expenses, s.currency)
            if (others.isNotEmpty()) {
                Text(
                    stringResource(R.string.expenses_other_currency, others.joinToString(", ") { formatMoney(it.amountMinor, it.currencyCode) }),
                    style = MaterialTheme.typography.bodySmall,
                    color = AquaGrowTheme.colors.mutedText,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
    val byCat = ExpenseCalc.byCategory(s.expenses, s.currency, YearMonth.from(s.today).minusMonths(5).atDay(1), s.today)
    if (byCat.isNotEmpty()) {
        item {
            AppCard(Modifier.fillMaxWidth()) {
                byCat.forEach { (cat, amount) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(cat.label(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(formatMoney(amount, s.currency), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    item { SectionHeader(stringResource(R.string.inventory_tab_expenses)) }
    if (s.expenses.isEmpty()) item { Text(stringResource(R.string.expenses_empty), color = AquaGrowTheme.colors.mutedText) }
    items(s.expenses, key = { "e-${it.id}" }) { e ->
        val f = rememberFormatters()
        AppCard(Modifier.fillMaxWidth(), onClick = { onEdit(e) }, contentPadding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.title, style = MaterialTheme.typography.titleSmall)
                    Text("${e.category.label()} · ${f.dayMonthYear.format(e.spentOn)}", style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                }
                Text(formatMoney(e.amountMinor, e.currencyCode), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ItemSheet(
    item: InventoryItem,
    currency: String,
    tanks: List<Pair<Long, String>>,
    onSave: (InventoryItem) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var i by remember { mutableStateOf(item) }
    var qty by rememberSaveable { mutableStateOf(item.quantity?.let(::formatReading).orEmpty()) }
    var low by rememberSaveable { mutableStateOf(item.lowStockThreshold?.let(::formatReading).orEmpty()) }
    var cost by rememberSaveable { mutableStateOf(item.costMinor?.let { java.math.BigDecimal.valueOf(it, 2).stripTrailingZeros().toPlainString() }.orEmpty()) }
    var tried by remember { mutableStateOf(false) }
    val costValue = ExpenseCalc.parseAmount(cost)
    val costOk = cost.isBlank() || costValue != null
    FormSheet(
        title = stringResource(if (item.id == 0L) R.string.inventory_add else R.string.inventory_edit),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            tried = true
            if (i.name.isNotBlank() && costOk) {
                onSave(i.copy(quantity = qty.toDoubleOrNull(), lowStockThreshold = low.toDoubleOrNull(), costMinor = costValue, currencyCode = item.currencyCode ?: currency))
            }
        },
        onDismiss = onDismiss,
    ) {
        AppTextField(i.name, { i = i.copy(name = it) }, stringResource(R.string.inventory_name), required = true, maxLength = 60, error = if (tried && i.name.isBlank()) stringResource(R.string.error_name) else null)
        FormSection(stringResource(R.string.inventory_module)) {
            ChoiceChips(CareModule.entries, i.module, { stringResource(if (it == CareModule.GARDEN) R.string.calendar_garden else R.string.calendar_aquarium) }, { m ->
                i = i.copy(module = m, category = if (i.category.module == m) i.category else InventoryCategory.entries.first { it.module == m }, aquariumId = if (m == CareModule.AQUARIUM) i.aquariumId else null)
            })
        }
        FormSection(stringResource(R.string.inventory_category)) {
            ChoiceChips(InventoryCategory.entries.filter { it.module == i.module }, i.category, { it.label() }, { i = i.copy(category = it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(qty, { qty = it }, stringResource(R.string.inventory_quantity), Modifier.weight(1f))
            AppTextField(i.unit.orEmpty(), { i = i.copy(unit = it.ifBlank { null }) }, stringResource(R.string.inventory_unit), Modifier.weight(1f), placeholder = stringResource(R.string.inventory_unit_hint), maxLength = 12)
        }
        NumberField(low, { low = it }, stringResource(R.string.inventory_low_threshold))
        NumberField(cost, { cost = it }, stringResource(R.string.inventory_cost, currencySymbol(currency)), supporting = stringResource(R.string.inventory_cost_help), error = if (!costOk) stringResource(R.string.error_amount) else null)
        DateField(stringResource(R.string.inventory_purchase_date), i.purchaseDate, { i = i.copy(purchaseDate = it) }, clearable = true, maxDate = LocalDate.now())
        DateField(stringResource(R.string.inventory_expiry), i.expiryDate, { i = i.copy(expiryDate = it) }, clearable = true)
        if (i.module == CareModule.AQUARIUM && tanks.isNotEmpty()) {
            FormSection(stringResource(R.string.tank_equipment_items)) {
                ChoiceChips(listOf<Long?>(null) + tanks.map { it.first }, i.aquariumId, { id -> id?.let { tanks.first { t -> t.first == it }.second } ?: stringResource(R.string.task_field_none) }, { i = i.copy(aquariumId = it) })
            }
        }
        AppTextField(i.notes.orEmpty(), { i = i.copy(notes = it.ifBlank { null }) }, stringResource(R.string.inventory_notes), singleLine = false)
        if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.inventory_delete), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ExpenseSheet(expense: Expense, currency: String, onSave: (Expense) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var e by remember { mutableStateOf(expense) }
    var amount by rememberSaveable { mutableStateOf(if (expense.amountMinor > 0) java.math.BigDecimal.valueOf(expense.amountMinor, 2).stripTrailingZeros().toPlainString() else "") }
    var tried by remember { mutableStateOf(false) }
    val parsed = ExpenseCalc.parseAmount(amount)
    FormSheet(
        title = stringResource(R.string.expenses_add),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            tried = true
            if (e.title.isNotBlank() && parsed != null && parsed > 0) onSave(e.copy(title = e.title.trim(), amountMinor = parsed, currencyCode = expense.currencyCode.ifBlank { currency }))
        },
        onDismiss = onDismiss,
    ) {
        AppTextField(e.title, { e = e.copy(title = it) }, stringResource(R.string.expenses_title_field), required = true, maxLength = 60, error = if (tried && e.title.isBlank()) stringResource(R.string.error_name) else null)
        NumberField(amount, { amount = it }, stringResource(R.string.expenses_amount, currencySymbol(e.currencyCode)), error = if (tried && (parsed == null || parsed <= 0)) stringResource(R.string.error_amount) else null)
        FormSection(stringResource(R.string.expenses_category)) { ChoiceChips(ExpenseCategory.entries, e.category, { it.label() }, { e = e.copy(category = it) }) }
        FormSection(stringResource(R.string.expenses_module)) {
            ChoiceChips(listOf<CareModule?>(null) + CareModule.entries, e.module, {
                stringResource(
                    when (it) {
                        null -> R.string.expenses_shared
                        CareModule.GARDEN -> R.string.calendar_garden
                        CareModule.AQUARIUM -> R.string.calendar_aquarium
                    },
                )
            }, { e = e.copy(module = it) })
        }
        DateField(stringResource(R.string.expenses_date), e.spentOn, { d -> d?.let { e = e.copy(spentOn = it) } }, maxDate = LocalDate.now())
        AppTextField(e.note.orEmpty(), { e = e.copy(note = it.ifBlank { null }) }, stringResource(R.string.inventory_notes), singleLine = false)
        if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
    }
}
