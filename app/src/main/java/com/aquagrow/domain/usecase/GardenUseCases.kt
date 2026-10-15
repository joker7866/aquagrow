package com.aquagrow.domain.usecase

import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.FertilizerLog
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.GrowthLog
import com.aquagrow.domain.model.Photo
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.PlantCondition
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.SoilMoisture
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WateringLog
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.PhotoRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.time.TimeProvider
import java.time.LocalDate
import javax.inject.Inject

data class FertilizerPlan(
    val schedule: ScheduleInput,
    val type: FertilizerType,
    val productName: String?,
    /** Whatever the user typed (from the product label). Never pre-filled by the app. */
    val amountText: String?,
)

/**
 * Saves a plant and keeps its care tasks in sync: every schedule chosen on the form becomes
 * (or updates, or removes) a TaskSchedule row linked to the plant.
 */
class SavePlantUseCase @Inject constructor(
    private val garden: GardenRepository,
    private val tasks: TaskRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(plant: Plant, watering: ScheduleInput?, fertilizer: FertilizerPlan?): Long {
        val previousCover = if (plant.id != 0L) garden.getPlant(plant.id)?.coverPhotoPath else null
        val id = garden.savePlant(plant.copy(name = plant.name.trim(), variety = plant.variety?.trim()?.ifBlank { null }))

        // A newly chosen cover photo also goes into the plant's photo gallery.
        val cover = plant.coverPhotoPath
        if (cover != null && cover != previousCover) {
            photos.addPhoto(Photo(filePath = cover, takenAt = time.now(), plantId = id))
        }

        val existing = tasks.getTasksForPlant(id)
        syncTask(
            existing.firstOrNull { it.type == TaskType.WATERING },
            watering?.let {
                CareTask(
                    title = "Water",
                    module = CareModule.GARDEN,
                    type = TaskType.WATERING,
                    plantId = id,
                    startDate = it.startDate,
                    reminderTime = it.time,
                    recurrence = it.toRule(),
                )
            },
        )
        syncTask(
            existing.firstOrNull { it.type == TaskType.FERTILIZING && it.recurrence != RecurrenceRule.Once },
            fertilizer?.let {
                CareTask(
                    title = "Fertilize", module = CareModule.GARDEN, type = TaskType.FERTILIZING, plantId = id,
                    fertilizerType = it.type, itemName = it.productName?.ifBlank { null },
                    amountText = it.amountText?.ifBlank { null },
                    startDate = it.schedule.startDate, reminderTime = it.schedule.time, recurrence = it.schedule.toRule(),
                )
            },
        )
        return id
    }

    private suspend fun syncTask(existing: CareTask?, wanted: CareTask?) {
        when {
            wanted == null && existing != null -> tasks.deleteTask(existing.id)
            wanted != null -> tasks.saveTask(wanted.copy(id = existing?.id ?: 0, notes = existing?.notes))
        }
    }
}

/** Finds the watering/fertilizing occurrence that is due today or overdue for a plant. */
class FindDueOccurrenceUseCase @Inject constructor(
    private val tasks: TaskRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(
        plantId: Long? = null,
        aquariumId: Long? = null,
        type: TaskType,
        extra: (CareTask) -> Boolean = { true },
    ): TaskOccurrence? {
        val today = time.today()
        val candidates = tasks.getOverdue(today) + tasks.getOccurrences(today, today)
        return candidates
            .filter { !it.isHandled && it.task.type == type }
            .filter { plantId == null || it.task.plantId == plantId }
            .filter { aquariumId == null || it.task.aquariumId == aquariumId }
            .filter { extra(it.task) }
            .minByOrNull { it.date }
    }
}

/**
 * Manual watering log from the Record watering sheet. If a watering occurrence is due,
 * it is completed (or skipped when the user checked the soil and decided not to water),
 * sharing the same timestamp so undo removes exactly this log.
 */
class RecordWateringUseCase @Inject constructor(
    private val garden: GardenRepository,
    private val findDue: FindDueOccurrenceUseCase,
    private val actions: OccurrenceActions,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(
        plantId: Long,
        watered: Boolean,
        moisture: SoilMoisture?,
        amountMl: Int?,
        note: String?,
    ) {
        val now = time.now()
        val due = findDue(plantId = plantId, type = TaskType.WATERING)
        garden.addWateringLog(
            WateringLog(
                plantId = plantId,
                taskId = due?.task?.id,
                loggedAt = now,
                watered = watered,
                soilMoisture = moisture,
                amountMl = amountMl,
                note = note?.trim()?.ifBlank { null },
            ),
        )
        if (due != null) {
            if (watered) {
                actions.markDone(due.task.id, due.originalDate, writeLog = false, at = now)
            } else {
                actions.skip(due.task.id, due.originalDate, at = now)
            }
        }
    }
}

class AddGrowthLogUseCase @Inject constructor(
    private val garden: GardenRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(
        plant: Plant,
        stageIndex: Int?,
        heightCm: Double?,
        condition: PlantCondition?,
        note: String?,
        photoPath: String?,
    ) {
        val now = time.now()
        val newStage = stageIndex?.takeIf { it in plant.growthStages.indices }
        val logId = garden.addGrowthLog(
            GrowthLog(
                plantId = plant.id,
                loggedAt = now,
                stageIndex = newStage,
                stageName = newStage?.let { plant.growthStages[it] },
                heightCm = heightCm,
                condition = condition,
                note = note?.trim()?.ifBlank { null },
            ),
        )
        if (photoPath != null) {
            photos.addPhoto(Photo(filePath = photoPath, takenAt = now, plantId = plant.id, growthLogId = logId))
        }
        val updated = plant.copy(
            currentStageIndex = newStage ?: plant.currentStageIndex,
            condition = condition ?: plant.condition,
            coverPhotoPath = plant.coverPhotoPath ?: photoPath,
        )
        if (updated != plant) garden.savePlant(updated)
    }
}

/** Tapping a stage on the stepper: updates the plant and records it in the timeline. */
class SetGrowthStageUseCase @Inject constructor(
    private val garden: GardenRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(plant: Plant, index: Int) {
        if (index !in plant.growthStages.indices || index == plant.currentStageIndex) return
        garden.savePlant(plant.copy(currentStageIndex = index))
        garden.addGrowthLog(
            GrowthLog(plantId = plant.id, loggedAt = time.now(), stageIndex = index, stageName = plant.growthStages[index]),
        )
    }
}

/**
 * Logs a fertilizer application, completes a due fertilizer occurrence, and optionally
 * schedules a one-off reminder for the next application on a date the user picks.
 */
class AddFertilizerLogUseCase @Inject constructor(
    private val garden: GardenRepository,
    private val tasks: TaskRepository,
    private val findDue: FindDueOccurrenceUseCase,
    private val actions: OccurrenceActions,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(log: FertilizerLog, nextDate: LocalDate?) {
        val now = time.now()
        val due = findDue(plantId = log.plantId, type = TaskType.FERTILIZING)
        garden.addFertilizerLog(log.copy(appliedAt = now, taskId = due?.task?.id))
        if (due != null) actions.markDone(due.task.id, due.originalDate, writeLog = false, at = now)
        if (nextDate != null) {
            tasks.saveTask(
                CareTask(
                    title = "Fertilize",
                    module = CareModule.GARDEN,
                    type = TaskType.FERTILIZING,
                    plantId = log.plantId,
                    fertilizerType = log.type,
                    itemName = log.productName,
                    startDate = nextDate,
                    recurrence = RecurrenceRule.Once,
                ),
            )
        }
    }
}
