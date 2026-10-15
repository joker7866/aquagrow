package com.aquagrow.domain.analytics

import com.aquagrow.domain.model.Expense
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskOccurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** done out of total; [percent] is null when there is nothing to measure yet. */
data class Completion(val done: Int, val total: Int) {
    val percent: Int? get() = if (total == 0) null else (done * 100 + total / 2) / total
}

data class WeekStat(val weekStart: LocalDate, val done: Int, val total: Int)

object AnalyticsCalc {
    /**
     * Completion for occurrences that are already due (dated before today, or today and handled).
     * Skipped counts as handled-but-not-done: it is in the total, not in done.
     */
    fun completion(occurrences: List<TaskOccurrence>, today: LocalDate): Completion {
        val due = occurrences.filter { it.date.isBefore(today) || (it.date == today && it.isHandled) }
        return Completion(due.count { it.status == OccurrenceStatus.DONE }, due.size)
    }

    /** Last [weeks] Monday-based weeks ending with the current one, oldest first. */
    fun weekly(occurrences: List<TaskOccurrence>, today: LocalDate, weeks: Int = 8): List<WeekStat> {
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (weeks - 1 downTo 0).map { back ->
            val start = thisWeek.minusWeeks(back.toLong())
            val inWeek = occurrences.filter { !it.date.isBefore(start) && !it.date.isAfter(start.plusDays(6)) }
            val c = completion(inWeek, today)
            WeekStat(start, c.done, c.total)
        }
    }

    /**
     * Consecutive days (ending today if today is fully handled, otherwise yesterday) on which
     * every due task was done or skipped. Days with nothing scheduled neither count nor break it.
     */
    fun streak(occurrences: List<TaskOccurrence>, today: LocalDate, maxDays: Int = 365): Int {
        val byDay = occurrences.groupBy { it.date }
        var day = today
        val todayList = byDay[today].orEmpty()
        if (todayList.isEmpty() || todayList.any { !it.isHandled }) day = today.minusDays(1)
        var count = 0
        repeat(maxDays) {
            val list = byDay[day].orEmpty()
            if (list.isNotEmpty()) {
                if (list.any { !it.isHandled }) return count
                count++
            }
            day = day.minusDays(1)
        }
        return count
    }
}

data class MoneyTotal(val amountMinor: Long, val currencyCode: String)

object ExpenseCalc {
    /** Total in [currency] for [from]..[to] inclusive. Other currencies are never mixed in. */
    fun total(expenses: List<Expense>, currency: String, from: LocalDate, to: LocalDate): Long = expenses.filter { it.currencyCode == currency && !it.spentOn.isBefore(from) && !it.spentOn.isAfter(to) }.sumOf { it.amountMinor }

    /** One total per month for the last [months] months ending with [current], oldest first. */
    fun monthly(expenses: List<Expense>, currency: String, current: YearMonth, months: Int = 6): List<Pair<YearMonth, Long>> = (months - 1 downTo 0).map { back ->
        val ym = current.minusMonths(back.toLong())
        ym to total(expenses, currency, ym.atDay(1), ym.atEndOfMonth())
    }

    fun byCategory(expenses: List<Expense>, currency: String, from: LocalDate, to: LocalDate) = expenses.filter { it.currencyCode == currency && !it.spentOn.isBefore(from) && !it.spentOn.isAfter(to) }
        .groupBy { it.category }
        .mapValues { (_, l) -> l.sumOf { it.amountMinor } }
        .toList()
        .sortedByDescending { it.second }

    /** Expenses recorded in other currencies (shown separately instead of being converted). */
    fun otherCurrencies(expenses: List<Expense>, currency: String): List<MoneyTotal> = expenses.filter { it.currencyCode != currency }
        .groupBy { it.currencyCode }
        .map { (code, l) -> MoneyTotal(l.sumOf { it.amountMinor }, code) }

    /** Parses user input like "1,250.50" into minor units (paise). Null if invalid or negative. */
    fun parseAmount(text: String, fractionDigits: Int = 2): Long? {
        val clean = text.trim().replace(",", "")
        if (clean.isEmpty()) return null
        val value = clean.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return runCatching { value.movePointRight(fractionDigits).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() }.getOrNull()
    }
}
