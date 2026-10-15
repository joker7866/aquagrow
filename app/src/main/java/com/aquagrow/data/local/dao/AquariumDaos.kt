package com.aquagrow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aquagrow.data.local.entity.AquariumEntity
import com.aquagrow.data.local.entity.AquariumSummaryRow
import com.aquagrow.data.local.entity.FeedingLogEntity
import com.aquagrow.data.local.entity.FishEntity
import com.aquagrow.data.local.entity.MaintenanceLogEntity
import com.aquagrow.data.local.entity.WaterQualityLogEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface AquariumDao {
    @Query("SELECT * FROM aquariums WHERE archived = 0 ORDER BY name COLLATE NOCASE, id")
    fun observeActive(): Flow<List<AquariumEntity>>

    /** Tank cards: each active tank with its most recent water test and total fish. */
    @Query(
        """
        SELECT a.*,
               w.id AS latest_id, w.aquariumId AS latest_aquariumId, w.testedAt AS latest_testedAt,
               w.temperatureC AS latest_temperatureC, w.ph AS latest_ph,
               w.ammoniaPpm AS latest_ammoniaPpm, w.nitritePpm AS latest_nitritePpm,
               w.nitratePpm AS latest_nitratePpm, w.ghDgh AS latest_ghDgh, w.khDkh AS latest_khDkh,
               w.note AS latest_note,
               (SELECT COALESCE(SUM(f.count), 0) FROM fish f WHERE f.aquariumId = a.id) AS fishCount
        FROM aquariums a
        LEFT JOIN water_quality_logs w ON w.id = (
            SELECT id FROM water_quality_logs
            WHERE aquariumId = a.id ORDER BY testedAt DESC LIMIT 1
        )
        WHERE a.archived = 0
        ORDER BY a.name COLLATE NOCASE, a.id
        """,
    )
    fun observeSummaries(): Flow<List<AquariumSummaryRow>>

    @Query("SELECT * FROM aquariums WHERE id = :id")
    fun observeById(id: Long): Flow<AquariumEntity?>

    @Query("SELECT * FROM aquariums WHERE id = :id")
    suspend fun getById(id: Long): AquariumEntity?

    @Query("SELECT COUNT(*) FROM aquariums WHERE archived = 0")
    fun observeActiveCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM aquariums
        WHERE name LIKE '%' || :q || '%' OR notes LIKE '%' || :q || '%' OR location LIKE '%' || :q || '%'
        ORDER BY archived, name COLLATE NOCASE LIMIT 50
        """,
    )
    suspend fun search(q: String): List<AquariumEntity>

    @Insert
    suspend fun insert(aquarium: AquariumEntity): Long

    @Update
    suspend fun update(aquarium: AquariumEntity)

    @Delete
    suspend fun delete(aquarium: AquariumEntity)
}

@Dao
interface FishDao {
    @Query("SELECT * FROM fish WHERE aquariumId = :aquariumId ORDER BY speciesName COLLATE NOCASE, id")
    fun observeForAquarium(aquariumId: Long): Flow<List<FishEntity>>

    @Query("SELECT * FROM fish WHERE aquariumId = :aquariumId")
    suspend fun getForAquarium(aquariumId: Long): List<FishEntity>

    /** Fish in active (non-archived) tanks. */
    @Query(
        """
        SELECT f.* FROM fish f JOIN aquariums a ON a.id = f.aquariumId
        WHERE a.archived = 0 ORDER BY f.speciesName COLLATE NOCASE, f.id
        """,
    )
    fun observeActive(): Flow<List<FishEntity>>

    @Query("SELECT * FROM fish WHERE id = :id")
    fun observeById(id: Long): Flow<FishEntity?>

    @Query("SELECT * FROM fish WHERE id = :id")
    suspend fun getById(id: Long): FishEntity?

    @Query(
        """
        SELECT COALESCE(SUM(f.count), 0) FROM fish f
        JOIN aquariums a ON a.id = f.aquariumId
        WHERE a.archived = 0
        """,
    )
    fun observeTotalCount(): Flow<Int>

    @Query(
        """
        SELECT * FROM fish
        WHERE speciesName LIKE '%' || :q || '%' OR nickname LIKE '%' || :q || '%'
           OR careNotes LIKE '%' || :q || '%' OR healthObservations LIKE '%' || :q || '%'
        ORDER BY speciesName COLLATE NOCASE LIMIT 50
        """,
    )
    suspend fun search(q: String): List<FishEntity>

    @Insert
    suspend fun insert(fish: FishEntity): Long

    @Update
    suspend fun update(fish: FishEntity)

    @Delete
    suspend fun delete(fish: FishEntity)
}

@Dao
interface AquariumLogDao {
    // Feeding
    @Query("SELECT * FROM feeding_logs WHERE aquariumId = :aquariumId ORDER BY fedAt DESC")
    fun observeFeedingLogs(aquariumId: Long): Flow<List<FeedingLogEntity>>

    @Query("SELECT * FROM feeding_logs ORDER BY fedAt DESC LIMIT :limit")
    fun observeRecentFeeding(limit: Int): Flow<List<FeedingLogEntity>>

    @Insert
    suspend fun insertFeedingLog(log: FeedingLogEntity): Long

    @Delete
    suspend fun deleteFeedingLog(log: FeedingLogEntity)

    @Query("DELETE FROM feeding_logs WHERE taskId = :taskId AND fedAt = :at")
    suspend fun deleteFeedingForTask(taskId: Long, at: Instant)

    // Water quality
    @Query("SELECT * FROM water_quality_logs WHERE aquariumId = :aquariumId ORDER BY testedAt DESC")
    fun observeWaterTests(aquariumId: Long): Flow<List<WaterQualityLogEntity>>

    @Query("SELECT * FROM water_quality_logs ORDER BY testedAt")
    fun observeAllWaterTests(): Flow<List<WaterQualityLogEntity>>

    @Insert
    suspend fun insertWaterTest(log: WaterQualityLogEntity): Long

    @Delete
    suspend fun deleteWaterTest(log: WaterQualityLogEntity)

    // Maintenance
    @Query("SELECT * FROM maintenance_logs WHERE aquariumId = :aquariumId ORDER BY performedAt DESC")
    fun observeMaintenanceLogs(aquariumId: Long): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_logs ORDER BY performedAt DESC")
    fun observeAllMaintenanceLogs(): Flow<List<MaintenanceLogEntity>>

    @Insert
    suspend fun insertMaintenanceLog(log: MaintenanceLogEntity): Long

    @Delete
    suspend fun deleteMaintenanceLog(log: MaintenanceLogEntity)

    @Query("DELETE FROM maintenance_logs WHERE taskId = :taskId AND performedAt = :at")
    suspend fun deleteMaintenanceForTask(taskId: Long, at: Instant)
}
