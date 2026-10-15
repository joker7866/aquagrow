package com.aquagrow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.aquagrow.data.local.entity.FertilizerLogEntity
import com.aquagrow.data.local.entity.HarvestLogEntity
import com.aquagrow.data.local.entity.PlantEntity
import com.aquagrow.data.local.entity.PlantGrowthLogEntity
import com.aquagrow.data.local.entity.SowingGuideEntity
import com.aquagrow.data.local.entity.WateringLogEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants WHERE archived = 0 ORDER BY name COLLATE NOCASE, id")
    fun observeActive(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun observeById(id: Long): Flow<PlantEntity?>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getById(id: Long): PlantEntity?

    @Query("SELECT COUNT(*) FROM plants WHERE archived = 0")
    fun observeActiveCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM plants
        WHERE name LIKE '%' || :q || '%' OR variety LIKE '%' || :q || '%'
           OR notes LIKE '%' || :q || '%' OR location LIKE '%' || :q || '%'
        ORDER BY archived, name COLLATE NOCASE LIMIT 50
        """,
    )
    suspend fun search(q: String): List<PlantEntity>

    @Insert
    suspend fun insert(plant: PlantEntity): Long

    @Update
    suspend fun update(plant: PlantEntity)

    @Delete
    suspend fun delete(plant: PlantEntity)
}

@Dao
interface GardenLogDao {
    // Growth
    @Query("SELECT * FROM plant_growth_logs WHERE plantId = :plantId ORDER BY loggedAt DESC")
    fun observeGrowthLogs(plantId: Long): Flow<List<PlantGrowthLogEntity>>

    @Query("SELECT * FROM plant_growth_logs ORDER BY loggedAt")
    fun observeAllGrowthLogs(): Flow<List<PlantGrowthLogEntity>>

    @Query("SELECT * FROM plant_growth_logs WHERE note LIKE '%' || :q || '%' ORDER BY loggedAt DESC LIMIT 30")
    suspend fun searchGrowthNotes(q: String): List<PlantGrowthLogEntity>

    @Insert
    suspend fun insertGrowthLog(log: PlantGrowthLogEntity): Long

    @Delete
    suspend fun deleteGrowthLog(log: PlantGrowthLogEntity)

    // Watering
    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY loggedAt DESC")
    fun observeWateringLogs(plantId: Long): Flow<List<WateringLogEntity>>

    @Query("SELECT * FROM watering_logs ORDER BY loggedAt DESC LIMIT :limit")
    fun observeRecentWatering(limit: Int): Flow<List<WateringLogEntity>>

    @Query("SELECT MAX(loggedAt) FROM watering_logs WHERE plantId = :plantId AND watered = 1")
    fun observeLastWatered(plantId: Long): Flow<Instant?>

    @Insert
    suspend fun insertWateringLog(log: WateringLogEntity): Long

    @Delete
    suspend fun deleteWateringLog(log: WateringLogEntity)

    @Query("DELETE FROM watering_logs WHERE taskId = :taskId AND loggedAt = :at")
    suspend fun deleteWateringForTask(taskId: Long, at: Instant)

    // Fertilizer
    @Query("SELECT * FROM fertilizer_logs WHERE plantId = :plantId ORDER BY appliedAt DESC")
    fun observeFertilizerLogs(plantId: Long): Flow<List<FertilizerLogEntity>>

    @Query("SELECT * FROM fertilizer_logs ORDER BY appliedAt DESC")
    fun observeAllFertilizerLogs(): Flow<List<FertilizerLogEntity>>

    @Insert
    suspend fun insertFertilizerLog(log: FertilizerLogEntity): Long

    @Delete
    suspend fun deleteFertilizerLog(log: FertilizerLogEntity)

    @Query("DELETE FROM fertilizer_logs WHERE taskId = :taskId AND appliedAt = :at")
    suspend fun deleteFertilizerForTask(taskId: Long, at: Instant)

    // Harvest
    @Query("SELECT * FROM harvest_logs WHERE plantId = :plantId ORDER BY harvestedAt DESC")
    fun observeHarvestLogs(plantId: Long): Flow<List<HarvestLogEntity>>

    @Query("SELECT * FROM harvest_logs ORDER BY harvestedAt DESC")
    fun observeAllHarvestLogs(): Flow<List<HarvestLogEntity>>

    @Insert
    suspend fun insertHarvestLog(log: HarvestLogEntity): Long

    @Delete
    suspend fun deleteHarvestLog(log: HarvestLogEntity)
}

@Dao
interface SowingGuideDao {
    @Query("SELECT * FROM sowing_guides ORDER BY plantName COLLATE NOCASE, season")
    fun observeAll(): Flow<List<SowingGuideEntity>>

    @Query("SELECT COUNT(*) FROM sowing_guides")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(guides: List<SowingGuideEntity>)

    @Upsert
    suspend fun upsert(guide: SowingGuideEntity)

    @Delete
    suspend fun delete(guide: SowingGuideEntity)
}
