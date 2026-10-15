package com.aquagrow.ui.screens.garden

import com.aquagrow.domain.garden.DueStatus
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.OccurrenceState
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.RecurrenceRule
import com.aquagrow.domain.model.TaskType
import com.aquagrow.testing.FakeGardenRepository
import com.aquagrow.testing.FakeTaskRepository
import com.aquagrow.testing.FakeTime
import com.aquagrow.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class GardenViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val time = FakeTime()
    private val today = time.today
    private val garden = FakeGardenRepository()
    private val tasks = FakeTaskRepository()

    private fun plant(id: Long, name: String) = Plant(
        id = id,
        name = name,
        category = PlantCategory.VEGETABLE,
        plantingDate = today.minusDays(10),
        growthStages = listOf("Seed"),
    )

    private fun watering(id: Long, plantId: Long, every: Int, start: java.time.LocalDate) = CareTask(
        id = id,
        title = "Water",
        module = CareModule.GARDEN,
        type = TaskType.WATERING,
        plantId = plantId,
        startDate = start,
        recurrence = RecurrenceRule.everyDays(every),
    )

    private suspend fun state(vm: GardenViewModel) = vm.uiState.first { !it.loading }

    @Test
    fun everyThreeDays_startedToday_isDueToday() = runTest {
        garden.plants.value = listOf(plant(1, "Tomato"))
        tasks.tasks.value = listOf(watering(1, 1, every = 3, start = today))
        val vm = GardenViewModel(garden, tasks, time)
        backgroundScope.launch { vm.uiState.collect {} }

        val card = state(vm).plants.single()
        assertEquals(DueStatus.Today, card.wateringStatus)
        assertEquals(1, state(vm).needsWaterCount)
    }

    @Test
    fun afterMarkingTodayDone_nextIsThreeDaysLater() = runTest {
        garden.plants.value = listOf(plant(1, "Tomato"))
        tasks.tasks.value = listOf(watering(1, 1, every = 3, start = today))
        tasks.states.value = listOf(OccurrenceState(1, today, OccurrenceStatus.DONE))
        val vm = GardenViewModel(garden, tasks, time)
        backgroundScope.launch { vm.uiState.collect {} }

        val card = state(vm).plants.single()
        assertEquals(DueStatus.Upcoming(3), card.wateringStatus)
        assertEquals(today.plusDays(3), card.nextWatering!!.date)
        assertEquals(0, state(vm).needsWaterCount)
    }

    @Test
    fun missedWatering_showsOverdueWithDays() = runTest {
        garden.plants.value = listOf(plant(1, "Chilli"))
        // Every 2 days from 4 days ago: due 4 and 2 days ago, and today. Today's is pending too,
        // but an overdue one takes priority so the user sees it has been missed.
        tasks.tasks.value = listOf(watering(1, 1, every = 2, start = today.minusDays(4)))
        tasks.states.value = listOf(OccurrenceState(1, today.minusDays(4), OccurrenceStatus.DONE))
        val vm = GardenViewModel(garden, tasks, time)
        backgroundScope.launch { vm.uiState.collect {} }

        assertEquals(DueStatus.Overdue(2), state(vm).plants.single().wateringStatus)
    }

    @Test
    fun rescheduledWatering_followsNewDate() = runTest {
        garden.plants.value = listOf(plant(1, "Okra"))
        tasks.tasks.value = listOf(watering(1, 1, every = 7, start = today))
        tasks.states.value = listOf(OccurrenceState(1, today, OccurrenceStatus.PENDING, rescheduledDate = today.plusDays(1)))
        val vm = GardenViewModel(garden, tasks, time)
        backgroundScope.launch { vm.uiState.collect {} }

        assertEquals(DueStatus.Upcoming(1), state(vm).plants.single().wateringStatus)
    }

    @Test
    fun plantWithoutSchedule_hasNoDueChip() = runTest {
        garden.plants.value = listOf(plant(1, "Rose"))
        val vm = GardenViewModel(garden, tasks, time)
        backgroundScope.launch { vm.uiState.collect {} }

        assertNull(state(vm).plants.single().wateringStatus)
    }

    @Test
    fun searchAndCategoryFilter() = runTest {
        garden.plants.value = listOf(plant(1, "Tomato"), plant(2, "Tulsi").copy(category = PlantCategory.HERB))
        val vm = GardenViewModel(garden, tasks, time)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onQueryChange("tul")
        assertEquals(listOf("Tulsi"), vm.uiState.first { it.query == "tul" }.plants.map { it.plant.name })
        vm.onQueryChange("")
        vm.onCategoryChange(PlantCategory.VEGETABLE)
        assertEquals(listOf("Tomato"), vm.uiState.first { it.category == PlantCategory.VEGETABLE && it.query == "" }.plants.map { it.plant.name })
    }
}
