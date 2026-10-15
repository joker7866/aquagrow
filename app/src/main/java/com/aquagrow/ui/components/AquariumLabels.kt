package com.aquagrow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aquagrow.R
import com.aquagrow.domain.aquarium.CompatibilityIssue
import com.aquagrow.domain.aquarium.CompatibilityWarning
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.TankType
import com.aquagrow.domain.model.Temperament
import com.aquagrow.domain.model.WaterParameter

@Composable
fun TankType.label(): String = stringResource(
    when (this) {
        TankType.COMMUNITY -> R.string.tank_type_community
        TankType.PLANTED -> R.string.tank_type_planted
        TankType.SPECIES_ONLY -> R.string.tank_type_species
        TankType.BREEDING -> R.string.tank_type_breeding
        TankType.COLDWATER -> R.string.tank_type_coldwater
        TankType.OTHER -> R.string.tank_type_other
    },
)

@Composable
fun Temperament.label(): String = stringResource(
    when (this) {
        Temperament.PEACEFUL -> R.string.temperament_peaceful
        Temperament.SEMI_AGGRESSIVE -> R.string.temperament_semi
        Temperament.AGGRESSIVE -> R.string.temperament_aggressive
    },
)

@Composable
fun MaintenanceType.label(): String = stringResource(
    when (this) {
        MaintenanceType.WATER_CHANGE -> R.string.maint_water_change
        MaintenanceType.FILTER_INSPECTION -> R.string.maint_filter_inspection
        MaintenanceType.FILTER_MAINTENANCE -> R.string.maint_filter_maintenance
        MaintenanceType.GLASS_CLEANING -> R.string.maint_glass
        MaintenanceType.GRAVEL_VACUUM -> R.string.maint_gravel
        MaintenanceType.EQUIPMENT_CHECK -> R.string.maint_equipment
        MaintenanceType.PLANT_TRIMMING -> R.string.maint_plants
        MaintenanceType.OTHER -> R.string.maint_other
    },
)

/** English title stored on the task row (shown with the tank name). */
fun MaintenanceType.taskTitle(): String = when (this) {
    MaintenanceType.WATER_CHANGE -> "Water change"
    MaintenanceType.FILTER_INSPECTION -> "Filter inspection"
    MaintenanceType.FILTER_MAINTENANCE -> "Filter maintenance"
    MaintenanceType.GLASS_CLEANING -> "Glass cleaning"
    MaintenanceType.GRAVEL_VACUUM -> "Gravel vacuum"
    MaintenanceType.EQUIPMENT_CHECK -> "Equipment check"
    MaintenanceType.PLANT_TRIMMING -> "Plant trimming"
    MaintenanceType.OTHER -> "Maintenance"
}

@Composable
fun WaterParameter.label(): String = stringResource(
    when (this) {
        WaterParameter.TEMPERATURE -> R.string.param_temperature
        WaterParameter.PH -> R.string.param_ph
        WaterParameter.AMMONIA -> R.string.param_ammonia
        WaterParameter.NITRITE -> R.string.param_nitrite
        WaterParameter.NITRATE -> R.string.param_nitrate
        WaterParameter.GH -> R.string.param_gh
        WaterParameter.KH -> R.string.param_kh
    },
)

@Composable
fun WaterParameter.unit(): String = when (this) {
    WaterParameter.TEMPERATURE -> stringResource(R.string.unit_c)
    WaterParameter.PH -> ""
    WaterParameter.AMMONIA, WaterParameter.NITRITE, WaterParameter.NITRATE -> stringResource(R.string.unit_ppm)
    WaterParameter.GH, WaterParameter.KH -> stringResource(R.string.unit_dgh)
}

fun formatReading(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else "%.2f".format(v).trimEnd('0').trimEnd('.')

@Composable
fun CompatibilityWarning.text(): String {
    val with = otherSpecies?.let { " " + stringResource(R.string.compat_with, it) }.orEmpty()
    return when (issue) {
        CompatibilityIssue.TEMPERATURE_MISMATCH -> stringResource(R.string.compat_temperature, with)
        CompatibilityIssue.PH_MISMATCH -> stringResource(R.string.compat_ph, with)
        CompatibilityIssue.TANK_TOO_SMALL -> stringResource(R.string.compat_tank_small)
        CompatibilityIssue.TEMPERAMENT -> stringResource(R.string.compat_temperament, with)
        CompatibilityIssue.FIN_NIPPING -> stringResource(R.string.compat_fin_nipping, with)
        CompatibilityIssue.SAME_SPECIES_MALES -> stringResource(R.string.compat_same_males)
        CompatibilityIssue.LABYRINTH_RIVALS -> stringResource(R.string.compat_labyrinth, with)
        CompatibilityIssue.COLD_VS_TROPICAL -> stringResource(R.string.compat_cold, with)
        CompatibilityIssue.MAY_EAT_SMALL_FISH -> stringResource(R.string.compat_eat_small, with)
        CompatibilityIssue.GROUP_TOO_SMALL -> stringResource(R.string.compat_group)
    }
}

@Composable
fun com.aquagrow.domain.model.InventoryCategory.label(): String = stringResource(
    when (this) {
        com.aquagrow.domain.model.InventoryCategory.SEEDS -> R.string.inv_cat_seeds
        com.aquagrow.domain.model.InventoryCategory.POTS -> R.string.inv_cat_pots
        com.aquagrow.domain.model.InventoryCategory.GROW_BAGS -> R.string.inv_cat_grow_bags
        com.aquagrow.domain.model.InventoryCategory.SOIL -> R.string.inv_cat_soil
        com.aquagrow.domain.model.InventoryCategory.FERTILIZER -> R.string.inv_cat_fertilizer
        com.aquagrow.domain.model.InventoryCategory.TOOLS -> R.string.inv_cat_tools
        com.aquagrow.domain.model.InventoryCategory.PEST_CONTROL -> R.string.inv_cat_pest
        com.aquagrow.domain.model.InventoryCategory.FISH_FOOD -> R.string.inv_cat_fish_food
        com.aquagrow.domain.model.InventoryCategory.WATER_CONDITIONER -> R.string.inv_cat_conditioner
        com.aquagrow.domain.model.InventoryCategory.FILTER_MEDIA -> R.string.inv_cat_filter_media
        com.aquagrow.domain.model.InventoryCategory.TEST_KIT -> R.string.inv_cat_test_kit
        com.aquagrow.domain.model.InventoryCategory.EQUIPMENT -> R.string.inv_cat_equipment
        com.aquagrow.domain.model.InventoryCategory.OTHER_GARDEN, com.aquagrow.domain.model.InventoryCategory.OTHER_AQUARIUM -> R.string.inv_cat_other
    },
)
