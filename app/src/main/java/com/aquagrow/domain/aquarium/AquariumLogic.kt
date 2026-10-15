package com.aquagrow.domain.aquarium

import com.aquagrow.domain.catalog.CatalogFish
import com.aquagrow.domain.catalog.FishCatalog
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.Temperament
import com.aquagrow.domain.model.WaterParameter
import com.aquagrow.domain.model.WaterRanges
import com.aquagrow.domain.model.WaterTest
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

object TankVolume {
    /** Litres from internal dimensions in cm (1 L = 1000 cm³), rounded to 1 decimal. */
    fun liters(lengthCm: Double?, widthCm: Double?, heightCm: Double?): Double? {
        if (lengthCm == null || widthCm == null || heightCm == null) return null
        if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) return null
        return (lengthCm * widthCm * heightCm / 100.0).roundToInt() / 10.0
    }
}

enum class RangeStatus { IN_RANGE, BELOW, ABOVE, NOT_SET, NO_READING }

data class ParameterCheck(val parameter: WaterParameter, val value: Double?, val status: RangeStatus)

/**
 * Compares readings to the user's own ranges. Results are phrased in the UI as
 * "outside your set range" and never interpreted as fish health.
 */
object WaterRangeChecker {
    fun check(p: WaterParameter, value: Double?, ranges: WaterRanges): RangeStatus {
        if (value == null) return RangeStatus.NO_READING
        val (min, max) = ranges.bounds(p)
        if (min == null && max == null) return RangeStatus.NOT_SET
        return when {
            min != null && value < min -> RangeStatus.BELOW
            max != null && value > max -> RangeStatus.ABOVE
            else -> RangeStatus.IN_RANGE
        }
    }

    fun checkAll(test: WaterTest, ranges: WaterRanges): List<ParameterCheck> = WaterParameter.entries.map { ParameterCheck(it, test.value(it), check(it, test.value(it), ranges)) }

    fun outOfRange(test: WaterTest?, ranges: WaterRanges): List<ParameterCheck> = test?.let { checkAll(it, ranges).filter { c -> c.status == RangeStatus.BELOW || c.status == RangeStatus.ABOVE } }.orEmpty()

    /**
     * Temperature and pH ranges that suit every catalog species in the tank (the overlap).
     * Null bounds when there are no catalog species or their ranges do not overlap.
     */
    fun defaultsFromSpecies(fish: List<Fish>, base: WaterRanges = WaterRanges()): WaterRanges {
        val species = fish.mapNotNull { FishCatalog.find(it.speciesKey) }.distinctBy { it.key }
        if (species.isEmpty()) return base
        val tMin = species.maxOf { it.tempMinC }
        val tMax = species.minOf { it.tempMaxC }
        val pMin = species.maxOf { it.phMin }
        val pMax = species.minOf { it.phMax }
        return base.copy(
            tempMinC = tMin.takeIf { tMin <= tMax },
            tempMaxC = tMax.takeIf { tMin <= tMax },
            phMin = pMin.takeIf { pMin <= pMax },
            phMax = pMax.takeIf { pMin <= pMax },
        )
    }

    /**
     * Widely used general reference points for ammonia, nitrite and nitrate (ppm),
     * offered as a starting point the user can edit, not a rule.
     */
    val GENERAL_REFERENCE = WaterRanges(ammoniaMaxPpm = 0.25, nitriteMaxPpm = 0.25, nitrateMaxPpm = 40.0)
}

enum class CompatibilityIssue {
    TEMPERATURE_MISMATCH,
    PH_MISMATCH,
    TANK_TOO_SMALL,
    TEMPERAMENT,
    FIN_NIPPING,
    SAME_SPECIES_MALES,
    LABYRINTH_RIVALS,
    COLD_VS_TROPICAL,
    MAY_EAT_SMALL_FISH,
    GROUP_TOO_SMALL,
}

/** A soft hint: "may conflict", always shown as a note the user can ignore. */
data class CompatibilityWarning(val issue: CompatibilityIssue, val otherSpecies: String? = null)

object CompatibilityChecker {
    /**
     * Hints for adding [newSpecies] (count [newCount]) to a tank of [tankLiters] that already
     * holds [existing]. Unknown (custom) species get no hints because there is no data.
     */
    fun check(
        newSpecies: CatalogFish,
        newCount: Int,
        existing: List<Fish>,
        tankLiters: Double?,
        ranges: WaterRanges = WaterRanges(),
    ): List<CompatibilityWarning> {
        val out = mutableListOf<CompatibilityWarning>()
        val others = existing.mapNotNull { f -> FishCatalog.find(f.speciesKey)?.let { it to f } }

        if (tankLiters != null && tankLiters < newSpecies.minTankLiters) out += CompatibilityWarning(CompatibilityIssue.TANK_TOO_SMALL)

        val sameSpeciesCount = others.filter { it.first.key == newSpecies.key }.sumOf { it.second.count } + newCount
        if (newSpecies.minGroup > 1 && sameSpeciesCount < newSpecies.minGroup) out += CompatibilityWarning(CompatibilityIssue.GROUP_TOO_SMALL)
        if (newSpecies.malesTerritorial && sameSpeciesCount > 1) out += CompatibilityWarning(CompatibilityIssue.SAME_SPECIES_MALES)

        // Against the tank's own set ranges.
        val (tMin, tMax) = ranges.bounds(WaterParameter.TEMPERATURE)
        if ((tMin != null && newSpecies.tempMaxC < tMin) || (tMax != null && newSpecies.tempMinC > tMax)) {
            out += CompatibilityWarning(CompatibilityIssue.TEMPERATURE_MISMATCH)
        }
        val (pMin, pMax) = ranges.bounds(WaterParameter.PH)
        if ((pMin != null && newSpecies.phMax < pMin) || (pMax != null && newSpecies.phMin > pMax)) {
            out += CompatibilityWarning(CompatibilityIssue.PH_MISMATCH)
        }

        for ((other, _) in others.distinctBy { it.first.key }) {
            if (other.key == newSpecies.key) continue
            val name = other.name
            if (newSpecies.tempMinC > other.tempMaxC || other.tempMinC > newSpecies.tempMaxC) out += CompatibilityWarning(CompatibilityIssue.TEMPERATURE_MISMATCH, name)
            if (newSpecies.phMin > other.phMax || other.phMin > newSpecies.phMax) out += CompatibilityWarning(CompatibilityIssue.PH_MISMATCH, name)
            if (newSpecies.coldWater != other.coldWater) out += CompatibilityWarning(CompatibilityIssue.COLD_VS_TROPICAL, name)
            if ((newSpecies.finNipper && other.longFinned) || (other.finNipper && newSpecies.longFinned)) out += CompatibilityWarning(CompatibilityIssue.FIN_NIPPING, name)
            if (newSpecies.labyrinth && other.labyrinth && (newSpecies.malesTerritorial || other.malesTerritorial)) out += CompatibilityWarning(CompatibilityIssue.LABYRINTH_RIVALS, name)
            val predatorPrey = (newSpecies.mayEatSmallFish && other.adultSizeCm <= 4.0) || (other.mayEatSmallFish && newSpecies.adultSizeCm <= 4.0)
            if (predatorPrey) out += CompatibilityWarning(CompatibilityIssue.MAY_EAT_SMALL_FISH, name)
            val temperamentGap = (newSpecies.temperament != Temperament.PEACEFUL && other.temperament == Temperament.PEACEFUL && other.adultSizeCm < newSpecies.adultSizeCm) ||
                (other.temperament != Temperament.PEACEFUL && newSpecies.temperament == Temperament.PEACEFUL && newSpecies.adultSizeCm < other.adultSizeCm)
            if (temperamentGap && !predatorPrey) out += CompatibilityWarning(CompatibilityIssue.TEMPERAMENT, name)
        }
        return out.distinct()
    }
}

/** One feeding plan: who it is for, what, and when. */
data class FeedingPlan(
    /** Null = the whole tank; otherwise one fish group (species-specific schedule). */
    val fishId: Long?,
    val fishName: String?,
    val foodName: String?,
    /** User-written portion guidance; the app never computes portions. */
    val portionText: String?,
    val times: List<LocalTime>,
    val rule: RecurrenceRule,
    val startDate: LocalDate,
)

/**
 * Turns feeding plans into task rows: one task per (plan, time). Duplicate times within a
 * plan are collapsed, and each plan keeps its own food and portion, so different species
 * never share a single schedule unless the user chose "whole tank".
 */
object FeedingScheduleGenerator {
    fun generate(aquariumId: Long, plans: List<FeedingPlan>): List<CareTask> = plans.flatMap { plan ->
        plan.times.distinct().sorted().map { time ->
            CareTask(
                title = "Feed",
                module = CareModule.AQUARIUM,
                type = TaskType.FEEDING,
                aquariumId = aquariumId,
                fishId = plan.fishId,
                itemName = plan.foodName?.trim()?.ifEmpty { null },
                amountText = plan.portionText?.trim()?.ifEmpty { null },
                startDate = plan.startDate,
                reminderTime = time,
                recurrence = plan.rule,
            )
        }
    }
}
