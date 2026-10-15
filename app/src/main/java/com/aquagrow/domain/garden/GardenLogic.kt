package com.aquagrow.domain.garden

import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class HarvestPhase { UPCOMING, IN_WINDOW, PAST_WINDOW }

/**
 * A rough harvest window computed from typical days-to-harvest. Always presented to the user
 * as an estimate: real timing depends on variety, weather, pot size and care.
 */
data class HarvestEstimate(
    val earliest: LocalDate,
    val latest: LocalDate,
    val phase: HarvestPhase,
    /** Days until the window opens (0 or negative once it has opened). */
    val daysToEarliest: Long,
    /** 0..1 progress from planting to the start of the window. */
    val progress: Float,
)

object HarvestEstimator {
    fun estimate(plantingDate: LocalDate, minDays: Int?, maxDays: Int?, today: LocalDate): HarvestEstimate? {
        if (minDays == null || minDays <= 0) return null
        val max = (maxDays ?: minDays).coerceAtLeast(minDays)
        val earliest = plantingDate.plusDays(minDays.toLong())
        val latest = plantingDate.plusDays(max.toLong())
        val phase = when {
            today.isBefore(earliest) -> HarvestPhase.UPCOMING
            today.isAfter(latest) -> HarvestPhase.PAST_WINDOW
            else -> HarvestPhase.IN_WINDOW
        }
        val elapsed = ChronoUnit.DAYS.between(plantingDate, today).coerceAtLeast(0)
        return HarvestEstimate(
            earliest = earliest,
            latest = latest,
            phase = phase,
            daysToEarliest = ChronoUnit.DAYS.between(today, earliest),
            progress = (elapsed.toFloat() / minDays).coerceIn(0f, 1f),
        )
    }
}

sealed interface DueStatus {
    data class Overdue(val days: Long) : DueStatus
    data object Today : DueStatus
    data class Upcoming(val days: Long) : DueStatus

    companion object {
        fun of(date: LocalDate, today: LocalDate): DueStatus {
            val diff = ChronoUnit.DAYS.between(today, date)
            return when {
                diff < 0 -> Overdue(-diff)
                diff == 0L -> Today
                else -> Upcoming(diff)
            }
        }
    }
}

object CareDue {
    /**
     * The next occurrence that still needs doing for a plant's task of [type]:
     * an overdue one if any, otherwise the first pending one on/after today.
     * [upcoming] must cover today onward; [overdue] comes from OccurrenceResolver.overdue.
     */
    fun nextForPlant(
        plantId: Long,
        type: TaskType,
        upcoming: List<TaskOccurrence>,
        overdue: List<TaskOccurrence>,
        today: LocalDate,
    ): TaskOccurrence? {
        fun TaskOccurrence.matches() = task.plantId == plantId && task.type == type
        return overdue.filter { it.matches() }.minByOrNull { it.date }
            ?: upcoming.filter { it.matches() && !it.isHandled && !it.date.isBefore(today) }.minByOrNull { it.date }
    }
}
