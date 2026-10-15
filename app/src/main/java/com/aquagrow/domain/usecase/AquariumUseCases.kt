package com.aquagrow.domain.usecase

import com.aquagrow.domain.aquarium.FeedingPlan
import com.aquagrow.domain.aquarium.FeedingScheduleGenerator
import com.aquagrow.domain.aquarium.TankVolume
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.FeedingLog
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.MaintenanceLog
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.Photo
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.PhotoRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.time.TimeProvider
import javax.inject.Inject

class SaveAquariumUseCase @Inject constructor(
    private val aquarium: AquariumRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(tank: Aquarium): Long {
        val previousCover = if (tank.id != 0L) aquarium.getAquarium(tank.id)?.coverPhotoPath else null
        // Volume follows the dimensions unless the user typed their own value.
        val volume = if (tank.volumeIsManual) tank.volumeLiters else TankVolume.liters(tank.lengthCm, tank.widthCm, tank.heightCm) ?: tank.volumeLiters
        val id = aquarium.saveAquarium(tank.copy(name = tank.name.trim(), volumeLiters = volume))
        if (tank.coverPhotoPath != null && tank.coverPhotoPath != previousCover) {
            photos.addPhoto(Photo(filePath = tank.coverPhotoPath, takenAt = time.now(), aquariumId = id))
        }
        return id
    }
}

class SaveFishUseCase @Inject constructor(
    private val aquarium: AquariumRepository,
    private val photos: PhotoRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(fish: Fish): Long {
        val previous = if (fish.id != 0L) aquarium.getFish(fish.id)?.photoPath else null
        val id = aquarium.saveFish(fish.copy(speciesName = fish.speciesName.trim(), nickname = fish.nickname?.trim()?.ifEmpty { null }))
        if (fish.photoPath != null && fish.photoPath != previous) {
            photos.addPhoto(Photo(filePath = fish.photoPath, takenAt = time.now(), aquariumId = fish.aquariumId, fishId = id))
        }
        return id
    }
}

/**
 * Replaces the feeding tasks of one target (whole tank, or one fish group) with tasks
 * generated from [plan]. Other targets' schedules are untouched, so each species keeps
 * its own schedule.
 */
class SaveFeedingScheduleUseCase @Inject constructor(private val tasks: TaskRepository) {
    suspend operator fun invoke(aquariumId: Long, plan: FeedingPlan) {
        tasks.getTasksForAquarium(aquariumId)
            .filter { it.type == TaskType.FEEDING && it.fishId == plan.fishId }
            .forEach { tasks.deleteTask(it.id) }
        FeedingScheduleGenerator.generate(aquariumId, listOf(plan)).forEach { tasks.saveTask(it) }
    }

    suspend fun clear(aquariumId: Long, fishId: Long?) {
        tasks.getTasksForAquarium(aquariumId)
            .filter { it.type == TaskType.FEEDING && it.fishId == fishId }
            .forEach { tasks.deleteTask(it.id) }
    }
}

/** Creates or updates a maintenance / water-test schedule for a tank. Null schedule removes it. */
class SaveTankScheduleUseCase @Inject constructor(private val tasks: TaskRepository) {
    suspend operator fun invoke(aquariumId: Long, type: TaskType, maintenanceType: MaintenanceType?, title: String, schedule: ScheduleInput?) {
        val existing = tasks.getTasksForAquarium(aquariumId)
            .firstOrNull { it.type == type && it.maintenanceType == maintenanceType }
        if (schedule == null) {
            existing?.let { tasks.deleteTask(it.id) }
            return
        }
        tasks.saveTask(
            CareTask(
                id = existing?.id ?: 0,
                title = title,
                module = CareModule.AQUARIUM,
                type = type,
                aquariumId = aquariumId,
                maintenanceType = maintenanceType,
                startDate = schedule.startDate,
                reminderTime = schedule.time,
                recurrence = schedule.toRule(),
                notes = existing?.notes,
            ),
        )
    }
}

class LogWaterTestUseCase @Inject constructor(
    private val aquarium: AquariumRepository,
    private val findDue: FindDueOccurrenceUseCase,
    private val actions: OccurrenceActions,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(test: WaterTest): Long {
        val now = time.now()
        val id = aquarium.addWaterTest(test.copy(testedAt = now))
        findDue(aquariumId = test.aquariumId, type = TaskType.WATER_TEST)?.let {
            actions.markDone(it.task.id, it.originalDate, writeLog = false, at = now)
        }
        return id
    }
}

class LogMaintenanceUseCase @Inject constructor(
    private val aquarium: AquariumRepository,
    private val findDue: FindDueOccurrenceUseCase,
    private val actions: OccurrenceActions,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(log: MaintenanceLog) {
        val now = time.now()
        val due = findDue(aquariumId = log.aquariumId, type = TaskType.MAINTENANCE) { it.maintenanceType == log.type }
        aquarium.addMaintenanceLog(log.copy(performedAt = now, taskId = due?.task?.id))
        due?.let { actions.markDone(it.task.id, it.originalDate, writeLog = false, at = now) }
    }
}

class LogFeedingUseCase @Inject constructor(
    private val aquarium: AquariumRepository,
    private val findDue: FindDueOccurrenceUseCase,
    private val actions: OccurrenceActions,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(log: FeedingLog) {
        val now = time.now()
        val due = findDue(aquariumId = log.aquariumId, type = TaskType.FEEDING) { log.fishId == null || it.fishId == log.fishId }
        aquarium.addFeedingLog(log.copy(fedAt = now, taskId = due?.task?.id))
        due?.let { actions.markDone(it.task.id, it.originalDate, writeLog = false, at = now) }
    }
}
