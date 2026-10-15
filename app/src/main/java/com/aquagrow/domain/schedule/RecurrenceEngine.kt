package com.aquagrow.domain.schedule

import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Pure date maths for recurring tasks. No storage; occurrences are always recomputed. */
object RecurrenceEngine {

    /**
     * All occurrence dates of a task in [from]..[to] (inclusive), in ascending order.
     * [start] is the first possible occurrence and [end] (optional) the last.
     */
    fun occurrences(
        rule: RecurrenceRule,
        start: LocalDate,
        end: LocalDate?,
        from: LocalDate,
        to: LocalDate,
    ): List<LocalDate> {
        val lo = maxOf(from, start)
        val hi = if (end != null) minOf(to, end) else to
        if (lo.isAfter(hi)) return emptyList()

        return when (rule.type) {
            RecurrenceType.ONCE -> if (start in lo..hi) listOf(start) else emptyList()
            RecurrenceType.DAILY -> daily(rule.interval, start, lo, hi)
            RecurrenceType.WEEKLY -> weekly(rule, start, lo, hi)
            RecurrenceType.MONTHLY -> monthly(rule, start, lo, hi)
        }
    }

    /** First occurrence on or after [date], looking at most [horizonDays] ahead. */
    fun nextOnOrAfter(
        rule: RecurrenceRule,
        start: LocalDate,
        end: LocalDate?,
        date: LocalDate,
        horizonDays: Long = 400,
    ): LocalDate? = occurrences(rule, start, end, date, date.plusDays(horizonDays)).firstOrNull()

    private fun daily(interval: Int, start: LocalDate, lo: LocalDate, hi: LocalDate): List<LocalDate> {
        val offset = ChronoUnit.DAYS.between(start, lo)
        // Round up to the first multiple of interval that is >= lo.
        val steps = (offset + interval - 1) / interval
        var d = start.plusDays(steps * interval)
        val out = mutableListOf<LocalDate>()
        while (!d.isAfter(hi)) {
            out += d
            d = d.plusDays(interval.toLong())
        }
        return out
    }

    private fun weekly(rule: RecurrenceRule, start: LocalDate, lo: LocalDate, hi: LocalDate): List<LocalDate> {
        val days = rule.daysOfWeek.ifEmpty { setOf(start.dayOfWeek) }
        val anchorWeek = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val out = mutableListOf<LocalDate>()
        var d = lo
        while (!d.isAfter(hi)) {
            val week = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weeksApart = ChronoUnit.WEEKS.between(anchorWeek, week)
            if (weeksApart % rule.interval == 0L && d.dayOfWeek in days) out += d
            d = d.plusDays(1)
        }
        return out
    }

    private fun monthly(rule: RecurrenceRule, start: LocalDate, lo: LocalDate, hi: LocalDate): List<LocalDate> {
        val wantedDay = rule.dayOfMonth ?: start.dayOfMonth
        val startMonth = YearMonth.from(start)
        var month = YearMonth.from(lo)
        // Align to the interval grid that starts at the start month.
        val monthsApart = ChronoUnit.MONTHS.between(startMonth, month)
        val rem = monthsApart % rule.interval
        if (rem != 0L) month = month.plusMonths(rule.interval - rem)

        val out = mutableListOf<LocalDate>()
        while (!month.atDay(1).isAfter(hi)) {
            val d = month.atDay(minOf(wantedDay, month.lengthOfMonth()))
            if (!d.isBefore(lo) && !d.isAfter(hi)) out += d
            month = month.plusMonths(rule.interval.toLong())
        }
        return out
    }
}
