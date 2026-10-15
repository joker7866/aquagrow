package com.aquagrow.ui.preview

import com.aquagrow.domain.catalog.PlantCatalog
import com.aquagrow.domain.garden.DueStatus
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.AquariumSummary
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.PlantCondition
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterRanges
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.usecase.DayCare
import com.aquagrow.ui.screens.aquarium.AquariumUiState
import com.aquagrow.ui.screens.aquarium.TankCardUi
import com.aquagrow.ui.screens.aquarium.TankStatus
import com.aquagrow.ui.screens.garden.GardenUiState
import com.aquagrow.ui.screens.garden.PlantCardUi
import com.aquagrow.ui.screens.home.Greeting
import com.aquagrow.ui.screens.home.HomeUiState
import java.time.LocalDate
import java.time.LocalTime

/**
 * PREVIEW-ONLY SAMPLE DATA.
 *
 * Used exclusively by @Preview functions to render screens in Android Studio.
 * Never referenced by runtime code, never written to the database, never shown to users.
 */
object PreviewData {
    val today: LocalDate = LocalDate.of(2026, 10, 8)

    private fun task(id: Long, title: String, type: TaskType, module: CareModule, subject: String, time: LocalTime?) = CareTask(
        id = id,
        title = title,
        module = module,
        type = type,
        subjectName = subject,
        startDate = today.minusDays(30),
        reminderTime = time,
        recurrence = RecurrenceRule.everyDays(1),
    )

    val tasks: List<TaskOccurrence> = listOf(
        TaskOccurrence(
            task(1, "Feed", TaskType.FEEDING, CareModule.AQUARIUM, "[Preview] Guppies", LocalTime.of(8, 0)),
            originalDate = today.minusDays(1),
            date = today.minusDays(1),
        ),
        TaskOccurrence(
            task(2, "Water", TaskType.WATERING, CareModule.GARDEN, "[Preview] Tomato", LocalTime.of(7, 30)),
            originalDate = today,
            date = today,
            status = OccurrenceStatus.DONE,
        ),
        TaskOccurrence(
            task(3, "Test water", TaskType.WATER_TEST, CareModule.AQUARIUM, "[Preview] Living room tank", LocalTime.of(18, 0)),
            originalDate = today,
            date = today,
        ),
        TaskOccurrence(
            task(4, "Fertilize", TaskType.FERTILIZING, CareModule.GARDEN, "[Preview] Chilli", null),
            originalDate = today,
            date = today,
        ),
    )

    val week: List<DayCare> = listOf(4 to 4, 3 to 2, 5 to 5, 4 to 1, 2 to 0, 3 to 0, 2 to 0).mapIndexed { i, (s, d) ->
        DayCare(today.minusDays(3).plusDays(i.toLong()), scheduled = s, done = d)
    }

    val homeDashboard = HomeUiState.Dashboard(
        today = today,
        greeting = Greeting.MORNING,
        gardenEnabled = true,
        aquariumEnabled = true,
        plantCount = 12,
        fishCount = 18,
        tankCount = 2,
        tasks = tasks,
        week = week,
    )

    val plants: List<Plant> = listOf(
        Plant(
            id = 1, name = "[Preview] Tomato", variety = "Cherry", catalogKey = "tomato", category = PlantCategory.VEGETABLE,
            plantingDate = today.minusDays(42), potSize = "15 L grow bag", location = "Terrace",
            growthStages = PlantCatalog.find("tomato")!!.stages, currentStageIndex = 4, harvestDaysMin = 75, harvestDaysMax = 100,
            condition = PlantCondition.GOOD,
        ),
        Plant(
            id = 2, name = "[Preview] Tulsi", catalogKey = "tulsi", category = PlantCategory.HERB,
            plantingDate = today.minusDays(120), potSize = "10 inch", location = "Balcony",
            growthStages = PlantCatalog.find("tulsi")!!.stages, currentStageIndex = 3,
        ),
        Plant(
            id = 3,
            name = "[Preview] Rose",
            category = PlantCategory.FLOWER,
            plantingDate = today.minusDays(9),
            potSize = "12 inch",
            location = "Terrace",
            growthStages = PlantCatalog.find("rose")!!.stages,
            currentStageIndex = 1,
        ),
    )

    val garden = GardenUiState(
        loading = false,
        today = today,
        categories = listOf(PlantCategory.VEGETABLE, PlantCategory.HERB, PlantCategory.FLOWER),
        plants = listOf(
            PlantCardUi(plants[0], 43, null, DueStatus.Today),
            PlantCardUi(plants[1], 121, null, DueStatus.Upcoming(2)),
            PlantCardUi(plants[2], 10, null, DueStatus.Overdue(1)),
        ),
        totalPlants = 3,
        needsWaterCount = 2,
    )

    private val tank = Aquarium(
        id = 1,
        name = "[Preview] Living room tank",
        volumeLiters = 60.0,
        ranges = WaterRanges(tempMinC = 24.0, tempMaxC = 28.0, phMin = 6.5, phMax = 7.5, nitrateMaxPpm = 40.0),
    )
    private val previewTest = WaterTest(aquariumId = 1, testedAt = today.atStartOfDay().toInstant(java.time.ZoneOffset.UTC), temperatureC = 26.0, ph = 7.1, nitratePpm = 20.0)

    val aquarium = AquariumUiState(
        loading = false,
        tanks = listOf(
            TankCardUi(AquariumSummary(tank, previewTest, 14), TankStatus.OK, emptyList()),
            TankCardUi(AquariumSummary(tank.copy(id = 2, name = "[Preview] Betta tank", volumeLiters = 25.0), null, 1), TankStatus.NO_TEST, emptyList()),
        ),
        fish = listOf(
            Fish(id = 1, aquariumId = 1, speciesKey = "guppy", speciesName = "[Preview] Guppy", count = 6),
            Fish(id = 2, aquariumId = 1, speciesKey = "neon_tetra", speciesName = "[Preview] Neon Tetra", count = 8),
            Fish(id = 3, aquariumId = 2, speciesKey = "betta", speciesName = "[Preview] Betta", count = 1),
        ),
    )
}
