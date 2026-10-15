package com.aquagrow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aquagrow.R
import com.aquagrow.domain.model.FertilizerMethod
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.PlantCondition
import com.aquagrow.domain.model.PlantingMethod
import com.aquagrow.domain.model.Season
import com.aquagrow.domain.model.SoilMoisture
import com.aquagrow.domain.model.SunlightLevel

// Enum → user-visible label. Kept in one place so every screen says the same thing.

@Composable
fun PlantCategory.label(): String = stringResource(
    when (this) {
        PlantCategory.VEGETABLE -> R.string.category_vegetable
        PlantCategory.LEAFY_GREEN -> R.string.category_leafy_green
        PlantCategory.HERB -> R.string.category_herb
        PlantCategory.FLOWER -> R.string.category_flower
        PlantCategory.FRUIT -> R.string.category_fruit
        PlantCategory.SUCCULENT -> R.string.category_succulent
        PlantCategory.OTHER -> R.string.category_other
    },
)

@Composable
fun SunlightLevel.label(): String = stringResource(
    when (this) {
        SunlightLevel.FULL_SUN -> R.string.sun_full
        SunlightLevel.PARTIAL_SUN -> R.string.sun_partial
        SunlightLevel.SHADE -> R.string.sun_shade
    },
)

@Composable
fun PlantingMethod.label(): String = stringResource(
    when (this) {
        PlantingMethod.SEED -> R.string.method_seed
        PlantingMethod.SEEDLING -> R.string.method_seedling
        PlantingMethod.CUTTING -> R.string.method_cutting
        PlantingMethod.TUBER_OR_BULB -> R.string.method_tuber
        PlantingMethod.SAPLING -> R.string.method_sapling
    },
)

@Composable
fun PlantCondition.label(): String = stringResource(
    when (this) {
        PlantCondition.THRIVING -> R.string.condition_thriving
        PlantCondition.GOOD -> R.string.condition_good
        PlantCondition.NEEDS_ATTENTION -> R.string.condition_needs_attention
        PlantCondition.STRUGGLING -> R.string.condition_struggling
    },
)

fun PlantCondition.tone(): ChipTone = when (this) {
    PlantCondition.THRIVING, PlantCondition.GOOD -> ChipTone.SUCCESS
    PlantCondition.NEEDS_ATTENTION, PlantCondition.STRUGGLING -> ChipTone.WARNING
}

@Composable
fun SoilMoisture.label(): String = stringResource(
    when (this) {
        SoilMoisture.DRY -> R.string.moisture_dry
        SoilMoisture.MOIST -> R.string.moisture_moist
        SoilMoisture.WET -> R.string.moisture_wet
    },
)

@Composable
fun FertilizerType.label(): String = stringResource(
    when (this) {
        FertilizerType.COMPOST -> R.string.fert_compost
        FertilizerType.VERMICOMPOST -> R.string.fert_vermicompost
        FertilizerType.ORGANIC -> R.string.fert_organic
        FertilizerType.NPK -> R.string.fert_npk
        FertilizerType.LIQUID -> R.string.fert_liquid
        FertilizerType.SEAWEED -> R.string.fert_seaweed
        FertilizerType.CUSTOM -> R.string.fert_custom
    },
)

@Composable
fun FertilizerMethod.label(): String = stringResource(
    when (this) {
        FertilizerMethod.MIXED_IN_SOIL -> R.string.fert_method_mixed
        FertilizerMethod.TOP_DRESSING -> R.string.fert_method_top
        FertilizerMethod.SOIL_DRENCH -> R.string.fert_method_drench
        FertilizerMethod.FOLIAR_SPRAY -> R.string.fert_method_foliar
        FertilizerMethod.OTHER -> R.string.fert_method_other
    },
)

@Composable
fun Season.label(): String = stringResource(
    when (this) {
        Season.KHARIF -> R.string.season_kharif
        Season.RABI -> R.string.season_rabi
        Season.ZAID -> R.string.season_zaid
    },
)
