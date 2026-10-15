package com.aquagrow.data.repository

import com.aquagrow.data.local.dao.AquariumDao
import com.aquagrow.data.local.dao.AquariumLogDao
import com.aquagrow.data.local.dao.FishDao
import com.aquagrow.data.mapper.toDomain
import com.aquagrow.data.mapper.toEntity
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.FeedingLog
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.MaintenanceLog
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.ReminderScheduler
import com.aquagrow.domain.time.TimeProvider
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class AquariumRepositoryImpl @Inject constructor(
    private val aquariumDao: AquariumDao,
    private val fishDao: FishDao,
    private val logDao: AquariumLogDao,
    private val time: TimeProvider,
    private val reminders: ReminderScheduler,
) : AquariumRepository {

    override fun observeSummaries() = aquariumDao.observeSummaries().map { l -> l.map { it.toDomain() } }
    override fun observeAquariums() = aquariumDao.observeActive().map { l -> l.map { it.toDomain() } }
    override fun observeAquarium(id: Long) = aquariumDao.observeById(id).map { it?.toDomain() }
    override suspend fun getAquarium(id: Long) = aquariumDao.getById(id)?.toDomain()
    override fun observeActiveCount() = aquariumDao.observeActiveCount()

    override suspend fun saveAquarium(aquarium: Aquarium): Long {
        val now = time.now()
        return if (aquarium.id == 0L) {
            aquariumDao.insert(aquarium.toEntity(now, now))
        } else {
            val createdAt = aquariumDao.getById(aquarium.id)?.createdAt ?: now
            aquariumDao.update(aquarium.toEntity(createdAt, now))
            reminders.requestReschedule()
            aquarium.id
        }
    }

    override suspend fun deleteAquarium(id: Long) {
        aquariumDao.getById(id)?.let { aquariumDao.delete(it) }
        reminders.requestReschedule()
    }

    override fun observeFish(aquariumId: Long) = fishDao.observeForAquarium(aquariumId).map { l -> l.map { it.toDomain() } }
    override fun observeActiveFish() = fishDao.observeActive().map { l -> l.map { it.toDomain() } }
    override fun observeFishById(id: Long) = fishDao.observeById(id).map { it?.toDomain() }
    override suspend fun getFish(id: Long) = fishDao.getById(id)?.toDomain()
    override suspend fun getFishForAquarium(aquariumId: Long) = fishDao.getForAquarium(aquariumId).map { it.toDomain() }
    override fun observeTotalFishCount() = fishDao.observeTotalCount()

    override suspend fun saveFish(fish: Fish): Long {
        val now = time.now()
        return if (fish.id == 0L) {
            fishDao.insert(fish.toEntity(now, now))
        } else {
            val createdAt = fishDao.getById(fish.id)?.createdAt ?: now
            fishDao.update(fish.toEntity(createdAt, now))
            reminders.requestReschedule()
            fish.id
        }
    }

    override suspend fun deleteFish(id: Long) {
        fishDao.getById(id)?.let { fishDao.delete(it) }
        reminders.requestReschedule()
    }

    override fun observeFeedingLogs(aquariumId: Long) = logDao.observeFeedingLogs(aquariumId).map { l -> l.map { it.toDomain() } }
    override fun observeRecentFeeding(limit: Int) = logDao.observeRecentFeeding(limit).map { l -> l.map { it.toDomain() } }
    override suspend fun addFeedingLog(log: FeedingLog) = logDao.insertFeedingLog(log.toEntity())
    override suspend fun deleteFeedingLog(log: FeedingLog) = logDao.deleteFeedingLog(log.toEntity())
    override suspend fun deleteFeedingForTask(taskId: Long, at: Instant) = logDao.deleteFeedingForTask(taskId, at)

    override fun observeWaterTests(aquariumId: Long) = logDao.observeWaterTests(aquariumId).map { l -> l.map { it.toDomain() } }
    override fun observeAllWaterTests() = logDao.observeAllWaterTests().map { l -> l.map { it.toDomain() } }
    override suspend fun addWaterTest(test: WaterTest) = logDao.insertWaterTest(test.toEntity())
    override suspend fun deleteWaterTest(test: WaterTest) = logDao.deleteWaterTest(test.toEntity())

    override fun observeMaintenanceLogs(aquariumId: Long) = logDao.observeMaintenanceLogs(aquariumId).map { l -> l.map { it.toDomain() } }
    override fun observeAllMaintenanceLogs() = logDao.observeAllMaintenanceLogs().map { l -> l.map { it.toDomain() } }
    override suspend fun addMaintenanceLog(log: MaintenanceLog) = logDao.insertMaintenanceLog(log.toEntity())
    override suspend fun deleteMaintenanceLog(log: MaintenanceLog) = logDao.deleteMaintenanceLog(log.toEntity())
    override suspend fun deleteMaintenanceForTask(taskId: Long, at: Instant) = logDao.deleteMaintenanceForTask(taskId, at)
}
