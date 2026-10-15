package com.aquagrow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aquagrow.R
import com.aquagrow.domain.model.ExpenseCategory
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Formats minor units (e.g. paise) for display: ₹1,250.50. Indian grouping for INR. */
fun formatMoney(minor: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
    val currency = runCatching { Currency.getInstance(currencyCode) }.getOrNull()
    val digits = currency?.defaultFractionDigits?.coerceAtLeast(0) ?: 2
    val format = if (currencyCode == "INR") NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) else NumberFormat.getCurrencyInstance(locale)
    currency?.let { format.currency = it }
    format.minimumFractionDigits = if (minor % 100 == 0L) 0 else digits
    format.maximumFractionDigits = digits
    return format.format(BigDecimal.valueOf(minor, digits))
}

fun currencySymbol(code: String): String = if (code == "INR") "₹" else runCatching { Currency.getInstance(code).symbol }.getOrDefault(code)

@Composable
fun ExpenseCategory.label(): String = stringResource(
    when (this) {
        ExpenseCategory.PLANTS -> R.string.expense_cat_plants
        ExpenseCategory.SEEDS -> R.string.expense_cat_seeds
        ExpenseCategory.SOIL -> R.string.expense_cat_soil
        ExpenseCategory.POTS -> R.string.expense_cat_pots
        ExpenseCategory.FERTILIZER -> R.string.expense_cat_fertilizer
        ExpenseCategory.TOOLS -> R.string.expense_cat_tools
        ExpenseCategory.FISH -> R.string.expense_cat_fish
        ExpenseCategory.FISH_FOOD -> R.string.expense_cat_fish_food
        ExpenseCategory.WATER_CARE -> R.string.expense_cat_water_care
        ExpenseCategory.EQUIPMENT -> R.string.expense_cat_equipment
        ExpenseCategory.OTHER -> R.string.expense_cat_other
    },
)
