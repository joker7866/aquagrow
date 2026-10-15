package com.aquagrow.data.mapper

import com.aquagrow.data.local.entity.AquariumEntity
import com.aquagrow.data.local.entity.AquariumSummaryRow
import com.aquagrow.data.local.entity.ExpenseEntity
import com.aquagrow.data.local.entity.FeedingLogEntity
import com.aquagrow.data.local.entity.FertilizerLogEntity
import com.aquagrow.data.local.entity.FishEntity
import com.aquagrow.data.local.entity.HarvestLogEntity
import com.aquagrow.data.local.entity.InventoryItemEntity
import com.aquagrow.data.local.entity.MaintenanceLogEntity
import com.aquagrow.data.local.entity.NotificationSettingsEntity
import com.aquagrow.data.local.entity.PhotoRecordEntity
import com.aquagrow.data.local.entity.PlantEntity
import com.aquagrow.data.local.entity.PlantGrowthLogEntity
import com.aquagrow.data.local.entity.ReminderLogEntity
import com.aquagrow.data.local.entity.SowingGuideEntity
import com.aquagrow.data.local.entity.TaskOccurrenceStateEntity
import com.aquagrow.data.local.entity.TaskScheduleEntity
import com.aquagrow.data.local.entity.TaskWithSubject
import com.aquagrow.data.local.entity.WaterQualityLogEntity
import com.aquagrow.data.local.entity.WateringLogEntity
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.AquariumSummary
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.Expense
import com.aquagrow.domain.model.FeedingLog
import com.aquagrow.domain.model.FertilizerLog
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.GrowthLog
import com.aquagrow.domain.model.HarvestLog
import com.aquagrow.domain.model.InventoryItem
import com.aquagrow.domain.model.MaintenanceLog
import com.aquagrow.domain.model.NotificationSettings
import com.aquagrow.domain.model.OccurrenceState
import com.aquagrow.domain.model.Photo
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.ReminderLogEntry
import com.aquagrow.domain.model.SowingGuide
import com.aquagrow.domain.model.WaterRanges
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.model.WateringLog
import java.time.DayOfWeek
import java.time.Instant

// ---------- Garden ----------

fun PlantEntity.toDomain() = Plant(
    id = id, name = name, variety = variety, catalogKey = catalogKey, category = category,
    plantingMethod = plantingMethod, sowingDate = sowingDate, plantingDate = plantingDate,
    potSize = potSize, soilMix = soilMix, location = location, sunlight = sunlight,
    condition = condition, growthStages = growthStages, currentStageIndex = currentStageIndex,
    harvestDaysMin = harvestDaysMin, harvestDaysMax = harvestDaysMax,
    coverPhotoPath = coverPhotoPath, notes = notes, archived = archived,
)

fun Plant.toEntity(createdAt: Instant, updatedAt: Instant) = PlantEntity(
    id = id, name = name, variety = variety, catalogKey = catalogKey, category = category,
    plantingMethod = plantingMethod, sowingDate = sowingDate, plantingDate = plantingDate,
    potSize = potSize, soilMix = soilMix, location = location, sunlight = sunlight,
    condition = condition, growthStages = growthStages, currentStageIndex = currentStageIndex,
    harvestDaysMin = harvestDaysMin, harvestDaysMax = harvestDaysMax,
    coverPhotoPath = coverPhotoPath, notes = notes, archived = archived,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun PlantGrowthLogEntity.toDomain(photoPaths: List<String> = emptyList()) = GrowthLog(
    id = id, plantId = plantId, loggedAt = loggedAt, stageIndex = stageIndex, stageName = stageName,
    heightCm = heightCm, condition = condition, note = note, photoPaths = photoPaths,
)

fun GrowthLog.toEntity() = PlantGrowthLogEntity(
    id = id,
    plantId = plantId,
    loggedAt = loggedAt,
    stageIndex = stageIndex,
    stageName = stageName,
    heightCm = heightCm,
    condition = condition,
    note = note,
)

fun WateringLogEntity.toDomain() = WateringLog(id, plantId, taskId, loggedAt, watered, soilMoisture, amountMl, note)
fun WateringLog.toEntity() = WateringLogEntity(id, plantId, taskId, loggedAt, watered, soilMoisture, amountMl, note)

fun FertilizerLogEntity.toDomain() = FertilizerLog(
    id, plantId, inventoryItemId, taskId, appliedAt, type, productName, quantity, unit, method, note,
)
fun FertilizerLog.toEntity() = FertilizerLogEntity(
    id, plantId, inventoryItemId, taskId, appliedAt, type, productName, quantity, unit, method, note,
)

fun HarvestLogEntity.toDomain() = HarvestLog(id, plantId, harvestedAt, quantity, unit, note)
fun HarvestLog.toEntity() = HarvestLogEntity(id, plantId, harvestedAt, quantity, unit, note)

fun SowingGuideEntity.toDomain() = SowingGuide(id, catalogKey, plantName, season, startMonth, endMonth, notes, userEdited)
fun SowingGuide.toEntity() = SowingGuideEntity(id, catalogKey, plantName, season, startMonth, endMonth, notes, userEdited)

// ---------- Aquarium ----------

fun AquariumEntity.toDomain() = Aquarium(
    id = id, name = name, lengthCm = lengthCm, widthCm = widthCm, heightCm = heightCm,
    volumeLiters = volumeLiters, volumeIsManual = volumeIsManual, tankType = tankType,
    setupDate = setupDate, location = location, filterType = filterType, heater = heater,
    lighting = lighting, substrate = substrate, hasLivePlants = hasLivePlants,
    livePlantsNote = livePlantsNote, otherEquipment = otherEquipment,
    coverPhotoPath = coverPhotoPath, notes = notes, archived = archived,
    ranges = WaterRanges(
        tempMinC = tempMinC, tempMaxC = tempMaxC, phMin = phMin, phMax = phMax,
        ammoniaMaxPpm = ammoniaMaxPpm, nitriteMaxPpm = nitriteMaxPpm, nitrateMaxPpm = nitrateMaxPpm,
        ghMin = ghMin, ghMax = ghMax, khMin = khMin, khMax = khMax,
    ),
)

fun Aquarium.toEntity(createdAt: Instant, updatedAt: Instant) = AquariumEntity(
    id = id, name = name, lengthCm = lengthCm, widthCm = widthCm, heightCm = heightCm,
    volumeLiters = volumeLiters, volumeIsManual = volumeIsManual, tankType = tankType,
    setupDate = setupDate, location = location, filterType = filterType, heater = heater,
    lighting = lighting, substrate = substrate, hasLivePlants = hasLivePlants,
    livePlantsNote = livePlantsNote, otherEquipment = otherEquipment,
    coverPhotoPath = coverPhotoPath, notes = notes, archived = archived,
    tempMinC = ranges.tempMinC, tempMaxC = ranges.tempMaxC, phMin = ranges.phMin, phMax = ranges.phMax,
    ammoniaMaxPpm = ranges.ammoniaMaxPpm, nitriteMaxPpm = ranges.nitriteMaxPpm,
    nitrateMaxPpm = ranges.nitrateMaxPpm, ghMin = ranges.ghMin, ghMax = ranges.ghMax,
    khMin = ranges.khMin, khMax = ranges.khMax,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun FishEntity.toDomain() = Fish(
    id = id, aquariumId = aquariumId, speciesKey = speciesKey, speciesName = speciesName,
    nickname = nickname, count = count, addedDate = addedDate, ageText = ageText, sizeCm = sizeCm,
    photoPath = photoPath, feedingNeeds = feedingNeeds, compatibilityNotes = compatibilityNotes,
    healthObservations = healthObservations, careNotes = careNotes,
)

fun Fish.toEntity(createdAt: Instant, updatedAt: Instant) = FishEntity(
    id = id, aquariumId = aquariumId, speciesKey = speciesKey, speciesName = speciesName,
    nickname = nickname, count = count, addedDate = addedDate, ageText = ageText, sizeCm = sizeCm,
    photoPath = photoPath, feedingNeeds = feedingNeeds, compatibilityNotes = compatibilityNotes,
    healthObservations = healthObservations, careNotes = careNotes,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun FeedingLogEntity.toDomain() = FeedingLog(id, aquariumId, fishId, taskId, fedAt, foodName, amount, skipped, note)
fun FeedingLog.toEntity() = FeedingLogEntity(id, aquariumId, fishId, taskId, fedAt, foodName, amount, skipped, note)

fun WaterQualityLogEntity.toDomain() = WaterTest(
    id, aquariumId, testedAt, temperatureC, ph, ammoniaPpm, nitritePpm, nitratePpm, ghDgh, khDkh, note,
)
fun WaterTest.toEntity() = WaterQualityLogEntity(
    id, aquariumId, testedAt, temperatureC, ph, ammoniaPpm, nitritePpm, nitratePpm, ghDgh, khDkh, note,
)

fun MaintenanceLogEntity.toDomain() = MaintenanceLog(id, aquariumId, taskId, performedAt, type, waterChangePercent, note)
fun MaintenanceLog.toEntity() = MaintenanceLogEntity(id, aquariumId, taskId, performedAt, type, waterChangePercent, note)

fun AquariumSummaryRow.toDomain() = AquariumSummary(aquarium.toDomain(), latestTest?.toDomain(), fishCount)

// ---------- Tasks ----------

fun Set<DayOfWeek>.toBitmask(): Int = fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
fun Int.toDaysOfWeek(): Set<DayOfWeek> = DayOfWeek.entries.filter { this and (1 shl (it.value - 1)) != 0 }.toSet()

fun TaskWithSubject.toDomain(): CareTask = task.toDomain(subjectName = fishName ?: plantName ?: aquariumName)

fun TaskScheduleEntity.toDomain(subjectName: String? = null) = CareTask(
    id = id, title = title, module = module, type = type, plantId = plantId, aquariumId = aquariumId,
    fishId = fishId, subjectName = subjectName, maintenanceType = maintenanceType,
    fertilizerType = fertilizerType, itemName = itemName, amountText = amountText,
    startDate = startDate, endDate = endDate, reminderTime = reminderTime,
    recurrence = RecurrenceRule(
        type = recurrenceType,
        interval = recurrenceInterval.coerceAtLeast(1),
        daysOfWeek = recurrenceDaysOfWeek.toDaysOfWeek(),
        dayOfMonth = recurrenceDayOfMonth,
    ),
    enabled = enabled, notes = notes,
)

fun CareTask.toEntity(createdAt: Instant, updatedAt: Instant) = TaskScheduleEntity(
    id = id, title = title, module = module, type = type, plantId = plantId, aquariumId = aquariumId,
    fishId = fishId, maintenanceType = maintenanceType, fertilizerType = fertilizerType,
    itemName = itemName, amountText = amountText, startDate = startDate, endDate = endDate,
    reminderTime = reminderTime, recurrenceType = recurrence.type,
    recurrenceInterval = recurrence.interval, recurrenceDaysOfWeek = recurrence.daysOfWeek.toBitmask(),
    recurrenceDayOfMonth = recurrence.dayOfMonth, enabled = enabled, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun TaskOccurrenceStateEntity.toDomain() = OccurrenceState(
    taskId = taskId,
    originalDate = occurrenceDate,
    status = status,
    rescheduledDate = rescheduledDate,
    snoozedUntil = snoozedUntil,
    handledAt = handledAt,
)

fun OccurrenceState.toEntity(id: Long = 0) = TaskOccurrenceStateEntity(
    id = id,
    taskId = taskId,
    occurrenceDate = originalDate,
    status = status,
    rescheduledDate = rescheduledDate,
    snoozedUntil = snoozedUntil,
    handledAt = handledAt,
)

fun ReminderLogEntity.toDomain() = ReminderLogEntry(id, kind, taskId, occurrenceDate, title, text, postedAt)

// ---------- Shared ----------

fun InventoryItemEntity.toDomain() = InventoryItem(
    id, module, name, category, quantity, unit, lowStockThreshold, purchaseDate, costMinor,
    currencyCode, expiryDate, aquariumId, notes,
)

fun InventoryItem.toEntity(createdAt: Instant, updatedAt: Instant) = InventoryItemEntity(
    id, module, name, category, quantity, unit, lowStockThreshold, purchaseDate, costMinor,
    currencyCode, expiryDate, aquariumId, notes, createdAt, updatedAt,
)

fun ExpenseEntity.toDomain() = Expense(
    id, title, module, category, amountMinor, currencyCode, spentOn, inventoryItemId, plantId, aquariumId, note,
)

fun Expense.toEntity(createdAt: Instant) = ExpenseEntity(
    id, title, module, category, amountMinor, currencyCode, spentOn, inventoryItemId, plantId, aquariumId,
    note, createdAt,
)

fun PhotoRecordEntity.toDomain() = Photo(id, filePath, takenAt, caption, plantId, aquariumId, fishId, growthLogId)
fun Photo.toEntity() = PhotoRecordEntity(id, filePath, takenAt, caption, plantId, aquariumId, fishId, growthLogId)

fun NotificationSettingsEntity.toDomain() = NotificationSettings(
    remindersEnabled,
    defaultReminderTime,
    quietHoursEnabled,
    quietHoursStart,
    quietHoursEnd,
    disabledChannels.toSet(),
    missedRemindersEnabled,
    inventoryRemindersEnabled,
)

fun NotificationSettings.toEntity() = NotificationSettingsEntity(
    remindersEnabled = remindersEnabled,
    defaultReminderTime = defaultReminderTime,
    quietHoursEnabled = quietHoursEnabled,
    quietHoursStart = quietHoursStart,
    quietHoursEnd = quietHoursEnd,
    disabledChannels = disabledChannels.sorted(),
    missedRemindersEnabled = missedRemindersEnabled,
    inventoryRemindersEnabled = inventoryRemindersEnabled,
)
