package com.aquagrow.domain.aquarium

import com.aquagrow.domain.catalog.FishCatalog
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterParameter
import com.aquagrow.domain.model.WaterRanges
import com.aquagrow.domain.model.WaterTest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TankVolumeTest {
    @Test fun standardTwoFootTank() = assertEquals(91.1, TankVolume.liters(60.0, 30.0, 50.6)!!, 0.001)

    @Test fun roundsToOneDecimal() = assertEquals(60.0, TankVolume.liters(50.0, 30.0, 40.0)!!, 0.0)

    @Test fun missingOrInvalidDimension() {
        assertNull(TankVolume.liters(60.0, null, 40.0))
        assertNull(TankVolume.liters(60.0, 0.0, 40.0))
        assertNull(TankVolume.liters(-1.0, 30.0, 40.0))
    }
}

class WaterRangeCheckerTest {
    private val ranges = WaterRanges(tempMinC = 24.0, tempMaxC = 28.0, phMin = 6.5, phMax = 7.5, nitrateMaxPpm = 40.0)

    @Test fun inBelowAboveAndBoundaries() {
        assertEquals(RangeStatus.IN_RANGE, WaterRangeChecker.check(WaterParameter.TEMPERATURE, 26.0, ranges))
        assertEquals(RangeStatus.IN_RANGE, WaterRangeChecker.check(WaterParameter.TEMPERATURE, 24.0, ranges))
        assertEquals(RangeStatus.IN_RANGE, WaterRangeChecker.check(WaterParameter.TEMPERATURE, 28.0, ranges))
        assertEquals(RangeStatus.BELOW, WaterRangeChecker.check(WaterParameter.PH, 6.4, ranges))
        assertEquals(RangeStatus.ABOVE, WaterRangeChecker.check(WaterParameter.NITRATE, 40.1, ranges))
    }

    @Test fun maxOnlyParameterHasNoLowerBound() = assertEquals(RangeStatus.IN_RANGE, WaterRangeChecker.check(WaterParameter.NITRATE, 0.0, ranges))

    @Test fun notSetAndNoReading() {
        assertEquals(RangeStatus.NOT_SET, WaterRangeChecker.check(WaterParameter.AMMONIA, 0.5, ranges))
        assertEquals(RangeStatus.NO_READING, WaterRangeChecker.check(WaterParameter.PH, null, ranges))
    }

    @Test fun outOfRangeListsOnlyOffenders() {
        val test = WaterTest(aquariumId = 1, testedAt = Instant.EPOCH, temperatureC = 30.0, ph = 7.0, nitratePpm = 60.0, ammoniaPpm = 1.0)
        val out = WaterRangeChecker.outOfRange(test, ranges).map { it.parameter }
        assertEquals(setOf(WaterParameter.TEMPERATURE, WaterParameter.NITRATE), out.toSet())
    }

    @Test fun defaultsFromSpecies_isTheOverlap() {
        val fish = listOf(
            Fish(aquariumId = 1, speciesKey = "guppy", speciesName = "Guppy"), // 22-28, pH 7.0-8.0
            Fish(aquariumId = 1, speciesKey = "platy", speciesName = "Platy"), // 20-26, pH 7.0-8.2
        )
        val r = WaterRangeChecker.defaultsFromSpecies(fish)
        assertEquals(22.0, r.tempMinC!!, 0.0)
        assertEquals(26.0, r.tempMaxC!!, 0.0)
        assertEquals(7.0, r.phMin!!, 0.0)
        assertEquals(8.0, r.phMax!!, 0.0)
    }

    @Test fun defaultsFromSpecies_noOverlapGivesNull() {
        val fish = listOf(
            Fish(aquariumId = 1, speciesKey = "goldfish", speciesName = "Goldfish"), // 18-23
            Fish(aquariumId = 1, speciesKey = "angelfish", speciesName = "Angelfish"), // 24-29
        )
        val r = WaterRangeChecker.defaultsFromSpecies(fish)
        assertNull(r.tempMinC)
        assertNull(r.tempMaxC)
    }
}

class FeedingScheduleGeneratorTest {
    private val start = LocalDate.of(2026, 10, 8)

    @Test fun onePerTime_dedupedAndSorted() {
        val tasks = FeedingScheduleGenerator.generate(
            7,
            listOf(FeedingPlan(fishId = 3, fishName = "Guppy", foodName = " Flakes ", portionText = "", times = listOf(LocalTime.of(18, 0), LocalTime.of(8, 0), LocalTime.of(8, 0)), rule = RecurrenceRule.everyDays(1), startDate = start)),
        )
        assertEquals(listOf(LocalTime.of(8, 0), LocalTime.of(18, 0)), tasks.map { it.reminderTime })
        assertTrue(tasks.all { it.type == TaskType.FEEDING && it.aquariumId == 7L && it.fishId == 3L })
        assertEquals("Flakes", tasks.first().itemName)
        assertNull(tasks.first().amountText) // blank portion text is not stored
    }

    @Test fun separatePlansStaySeparatePerSpecies() {
        val tasks = FeedingScheduleGenerator.generate(
            1,
            listOf(
                FeedingPlan(1, "Betta", "Betta pellets", null, listOf(LocalTime.of(9, 0)), RecurrenceRule.everyDays(1), start),
                FeedingPlan(2, "Corydoras", "Sinking wafers", null, listOf(LocalTime.of(21, 0)), RecurrenceRule.everyDays(2), start),
            ),
        )
        assertEquals(2, tasks.size)
        assertEquals(setOf(1L, 2L), tasks.map { it.fishId }.toSet())
        assertEquals(2, tasks.first { it.fishId == 2L }.recurrence.interval)
    }
}

class CompatibilityCheckerTest {
    private fun fish(key: String, count: Int = 1) = Fish(aquariumId = 1, speciesKey = key, speciesName = key, count = count)
    private fun issues(newKey: String, count: Int, existing: List<Fish>, liters: Double? = 100.0) = CompatibilityChecker.check(FishCatalog.find(newKey)!!, count, existing, liters).map { it.issue }.toSet()

    @Test fun peacefulCommunity_noWarnings() = assertTrue(issues("platy", 3, listOf(fish("guppy", 3)), 80.0).isEmpty())

    @Test fun goldfishWithTropical() = assertTrue(CompatibilityIssue.COLD_VS_TROPICAL in issues("goldfish", 2, listOf(fish("neon_tetra", 8)), 150.0))

    @Test fun secondBettaFlagsTerritorialMales() = assertTrue(CompatibilityIssue.SAME_SPECIES_MALES in issues("betta", 1, listOf(fish("betta", 1)), 40.0))

    @Test fun danioWithBettaFlagsFinNipping() = assertTrue(CompatibilityIssue.FIN_NIPPING in issues("zebra_danio", 6, listOf(fish("betta")), 80.0))

    @Test fun angelfishWithNeonsMayEatSmallFish() = assertTrue(CompatibilityIssue.MAY_EAT_SMALL_FISH in issues("angelfish", 1, listOf(fish("neon_tetra", 10)), 150.0))

    @Test fun smallTankAndSmallGroup() {
        val i = issues("neon_tetra", 2, emptyList(), 20.0)
        assertTrue(CompatibilityIssue.TANK_TOO_SMALL in i)
        assertTrue(CompatibilityIssue.GROUP_TOO_SMALL in i)
        assertFalse(CompatibilityIssue.GROUP_TOO_SMALL in issues("neon_tetra", 2, listOf(fish("neon_tetra", 6)), 60.0))
    }

    @Test fun tankRangesConflict() {
        val r = CompatibilityChecker.check(FishCatalog.find("goldfish")!!, 2, emptyList(), 150.0, WaterRanges(tempMinC = 25.0, tempMaxC = 28.0)).map { it.issue }
        assertTrue(CompatibilityIssue.TEMPERATURE_MISMATCH in r)
    }
}
