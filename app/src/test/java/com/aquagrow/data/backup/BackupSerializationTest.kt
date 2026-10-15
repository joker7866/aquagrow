package com.aquagrow.data.backup

import com.aquagrow.data.local.entity.AquariumEntity
import com.aquagrow.data.local.entity.ExpenseEntity
import com.aquagrow.data.local.entity.FishEntity
import com.aquagrow.data.local.entity.PlantEntity
import com.aquagrow.data.local.entity.TaskScheduleEntity
import com.aquagrow.data.local.entity.WateringLogEntity
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.ExpenseCategory
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.RecurrenceType
import com.aquagrow.domain.model.TankType
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.ThemeMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {
    private val now = Instant.parse("2026-10-08T03:30:00Z")
    private val day = LocalDate.of(2026, 9, 1)

    private fun sample(): BackupFile {
        val plant = PlantEntity(
            id = 3, name = "Tomato", variety = "Cherry", catalogKey = "tomato", category = PlantCategory.VEGETABLE, plantingMethod = null,
            sowingDate = null, plantingDate = day, potSize = "30 cm", soilMix = null, location = "Terrace", sunlight = null, condition = null,
            growthStages = listOf("Seedling", "Flowering"), harvestDaysMin = 60, harvestDaysMax = 80, coverPhotoPath = "photos/a.jpg",
            notes = "Line1\n\"quoted\" ₹", createdAt = now, updatedAt = now,
        )
        val tank = AquariumEntity(
            id = 7, name = "Living room", lengthCm = 60.0, widthCm = 30.0, heightCm = 40.0, volumeLiters = 72.0, tankType = TankType.PLANTED,
            setupDate = day, location = null, filterType = null, heater = null, lighting = null, substrate = null, livePlantsNote = null,
            otherEquipment = null, coverPhotoPath = null, notes = null, tempMinC = 24.0, tempMaxC = 27.0, phMin = 6.5, phMax = 7.5,
            ammoniaMaxPpm = 0.0, nitriteMaxPpm = 0.0, nitrateMaxPpm = 40.0, ghMin = null, ghMax = null, khMin = null, khMax = null,
            createdAt = now, updatedAt = now,
        )
        val fish = FishEntity(
            id = 11, aquariumId = 7, speciesKey = "neon_tetra", speciesName = "Neon tetra", nickname = null, count = 8, addedDate = day,
            ageText = null, sizeCm = null, photoPath = null, feedingNeeds = null, compatibilityNotes = null, healthObservations = null,
            careNotes = null, createdAt = now, updatedAt = now,
        )
        val task = TaskScheduleEntity(
            id = 20, title = "Water", module = CareModule.GARDEN, type = TaskType.WATERING, plantId = 3, aquariumId = null, fishId = null,
            maintenanceType = null, fertilizerType = null, itemName = null, amountText = null, startDate = day, endDate = null,
            reminderTime = LocalTime.of(7, 30), recurrenceType = RecurrenceType.DAILY, recurrenceInterval = 2, recurrenceDaysOfWeek = 0,
            recurrenceDayOfMonth = null, notes = null, createdAt = now, updatedAt = now,
        )
        val log = WateringLogEntity(id = 30, plantId = 3, taskId = 20, loggedAt = now, watered = true, soilMoisture = null, amountMl = 500, note = null)
        val expense = ExpenseEntity(
            id = 40, title = "Soil", module = CareModule.GARDEN, category = ExpenseCategory.SOIL, amountMinor = 199_00, currencyCode = "INR",
            spentOn = day, inventoryItemId = null, plantId = 3, aquariumId = null, note = null, createdAt = now,
        )
        return BackupFile(
            schemaVersion = 1,
            appVersion = "1.0.0",
            exportedAt = now,
            preferences = BackupPreferences(gardenEnabled = true, aquariumEnabled = false, themeMode = ThemeMode.DARK, currencyCode = "USD", displayName = "Asha"),
            data = BackupData(plants = listOf(plant), aquariums = listOf(tank), fish = listOf(fish), tasks = listOf(task), wateringLogs = listOf(log), expenses = listOf(expense)),
        )
    }

    @Test fun `json round trip keeps every value`() {
        val original = sample()
        val text = BackupFile.json.encodeToString(BackupFile.serializer(), original)
        val decoded = BackupFile.json.decodeFromString(BackupFile.serializer(), text)
        assertEquals(original, decoded)
        assertTrue("schema version is written", text.contains("\"schemaVersion\": 1"))
    }

    @Test fun `unknown fields from a newer app are ignored`() {
        val text = BackupFile.json.encodeToString(BackupFile.serializer(), sample()).replaceFirst("{", "{\n  \"futureField\": 42,")
        assertEquals(sample(), BackupFile.json.decodeFromString(BackupFile.serializer(), text))
    }

    @Test fun `valid backup passes validation`() {
        assertEquals(emptyList<String>(), BackupValidator.validate(sample(), currentSchema = 1))
    }

    @Test fun `newer schema is rejected`() {
        val errors = BackupValidator.validate(sample().copy(schemaVersion = 2), currentSchema = 1)
        assertTrue(errors.single().contains("newer"))
    }

    @Test fun `wrong format is rejected`() {
        assertTrue(BackupValidator.validate(sample().copy(format = "something-else"), 1).isNotEmpty())
    }

    @Test fun `dangling references are rejected`() {
        val s = sample()
        val broken = s.copy(data = s.data.copy(fish = s.data.fish.map { it.copy(aquariumId = 999) }))
        assertTrue(BackupValidator.validate(broken, 1).any { "999" in it })
    }

    @Test fun `duplicate ids are rejected`() {
        val s = sample()
        val broken = s.copy(data = s.data.copy(plants = s.data.plants + s.data.plants))
        assertTrue(BackupValidator.validate(broken, 1).any { "Duplicate" in it })
    }

    @Test fun `counts include every log type`() {
        assertEquals(1, sample().data.counts["logs"])
        assertEquals(1, sample().data.counts["plants"])
    }
}
