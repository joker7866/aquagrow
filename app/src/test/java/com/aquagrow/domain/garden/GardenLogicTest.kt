package com.aquagrow.domain.garden

import com.aquagrow.domain.catalog.PlantCatalog
import com.aquagrow.domain.model.RecurrenceType
import com.aquagrow.domain.schedule.ScheduleError
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.schedule.ScheduleKind
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HarvestEstimatorTest {
    private val planted = LocalDate.of(2026, 8, 1)

    @Test
    fun upcomingWindow() {
        val e = HarvestEstimator.estimate(planted, 75, 100, LocalDate.of(2026, 9, 1))!!
        assertEquals(LocalDate.of(2026, 10, 15), e.earliest)
        assertEquals(LocalDate.of(2026, 11, 9), e.latest)
        assertEquals(HarvestPhase.UPCOMING, e.phase)
        assertEquals(44L, e.daysToEarliest)
        assertEquals(31f / 75f, e.progress, 0.0001f)
    }

    @Test
    fun inWindowAndPast() {
        assertEquals(HarvestPhase.IN_WINDOW, HarvestEstimator.estimate(planted, 75, 100, LocalDate.of(2026, 10, 15))!!.phase)
        assertEquals(HarvestPhase.IN_WINDOW, HarvestEstimator.estimate(planted, 75, 100, LocalDate.of(2026, 11, 9))!!.phase)
        val past = HarvestEstimator.estimate(planted, 75, 100, LocalDate.of(2026, 11, 10))!!
        assertEquals(HarvestPhase.PAST_WINDOW, past.phase)
        assertEquals(1f, past.progress, 0f)
    }

    @Test
    fun missingMaxUsesMin_andInvalidMinGivesNoEstimate() {
        val e = HarvestEstimator.estimate(planted, 30, null, planted)!!
        assertEquals(e.earliest, e.latest)
        assertNull(HarvestEstimator.estimate(planted, null, 60, planted))
        assertNull(HarvestEstimator.estimate(planted, 0, 60, planted))
    }

    @Test
    fun catalogPlantsWithHarvestDaysHaveSaneRanges() {
        PlantCatalog.all.filter { it.harvestDaysMin != null }.forEach {
            assertTrue(it.name, it.harvestDaysMax!! >= it.harvestDaysMin!!)
        }
        assertEquals(20, PlantCatalog.all.size)
    }
}

class ScheduleInputTest {
    private val start = LocalDate.of(2026, 10, 8)

    @Test
    fun intervalIsNeverInvented() {
        val input = ScheduleInput(kind = ScheduleKind.EVERY_N_DAYS, startDate = start)
        assertEquals(ScheduleError.MISSING_INTERVAL, input.validate())
        assertNull(input.copy(interval = 3).validate())
        assertEquals(ScheduleError.INTERVAL_RANGE, input.copy(interval = 0).validate())
    }

    @Test
    fun weeklyNeedsDays() {
        val input = ScheduleInput(kind = ScheduleKind.WEEKLY, startDate = start)
        assertEquals(ScheduleError.NO_DAYS, input.validate())
        val rule = input.copy(daysOfWeek = setOf(DayOfWeek.MONDAY)).toRule()
        assertEquals(RecurrenceType.WEEKLY, rule.type)
    }

    @Test
    fun dailyNeedsNothing() {
        val rule = ScheduleInput(kind = ScheduleKind.DAILY, startDate = start).also { assertNull(it.validate()) }.toRule()
        assertEquals(1, rule.interval)
    }
}

class PlantValidatorTest {
    private val today = LocalDate.of(2026, 10, 8)
    private val ok = PlantFormInput("Tomato", today, null, 75, 100, listOf("Seed"), null, null)

    @Test fun validInput() = assertTrue(PlantValidator.validate(ok, today).isEmpty())

    @Test fun blankName() = assertTrue(PlantFieldError.NAME_REQUIRED in PlantValidator.validate(ok.copy(name = "  "), today))

    @Test fun futurePlanting() = assertTrue(PlantFieldError.PLANTING_DATE_IN_FUTURE in PlantValidator.validate(ok.copy(plantingDate = today.plusDays(1)), today))

    @Test fun sowingAfterPlanting() = assertTrue(PlantFieldError.SOWING_AFTER_PLANTING in PlantValidator.validate(ok.copy(plantingDate = today.minusDays(5), sowingDate = today), today))

    @Test fun harvestRange() {
        assertTrue(PlantFieldError.HARVEST_RANGE_INVALID in PlantValidator.validate(ok.copy(harvestDaysMin = 90, harvestDaysMax = 60), today))
        assertTrue(PlantFieldError.HARVEST_RANGE_INVALID in PlantValidator.validate(ok.copy(harvestDaysMin = null, harvestDaysMax = 60), today))
    }

    @Test fun emptyStages() = assertTrue(PlantFieldError.STAGES_EMPTY in PlantValidator.validate(ok.copy(stages = listOf(" ")), today))

    @Test fun wateringOnWithoutInterval() {
        val bad = ok.copy(watering = ScheduleInput(kind = ScheduleKind.EVERY_N_DAYS, startDate = today))
        assertTrue(PlantFieldError.WATERING_SCHEDULE_INVALID in PlantValidator.validate(bad, today))
    }
}
