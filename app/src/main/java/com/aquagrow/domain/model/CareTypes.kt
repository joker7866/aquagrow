package com.aquagrow.domain.model

/*
 * Enums shared by the data and UI layers. Room stores them by name, so renaming a
 * constant needs a migration; adding new ones is safe.
 */

enum class CareModule { GARDEN, AQUARIUM }

// ---------- Garden ----------

enum class PlantCategory { VEGETABLE, LEAFY_GREEN, HERB, FLOWER, FRUIT, SUCCULENT, OTHER }

enum class SunlightLevel { FULL_SUN, PARTIAL_SUN, SHADE }

enum class PlantingMethod { SEED, SEEDLING, CUTTING, TUBER_OR_BULB, SAPLING }

/** The user's own observation of how the plant looks, not a diagnosis. */
enum class PlantCondition { THRIVING, GOOD, NEEDS_ATTENTION, STRUGGLING }

enum class SoilMoisture { DRY, MOIST, WET }

enum class FertilizerType { COMPOST, VERMICOMPOST, ORGANIC, NPK, LIQUID, SEAWEED, CUSTOM }

enum class FertilizerMethod { MIXED_IN_SOIL, TOP_DRESSING, SOIL_DRENCH, FOLIAR_SPRAY, OTHER }

/** Indian cropping seasons used by the planner. */
enum class Season { KHARIF, RABI, ZAID }

// ---------- Aquarium ----------

enum class TankType { COMMUNITY, PLANTED, SPECIES_ONLY, BREEDING, COLDWATER, OTHER }

enum class Temperament { PEACEFUL, SEMI_AGGRESSIVE, AGGRESSIVE }

enum class MaintenanceType {
    WATER_CHANGE,
    FILTER_INSPECTION,
    FILTER_MAINTENANCE,
    GLASS_CLEANING,
    GRAVEL_VACUUM,
    EQUIPMENT_CHECK,
    PLANT_TRIMMING,
    OTHER,
}

enum class WaterParameter { TEMPERATURE, PH, AMMONIA, NITRITE, NITRATE, GH, KH }

// ---------- Shared ----------

enum class InventoryCategory(val module: CareModule) {
    SEEDS(CareModule.GARDEN),
    POTS(CareModule.GARDEN),
    GROW_BAGS(CareModule.GARDEN),
    SOIL(CareModule.GARDEN),
    FERTILIZER(CareModule.GARDEN),
    TOOLS(CareModule.GARDEN),
    PEST_CONTROL(CareModule.GARDEN),
    FISH_FOOD(CareModule.AQUARIUM),
    WATER_CONDITIONER(CareModule.AQUARIUM),
    FILTER_MEDIA(CareModule.AQUARIUM),
    TEST_KIT(CareModule.AQUARIUM),
    EQUIPMENT(CareModule.AQUARIUM),
    OTHER_GARDEN(CareModule.GARDEN),
    OTHER_AQUARIUM(CareModule.AQUARIUM),
}

enum class ExpenseCategory {
    PLANTS,
    SEEDS,
    SOIL,
    POTS,
    FERTILIZER,
    TOOLS,
    FISH,
    FISH_FOOD,
    WATER_CARE,
    EQUIPMENT,
    OTHER,
}

enum class TaskType(val module: CareModule?) {
    WATERING(CareModule.GARDEN),
    FERTILIZING(CareModule.GARDEN),
    SOWING(CareModule.GARDEN),
    TRANSPLANTING(CareModule.GARDEN),
    HARVEST(CareModule.GARDEN),
    FEEDING(CareModule.AQUARIUM),
    WATER_TEST(CareModule.AQUARIUM),
    MAINTENANCE(CareModule.AQUARIUM),
    CUSTOM(null),
}

enum class OccurrenceStatus { PENDING, DONE, SKIPPED }

enum class RecurrenceType { ONCE, DAILY, WEEKLY, MONTHLY }

enum class ReminderKind { DUE, MISSED, LOW_STOCK, EXPIRY }
