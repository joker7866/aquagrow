package com.aquagrow.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Plant(
    val id: Long = 0,
    val name: String,
    val variety: String? = null,
    val catalogKey: String? = null,
    val category: PlantCategory,
    val plantingMethod: PlantingMethod? = null,
    val sowingDate: LocalDate? = null,
    val plantingDate: LocalDate,
    val potSize: String? = null,
    val soilMix: String? = null,
    val location: String? = null,
    val sunlight: SunlightLevel? = null,
    val condition: PlantCondition? = null,
    val growthStages: List<String>,
    val currentStageIndex: Int = 0,
    val harvestDaysMin: Int? = null,
    val harvestDaysMax: Int? = null,
    val coverPhotoPath: String? = null,
    val notes: String? = null,
    val archived: Boolean = false,
) {
    val currentStage: String? get() = growthStages.getOrNull(currentStageIndex)

    /** Day 1 is the planting day itself. */
    fun dayNumber(today: LocalDate): Long = ChronoUnit.DAYS.between(plantingDate, today) + 1
}

data class GrowthLog(
    val id: Long = 0,
    val plantId: Long,
    val loggedAt: Instant,
    val stageIndex: Int? = null,
    val stageName: String? = null,
    val heightCm: Double? = null,
    val condition: PlantCondition? = null,
    val note: String? = null,
    val photoPaths: List<String> = emptyList(),
)

data class WateringLog(
    val id: Long = 0,
    val plantId: Long,
    val taskId: Long? = null,
    val loggedAt: Instant,
    val watered: Boolean = true,
    val soilMoisture: SoilMoisture? = null,
    val amountMl: Int? = null,
    val note: String? = null,
)

data class FertilizerLog(
    val id: Long = 0,
    val plantId: Long,
    val inventoryItemId: Long? = null,
    val taskId: Long? = null,
    val appliedAt: Instant,
    val type: FertilizerType,
    val productName: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val method: FertilizerMethod? = null,
    val note: String? = null,
)

data class HarvestLog(
    val id: Long = 0,
    val plantId: Long,
    val harvestedAt: Instant,
    val quantity: Double,
    val unit: String,
    val note: String? = null,
)

data class SowingGuide(
    val id: Long = 0,
    val catalogKey: String? = null,
    val plantName: String,
    val season: Season,
    val startMonth: Int,
    val endMonth: Int,
    val notes: String? = null,
    val userEdited: Boolean = false,
) {
    /** True if [month] (1..12) falls inside the window, handling windows that wrap the year end. */
    fun includesMonth(month: Int): Boolean = if (startMonth <= endMonth) month in startMonth..endMonth else month >= startMonth || month <= endMonth
}
