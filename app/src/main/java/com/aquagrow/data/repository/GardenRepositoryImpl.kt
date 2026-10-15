package com.aquagrow.data.repository

import com.aquagrow.data.local.dao.GardenLogDao
import com.aquagrow.data.local.dao.PhotoDao
import com.aquagrow.data.local.dao.PlantDao
import com.aquagrow.data.local.dao.SowingGuideDao
import com.aquagrow.data.mapper.toDomain
import com.aquagrow.data.mapper.toEntity
import com.aquagrow.domain.model.FertilizerLog
import com.aquagrow.domain.model.GrowthLog
import com.aquagrow.domain.model.HarvestLog
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.SowingGuide
import com.aquagrow.domain.model.WateringLog
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.ReminderScheduler
import com.aquagrow.domain.time.TimeProvider
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class GardenRepositoryImpl @Inject constructor(
    private val plantDao: PlantDao,
    private val logDao: GardenLogDao,
    private val photoDao: PhotoDao,
    private val sowingGuideDao: SowingGuideDao,
    private val time: TimeProvider,
    private val reminders: ReminderScheduler,
) : GardenRepository {

    override fun observePlants() = plantDao.observeActive().map { list -> list.map { it.toDomain() } }
    override fun observePlant(id: Long) = plantDao.observeById(id).map { it?.toDomain() }
    override suspend fun getPlant(id: Long) = plantDao.getById(id)?.toDomain()
    override fun observeActivePlantCount() = plantDao.observeActiveCount()

    override suspend fun savePlant(plant: Plant): Long {
        val now = time.now()
        return if (plant.id == 0L) {
            plantDao.insert(plant.toEntity(createdAt = now, updatedAt = now))
        } else {
            val createdAt = plantDao.getById(plant.id)?.createdAt ?: now
            plantDao.update(plant.toEntity(createdAt = createdAt, updatedAt = now))
            // Archiving or renaming changes which reminders should fire and their text.
            reminders.requestReschedule()
            plant.id
        }
    }

    override suspend fun deletePlant(id: Long) {
        plantDao.getById(id)?.let { plantDao.delete(it) } // cascades to logs, tasks, photos
        reminders.requestReschedule()
    }

    override fun observeGrowthLogs(plantId: Long): Flow<List<GrowthLog>> = combine(logDao.observeGrowthLogs(plantId), photoDao.observeForPlant(plantId)) { logs, photos ->
        val byLog = photos.filter { it.growthLogId != null }.groupBy({ it.growthLogId }, { it.filePath })
        logs.map { it.toDomain(byLog[it.id].orEmpty()) }
    }

    override fun observeAllGrowthLogs() = logDao.observeAllGrowthLogs().map { l -> l.map { it.toDomain() } }
    override suspend fun addGrowthLog(log: GrowthLog) = logDao.insertGrowthLog(log.toEntity())
    override suspend fun deleteGrowthLog(log: GrowthLog) = logDao.deleteGrowthLog(log.toEntity())

    override fun observeWateringLogs(plantId: Long) = logDao.observeWateringLogs(plantId).map { l -> l.map { it.toDomain() } }
    override fun observeRecentWatering(limit: Int) = logDao.observeRecentWatering(limit).map { l -> l.map { it.toDomain() } }
    override fun observeLastWatered(plantId: Long) = logDao.observeLastWatered(plantId)
    override suspend fun addWateringLog(log: WateringLog) = logDao.insertWateringLog(log.toEntity())
    override suspend fun deleteWateringLog(log: WateringLog) = logDao.deleteWateringLog(log.toEntity())
    override suspend fun deleteWateringForTask(taskId: Long, at: Instant) = logDao.deleteWateringForTask(taskId, at)

    override fun observeFertilizerLogs(plantId: Long) = logDao.observeFertilizerLogs(plantId).map { l -> l.map { it.toDomain() } }
    override fun observeAllFertilizerLogs() = logDao.observeAllFertilizerLogs().map { l -> l.map { it.toDomain() } }
    override suspend fun addFertilizerLog(log: FertilizerLog) = logDao.insertFertilizerLog(log.toEntity())
    override suspend fun deleteFertilizerLog(log: FertilizerLog) = logDao.deleteFertilizerLog(log.toEntity())
    override suspend fun deleteFertilizerForTask(taskId: Long, at: Instant) = logDao.deleteFertilizerForTask(taskId, at)

    override fun observeHarvestLogs(plantId: Long) = logDao.observeHarvestLogs(plantId).map { l -> l.map { it.toDomain() } }
    override fun observeAllHarvestLogs() = logDao.observeAllHarvestLogs().map { l -> l.map { it.toDomain() } }
    override suspend fun addHarvestLog(log: HarvestLog) = logDao.insertHarvestLog(log.toEntity())
    override suspend fun deleteHarvestLog(log: HarvestLog) = logDao.deleteHarvestLog(log.toEntity())

    override fun observeSowingGuides() = sowingGuideDao.observeAll().map { l -> l.map { it.toDomain() } }
    override suspend fun saveSowingGuide(guide: SowingGuide) = sowingGuideDao.upsert(guide.toEntity())
    override suspend fun deleteSowingGuide(guide: SowingGuide) = sowingGuideDao.delete(guide.toEntity())
}
