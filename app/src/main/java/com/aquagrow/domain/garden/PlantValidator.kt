package com.aquagrow.domain.garden

import com.aquagrow.domain.schedule.ScheduleError
import com.aquagrow.domain.schedule.ScheduleInput
import java.time.LocalDate

enum class PlantFieldError {
    NAME_REQUIRED,
    NAME_TOO_LONG,
    PLANTING_DATE_IN_FUTURE,
    SOWING_AFTER_PLANTING,
    HARVEST_RANGE_INVALID,
    STAGES_EMPTY,
    WATERING_SCHEDULE_INVALID,
    FERTILIZER_SCHEDULE_INVALID,
}

data class PlantFormInput(
    val name: String,
    val plantingDate: LocalDate,
    val sowingDate: LocalDate?,
    val harvestDaysMin: Int?,
    val harvestDaysMax: Int?,
    val stages: List<String>,
    val watering: ScheduleInput?,
    val fertilizer: ScheduleInput?,
)

object PlantValidator {
    const val MAX_NAME = 60

    fun validate(input: PlantFormInput, today: LocalDate): Set<PlantFieldError> = buildSet {
        val name = input.name.trim()
        if (name.isEmpty()) add(PlantFieldError.NAME_REQUIRED)
        if (name.length > MAX_NAME) add(PlantFieldError.NAME_TOO_LONG)
        if (input.plantingDate.isAfter(today)) add(PlantFieldError.PLANTING_DATE_IN_FUTURE)
        if (input.sowingDate != null && input.sowingDate.isAfter(input.plantingDate)) add(PlantFieldError.SOWING_AFTER_PLANTING)
        val min = input.harvestDaysMin
        val max = input.harvestDaysMax
        if ((min != null && min !in 1..1000) || (max != null && max !in 1..1000) ||
            (min != null && max != null && max < min) || (min == null && max != null)
        ) {
            add(PlantFieldError.HARVEST_RANGE_INVALID)
        }
        if (input.stages.none { it.isNotBlank() }) add(PlantFieldError.STAGES_EMPTY)
        if (input.watering?.validate() != null) add(PlantFieldError.WATERING_SCHEDULE_INVALID)
        if (input.fertilizer?.validate() != null) add(PlantFieldError.FERTILIZER_SCHEDULE_INVALID)
    }

    fun scheduleError(input: ScheduleInput?): ScheduleError? = input?.validate()
}
