package com.aquagrow.domain.usecase

import com.aquagrow.domain.model.FeedingLog
import com.aquagrow.domain.model.FertilizerLog
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.MaintenanceLog
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.OccurrenceState
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WateringLog
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Everything the user can do to one occurrence: done / undo / skip / snooze / reschedule.
 * Marking done also writes the matching care log (watering, feeding, ...) so history is the
 * same whether the user ticks the task on Home, in a notification, or on a detail screen.
 */
class OccurrenceActions @Inject constructor(
    private val tasks: TaskRepository,
    private val garden: GardenRepository,
    private val aquarium: AquariumRepository,
    private val time: TimeProvider,
) {
    /**
     * Idempotent: a second call for the same occurrence does nothing, so a double tap or a
     * notification action racing the app can never create two logs.
     * Pass [writeLog] = false when the caller already recorded its own, more detailed log.
     */
    suspend fun markDone(
        taskId: Long,
        originalDate: LocalDate,
        writeLog: Boolean = true,
        at: Instant = time.now(),
    ): Boolean {
        val existing = tasks.getState(taskId, originalDate)
        if (existing?.status == OccurrenceStatus.DONE) return false
        val task = tasks.getTask(taskId) ?: return false
        val now = at
        tasks.putState(
            OccurrenceState(
                taskId = taskId,
                originalDate = originalDate,
                status = OccurrenceStatus.DONE,
                rescheduledDate = existing?.rescheduledDate,
                handledAt = now,
            ),
        )
        if (writeLog) writeCareLog(task.id, task.type, now)
        return true
    }

    /** Reverts done/skipped back to pending and removes the log that marking created. */
    suspend fun undo(taskId: Long, originalDate: LocalDate) {
        val existing = tasks.getState(taskId, originalDate) ?: return
        val at = existing.handledAt
        if (at != null) deleteCareLog(taskId, at)
        if (existing.rescheduledDate != null) {
            tasks.putState(existing.copy(status = OccurrenceStatus.PENDING, handledAt = null))
        } else {
            tasks.clearState(taskId, originalDate)
        }
    }

    suspend fun skip(taskId: Long, originalDate: LocalDate, at: Instant = time.now(), logFeeding: Boolean = true) {
        val existing = tasks.getState(taskId, originalDate)
        if (existing?.status == OccurrenceStatus.SKIPPED) return
        val task = tasks.getTask(taskId) ?: return
        val now = at
        if (existing?.status == OccurrenceStatus.DONE) existing.handledAt?.let { deleteCareLog(taskId, it) }
        tasks.putState(
            OccurrenceState(
                taskId = taskId,
                originalDate = originalDate,
                status = OccurrenceStatus.SKIPPED,
                rescheduledDate = existing?.rescheduledDate,
                handledAt = now,
            ),
        )
        // Skipped feedings are part of the feeding history.
        if (logFeeding && task.type == TaskType.FEEDING && task.aquariumId != null) {
            aquarium.addFeedingLog(
                FeedingLog(
                    aquariumId = task.aquariumId,
                    fishId = task.fishId,
                    taskId = taskId,
                    fedAt = now,
                    foodName = task.itemName,
                    skipped = true,
                ),
            )
        }
    }

    /** Moves the occurrence to [newDate] (pending again). */
    suspend fun reschedule(taskId: Long, originalDate: LocalDate, newDate: LocalDate) {
        val existing = tasks.getState(taskId, originalDate)
        existing?.handledAt?.let { if (existing.status != OccurrenceStatus.PENDING) deleteCareLog(taskId, it) }
        tasks.putState(
            OccurrenceState(
                taskId = taskId,
                originalDate = originalDate,
                status = OccurrenceStatus.PENDING,
                rescheduledDate = newDate.takeIf { it != originalDate },
            ),
        )
    }

    /** Delays the reminder only; the occurrence stays on its date. */
    suspend fun snooze(taskId: Long, originalDate: LocalDate, until: Instant) {
        val existing = tasks.getState(taskId, originalDate)
        if (existing != null && existing.status != OccurrenceStatus.PENDING) return
        tasks.putState(
            (existing ?: OccurrenceState(taskId, originalDate, OccurrenceStatus.PENDING)).copy(snoozedUntil = until),
        )
    }

    private suspend fun writeCareLog(taskId: Long, type: TaskType, at: Instant) {
        val task = tasks.getTask(taskId) ?: return
        when (type) {
            TaskType.WATERING -> task.plantId?.let {
                garden.addWateringLog(WateringLog(plantId = it, taskId = taskId, loggedAt = at, watered = true))
            }

            TaskType.FERTILIZING -> task.plantId?.let {
                garden.addFertilizerLog(
                    FertilizerLog(
                        plantId = it,
                        taskId = taskId,
                        appliedAt = at,
                        type = task.fertilizerType ?: FertilizerType.CUSTOM,
                        productName = task.itemName,
                    ),
                )
            }

            TaskType.FEEDING -> task.aquariumId?.let {
                aquarium.addFeedingLog(
                    FeedingLog(
                        aquariumId = it,
                        fishId = task.fishId,
                        taskId = taskId,
                        fedAt = at,
                        foodName = task.itemName,
                        amount = task.amountText,
                    ),
                )
            }

            TaskType.MAINTENANCE -> task.aquariumId?.let {
                aquarium.addMaintenanceLog(
                    MaintenanceLog(
                        aquariumId = it,
                        taskId = taskId,
                        performedAt = at,
                        type = task.maintenanceType ?: MaintenanceType.OTHER,
                    ),
                )
            }

            // Water tests need readings and harvests need a quantity: the user logs those
            // explicitly, so ticking the task only records that it was done.
            else -> Unit
        }
    }

    private suspend fun deleteCareLog(taskId: Long, at: Instant) {
        garden.deleteWateringForTask(taskId, at)
        garden.deleteFertilizerForTask(taskId, at)
        aquarium.deleteFeedingForTask(taskId, at)
        aquarium.deleteMaintenanceForTask(taskId, at)
    }
}
