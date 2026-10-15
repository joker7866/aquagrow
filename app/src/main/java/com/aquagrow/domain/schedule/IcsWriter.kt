package com.aquagrow.domain.schedule

import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Writes tasks as an RFC 5545 iCalendar file. Each task becomes one VEVENT with an RRULE,
 * so calendar apps expand the repeats themselves. Times use the phone's current timezone.
 */
object IcsWriter {
    private val local = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val date = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val utc = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    fun write(tasks: List<CareTask>, zone: ZoneId, now: Instant, defaultTime: LocalTime = LocalTime.of(7, 0)): String = buildString {
        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//AquaGrow//Care calendar//EN")
        line("CALSCALE:GREGORIAN")
        tasks.filter { it.enabled }.forEach { t ->
            val start = t.startDate.atTime(t.reminderTime ?: defaultTime)
            line("BEGIN:VEVENT")
            line("UID:aquagrow-task-${t.id}@aquagrow.local")
            line("DTSTAMP:${utc.format(now)}")
            line("DTSTART;TZID=${zone.id}:${local.format(start)}")
            line("DURATION:PT15M")
            line("SUMMARY:${escape(listOfNotNull(t.title, t.subjectName).joinToString(" · "))}")
            t.notes?.let { line("DESCRIPTION:${escape(it)}") }
            rrule(t)?.let { line("RRULE:$it") }
            line("END:VEVENT")
        }
        line("END:VCALENDAR")
    }

    fun rrule(t: CareTask): String? {
        val r = t.recurrence
        val until = t.endDate?.let { ";UNTIL=${date.format(it)}" }.orEmpty()
        return when (r.type) {
            RecurrenceType.ONCE -> null

            RecurrenceType.DAILY -> "FREQ=DAILY;INTERVAL=${r.interval}$until"

            RecurrenceType.WEEKLY -> {
                val days = r.daysOfWeek.ifEmpty { setOf(t.startDate.dayOfWeek) }.sorted().joinToString(",") { it.ics() }
                "FREQ=WEEKLY;INTERVAL=${r.interval};BYDAY=$days$until"
            }

            RecurrenceType.MONTHLY -> {
                val day = r.dayOfMonth ?: t.startDate.dayOfMonth
                // BYMONTHDAY=31 skips short months in most calendars; -1 means "last day" and matches our clamping.
                val by = if (day >= 29) "-1" else day.toString()
                "FREQ=MONTHLY;INTERVAL=${r.interval};BYMONTHDAY=$by$until"
            }
        }
    }

    private fun DayOfWeek.ics() = name.take(2)

    /** Escapes text values per RFC 5545 (backslash, semicolon, comma, newline). */
    fun escape(s: String) = s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")

    /** Lines must be CRLF-terminated and folded at 75 octets. */
    private fun StringBuilder.line(content: String) {
        var rest = content
        var first = true
        while (rest.toByteArray(Charsets.UTF_8).size > 75) {
            var cut = 74.coerceAtMost(rest.length)
            while (rest.substring(0, cut).toByteArray(Charsets.UTF_8).size > (if (first) 75 else 74)) cut--
            append(if (first) "" else " ").append(rest, 0, cut).append("\r\n")
            rest = rest.substring(cut)
            first = false
        }
        append(if (first) "" else " ").append(rest).append("\r\n")
    }
}
