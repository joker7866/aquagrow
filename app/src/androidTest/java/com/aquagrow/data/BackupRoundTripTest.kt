package com.aquagrow.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquagrow.data.backup.BackupData
import com.aquagrow.data.backup.BackupFile
import com.aquagrow.data.backup.BackupPreferences
import com.aquagrow.data.backup.BackupValidator
import com.aquagrow.data.local.db.AquaGrowDatabase
import com.aquagrow.data.local.entity.AquariumEntity
import com.aquagrow.data.local.entity.FishEntity
import com.aquagrow.data.local.entity.PlantEntity
import com.aquagrow.data.local.entity.WateringLogEntity
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.TankType
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Real SQLite: rows read from one database, sent through JSON, inserted into a fresh one. */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val source = Room.inMemoryDatabaseBuilder(context, AquaGrowDatabase::class.java).build()
    private val target = Room.inMemoryDatabaseBuilder(context, AquaGrowDatabase::class.java).build()

    @After fun close() {
        source.close()
        target.close()
    }

    @Test
    fun exportThenRestoreGivesIdenticalRows() = runTest {
        val now = Instant.parse("2026-10-08T03:30:00Z")
        val day = LocalDate.of(2026, 9, 1)
        val s = source.backupDao()
        s.insertPlants(
            listOf(
                PlantEntity(
                    name = "Basil", variety = null, catalogKey = "basil", category = PlantCategory.HERB, plantingMethod = null, sowingDate = null,
                    plantingDate = day, potSize = null, soilMix = null, location = null, sunlight = null, condition = null, growthStages = listOf("A", "B"),
                    harvestDaysMin = null, harvestDaysMax = null, coverPhotoPath = null, notes = "₹ note", createdAt = now, updatedAt = now,
                ),
            ),
        )
        val plantId = s.plants().single().id
        s.insertWateringLogs(listOf(WateringLogEntity(plantId = plantId, taskId = null, loggedAt = now, watered = true, soilMoisture = null, amountMl = null, note = null)))
        s.insertAquariums(
            listOf(
                AquariumEntity(
                    name = "Tank", lengthCm = null, widthCm = null, heightCm = null, volumeLiters = 40.0, tankType = TankType.COMMUNITY, setupDate = null,
                    location = null, filterType = null, heater = null, lighting = null, substrate = null, livePlantsNote = null, otherEquipment = null,
                    coverPhotoPath = null, notes = null, tempMinC = null, tempMaxC = null, phMin = null, phMax = null, ammoniaMaxPpm = null,
                    nitriteMaxPpm = null, nitrateMaxPpm = null, ghMin = null, ghMax = null, khMin = null, khMax = null, createdAt = now, updatedAt = now,
                ),
            ),
        )
        val tankId = s.aquariums().single().id
        s.insertFish(
            listOf(
                FishEntity(
                    aquariumId = tankId, speciesKey = null, speciesName = "Guppy", nickname = null, count = 5, addedDate = day, ageText = null,
                    sizeCm = null, photoPath = null, feedingNeeds = null, compatibilityNotes = null, healthObservations = null, careNotes = null,
                    createdAt = now, updatedAt = now,
                ),
            ),
        )

        val file = BackupFile(
            schemaVersion = 1,
            appVersion = "test",
            exportedAt = now,
            preferences = BackupPreferences(),
            data = BackupData(plants = s.plants(), wateringLogs = s.wateringLogs(), aquariums = s.aquariums(), fish = s.fish()),
        )
        val decoded = BackupFile.json.decodeFromString(BackupFile.serializer(), BackupFile.json.encodeToString(BackupFile.serializer(), file))
        assertEquals(emptyList<String>(), BackupValidator.validate(decoded, 1))

        val t = target.backupDao()
        t.insertPlants(decoded.data.plants)
        t.insertAquariums(decoded.data.aquariums)
        t.insertFish(decoded.data.fish)
        t.insertWateringLogs(decoded.data.wateringLogs)

        assertEquals(s.plants(), t.plants())
        assertEquals(s.aquariums(), t.aquariums())
        assertEquals(s.fish(), t.fish())
        assertEquals(s.wateringLogs(), t.wateringLogs())
    }
}
