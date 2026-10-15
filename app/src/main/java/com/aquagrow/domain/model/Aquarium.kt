package com.aquagrow.domain.model

import java.time.Instant
import java.time.LocalDate

/** The user's own reference ranges for one tank. Null bounds are not tracked. */
data class WaterRanges(
    val tempMinC: Double? = null,
    val tempMaxC: Double? = null,
    val phMin: Double? = null,
    val phMax: Double? = null,
    val ammoniaMaxPpm: Double? = null,
    val nitriteMaxPpm: Double? = null,
    val nitrateMaxPpm: Double? = null,
    val ghMin: Double? = null,
    val ghMax: Double? = null,
    val khMin: Double? = null,
    val khMax: Double? = null,
) {
    fun bounds(p: WaterParameter): Pair<Double?, Double?> = when (p) {
        WaterParameter.TEMPERATURE -> tempMinC to tempMaxC
        WaterParameter.PH -> phMin to phMax
        WaterParameter.AMMONIA -> null to ammoniaMaxPpm
        WaterParameter.NITRITE -> null to nitriteMaxPpm
        WaterParameter.NITRATE -> null to nitrateMaxPpm
        WaterParameter.GH -> ghMin to ghMax
        WaterParameter.KH -> khMin to khMax
    }
}

data class Aquarium(
    val id: Long = 0,
    val name: String,
    val lengthCm: Double? = null,
    val widthCm: Double? = null,
    val heightCm: Double? = null,
    val volumeLiters: Double? = null,
    val volumeIsManual: Boolean = false,
    val tankType: TankType = TankType.COMMUNITY,
    val setupDate: LocalDate? = null,
    val location: String? = null,
    val filterType: String? = null,
    val heater: String? = null,
    val lighting: String? = null,
    val substrate: String? = null,
    val hasLivePlants: Boolean = false,
    val livePlantsNote: String? = null,
    val otherEquipment: String? = null,
    val coverPhotoPath: String? = null,
    val notes: String? = null,
    val ranges: WaterRanges = WaterRanges(),
    val archived: Boolean = false,
)

data class Fish(
    val id: Long = 0,
    val aquariumId: Long,
    val speciesKey: String? = null,
    val speciesName: String,
    val nickname: String? = null,
    val count: Int = 1,
    val addedDate: LocalDate? = null,
    val ageText: String? = null,
    val sizeCm: Double? = null,
    val photoPath: String? = null,
    val feedingNeeds: String? = null,
    val compatibilityNotes: String? = null,
    val healthObservations: String? = null,
    val careNotes: String? = null,
) {
    val displayName: String get() = nickname?.takeIf { it.isNotBlank() } ?: speciesName
}

data class FeedingLog(
    val id: Long = 0,
    val aquariumId: Long,
    val fishId: Long? = null,
    val taskId: Long? = null,
    val fedAt: Instant,
    val foodName: String? = null,
    val amount: String? = null,
    val skipped: Boolean = false,
    val note: String? = null,
)

data class WaterTest(
    val id: Long = 0,
    val aquariumId: Long,
    val testedAt: Instant,
    val temperatureC: Double? = null,
    val ph: Double? = null,
    val ammoniaPpm: Double? = null,
    val nitritePpm: Double? = null,
    val nitratePpm: Double? = null,
    val ghDgh: Double? = null,
    val khDkh: Double? = null,
    val note: String? = null,
) {
    fun value(p: WaterParameter): Double? = when (p) {
        WaterParameter.TEMPERATURE -> temperatureC
        WaterParameter.PH -> ph
        WaterParameter.AMMONIA -> ammoniaPpm
        WaterParameter.NITRITE -> nitritePpm
        WaterParameter.NITRATE -> nitratePpm
        WaterParameter.GH -> ghDgh
        WaterParameter.KH -> khDkh
    }
}

data class MaintenanceLog(
    val id: Long = 0,
    val aquariumId: Long,
    val taskId: Long? = null,
    val performedAt: Instant,
    val type: MaintenanceType,
    val waterChangePercent: Int? = null,
    val note: String? = null,
)

data class AquariumSummary(
    val aquarium: Aquarium,
    val latestTest: WaterTest?,
    val fishCount: Int,
)
