package com.aquagrow.domain.reminders

import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.NotificationSettings
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** One Android notification channel per kind of reminder, so users can tune them in system settings too. */
enum class ReminderChannel {
    WATERING,
    FERTILIZER,
    SOWING,
    TRANSPLANTING,
    HARVEST,
    FEEDING,
    WATER_CHANGE,
    TESTING,
    FILTER,
    GENERAL,
    MISSED,
    INVENTORY,
    ;

    companion object {
        fun forTask(type: TaskType, maintenance: MaintenanceType?): ReminderChannel = when (type) {
            TaskType.WATERING -> WATERING

            TaskType.FERTILIZING -> FERTILIZER

            TaskType.SOWING -> SOWING

            TaskType.TRANSPLANTING -> TRANSPLANTING

            TaskType.HARVEST -> HARVEST

            TaskType.FEEDING -> FEEDING

            TaskType.WATER_TEST -> TESTING

            TaskType.MAINTENANCE -> when (maintenance) {
                MaintenanceType.WATER_CHANGE -> WATER_CHANGE
                MaintenanceType.FILTER_INSPECTION, MaintenanceType.FILTER_MAINTENANCE -> FILTER
                else -> GENERAL
            }

            TaskType.CUSTOM -> GENERAL
        }

        /** Channels the user can switch on/off individually (MISSED/INVENTORY have their own toggles). */
        val userToggleable = listOf(WATERING, FERTILIZER, SOWING, TRANSPLANTING, HARVEST, FEEDING, WATER_CHANGE, TESTING, FILTER, GENERAL)
    }
}

object QuietHours {
    /** True if [time] is inside the window; windows may cross midnight (22:00 → 06:00). */
    fun contains(time: LocalTime, start: LocalTime, end: LocalTime): Boolean = when {
        start == end -> false
        start < end -> time >= start && time < end
        else -> time >= start || time < end
    }

    /**
     * Delays (never drops) a reminder that falls inside quiet hours to the moment they end.
     * E.g. 23:30 with 22:00–06:00 → 06:00 next day; 05:00 → 06:00 same day.
     */
    fun shift(dateTime: LocalDateTime, start: LocalTime, end: LocalTime): LocalDateTime {
        val t = dateTime.toLocalTime()
        if (!contains(t, start, end)) return dateTime
        val endToday = dateTime.toLocalDate().atTime(end)
        return if (start < end || t < end) endToday else endToday.plusDays(1)
    }
}

data class PlannedReminder(
    val taskId: Long,
    val originalDate: LocalDate,
    val triggerAt: Instant,
    val channel: ReminderChannel,
) {
    /** One unique key per occurrence: WorkManager unique work names use it, so there are never duplicates. */
    val key: String get() = keyFor(taskId, originalDate)

    companion object {
        fun keyFor(taskId: Long, date: LocalDate) = "rem-$taskId-$date"
    }
}

object ReminderPlanner {
    /**
     * Turns occurrences into concrete reminder times.
     * - Reminder time = task time, else the default time from settings.
     * - An active snooze replaces that time.
     * - Quiet hours delay the reminder to their end.
     * - Handled occurrences, disabled channels and times already past (beyond [graceMinutes]) are skipped.
     * Each occurrence yields at most one reminder (unique by [PlannedReminder.key]).
     */
    fun plan(
        occurrences: List<TaskOccurrence>,
        settings: NotificationSettings,
        now: Instant,
        zone: ZoneId,
        graceMinutes: Long = 10,
    ): List<PlannedReminder> {
        if (!settings.remindersEnabled) return emptyList()
        return occurrences
            .asSequence()
            .filter { !it.isHandled && it.task.enabled }
            .mapNotNull { occ ->
                val channel = ReminderChannel.forTask(occ.task.type, occ.task.maintenanceType)
                if (channel.name in settings.disabledChannels) return@mapNotNull null
                val base = occ.snoozedUntil?.atZone(zone)?.toLocalDateTime()
                    ?: occ.date.atTime(occ.task.reminderTime ?: settings.defaultReminderTime)
                val shifted = if (settings.quietHoursEnabled) QuietHours.shift(base, settings.quietHoursStart, settings.quietHoursEnd) else base
                val at = shifted.atZone(zone).toInstant()
                if (at.isBefore(now.minusSeconds(graceMinutes * 60))) return@mapNotNull null
                PlannedReminder(occ.task.id, occ.originalDate, maxOf(at, now), channel)
            }
            .distinctBy { it.key }
            .sortedBy { it.triggerAt }
            .toList()
    }
}
