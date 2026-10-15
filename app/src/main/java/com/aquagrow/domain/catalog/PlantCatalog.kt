package com.aquagrow.domain.catalog

import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.PlantingMethod
import com.aquagrow.domain.model.Season
import com.aquagrow.domain.model.SunlightLevel

/** What the harvest-day estimate refers to. */
enum class HarvestKind { FIRST_HARVEST, FIRST_FLOWERS, LEAVES, NONE }

data class SeasonWindow(val season: Season, val startMonth: Int, val endMonth: Int)

/**
 * Built-in reference data for common Indian terrace-garden plants.
 *
 * All numbers are rough, typical values for guidance only. Harvest days are a range from
 * planting, shown in the UI as an estimate. Care tips deliberately avoid fixed watering
 * intervals and fertilizer quantities: those depend on pot, soil, weather and product label.
 */
data class CatalogPlant(
    val key: String,
    val name: String,
    val category: PlantCategory,
    val sunlight: SunlightLevel,
    val usualMethod: PlantingMethod,
    val stages: List<String>,
    val harvestDaysMin: Int?,
    val harvestDaysMax: Int?,
    val harvestKind: HarvestKind,
    val seasons: List<SeasonWindow>,
    val careTips: List<String>,
)

object PlantCatalog {
    private val FRUITING = listOf("Seed", "Germination", "Seedling", "Vegetative", "Flowering", "Fruiting", "Harvest")
    private val LEAFY = listOf("Seed", "Germination", "Seedling", "Leafy growth", "Harvest")
    private val TUBER = listOf("Planted", "Sprouting", "Vegetative", "Tuber forming", "Harvest")
    private val BULB = listOf("Seed", "Germination", "Seedling", "Bulb forming", "Harvest")
    private val CUTTING_HERB = listOf("Planted", "Rooting", "Vegetative", "Harvest")
    private val FLOWER = listOf("Planted", "Establishing", "Vegetative", "Budding", "Flowering")
    private val SEED_FLOWER = listOf("Seed", "Germination", "Seedling", "Vegetative", "Budding", "Flowering")
    private val SUCCULENT = listOf("Planted", "Establishing", "Growing", "Mature")

    private const val CHECK_SOIL = "Water when the top 2–3 cm of soil feels dry; pots dry faster in summer and on windy terraces."
    private const val DRAINAGE = "Use a pot with drainage holes so roots never sit in water."
    private const val LABEL = "If you feed, follow the product label for amounts; more is not better."

    private fun w(season: Season, start: Int, end: Int) = SeasonWindow(season, start, end)

    val all: List<CatalogPlant> = listOf(
        CatalogPlant(
            "tomato", "Tomato", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEEDLING, FRUITING,
            75, 100, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.KHARIF, 6, 7), w(Season.RABI, 9, 11)),
            listOf(CHECK_SOIL, "Give support (stake or cage) before plants get heavy.", "Water at the base, keeping leaves dry.", LABEL),
        ),
        CatalogPlant(
            "potato", "Potato", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.TUBER_OR_BULB, TUBER,
            90, 120, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.RABI, 10, 11)),
            listOf("Grow bags work well; add soil around stems as they grow (earthing up).", CHECK_SOIL, "Harvest after the foliage yellows and dies back."),
        ),
        CatalogPlant(
            "onion", "Onion", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEED, BULB,
            100, 150, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.RABI, 10, 12), w(Season.KHARIF, 6, 7)),
            listOf("Loose, well-draining soil helps bulbs swell.", CHECK_SOIL, "Leaves can be harvested early as spring onions."),
        ),
        CatalogPlant(
            "brinjal", "Brinjal", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEEDLING, FRUITING,
            90, 130, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.KHARIF, 6, 7), w(Season.RABI, 10, 11)),
            listOf(CHECK_SOIL, "Pick fruits while glossy; dull skin means they are past their best.", DRAINAGE),
        ),
        CatalogPlant(
            "chilli", "Chilli", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEEDLING, FRUITING,
            90, 120, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.KHARIF, 5, 6), w(Season.RABI, 9, 10)),
            listOf(CHECK_SOIL, "Inspect leaf undersides regularly for small insects.", DRAINAGE),
        ),
        CatalogPlant(
            "okra", "Okra", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEED, FRUITING,
            50, 65, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.ZAID, 2, 3), w(Season.KHARIF, 6, 7)),
            listOf("Pick pods young and tender, every day or two once fruiting starts.", CHECK_SOIL, "Loves heat; slow in cool weather."),
        ),
        CatalogPlant(
            "cucumber", "Cucumber", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEED, FRUITING,
            50, 70, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.ZAID, 2, 3), w(Season.KHARIF, 6, 7)),
            listOf("Provide a trellis or net to climb.", CHECK_SOIL, "Hand-pollinating flowers can help on terraces with few bees."),
        ),
        CatalogPlant(
            "bitter_gourd", "Bitter Gourd", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEED, FRUITING,
            55, 75, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.ZAID, 1, 3), w(Season.KHARIF, 6, 7)),
            listOf("Needs a trellis or pandal to climb.", CHECK_SOIL, "Harvest while fruits are still green and firm."),
        ),
        CatalogPlant(
            "bottle_gourd", "Bottle Gourd", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEED, FRUITING,
            60, 80, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.ZAID, 2, 3), w(Season.KHARIF, 6, 7)),
            listOf("A vigorous climber: give strong support and a large container.", CHECK_SOIL),
        ),
        CatalogPlant(
            "beans", "Beans", PlantCategory.VEGETABLE, SunlightLevel.FULL_SUN, PlantingMethod.SEED, FRUITING,
            50, 70, HarvestKind.FIRST_HARVEST,
            listOf(w(Season.RABI, 10, 11), w(Season.KHARIF, 6, 7)),
            listOf("Pole varieties need support; bush varieties do not.", CHECK_SOIL, "Pick pods regularly to keep plants producing."),
        ),
        CatalogPlant(
            "spinach", "Spinach", PlantCategory.LEAFY_GREEN, SunlightLevel.PARTIAL_SUN, PlantingMethod.SEED, LEAFY,
            30, 45, HarvestKind.LEAVES,
            listOf(w(Season.RABI, 9, 11)),
            listOf("Prefers cooler months; light afternoon shade helps in heat.", CHECK_SOIL, "Cut outer leaves first so the plant keeps growing."),
        ),
        CatalogPlant(
            "coriander", "Coriander", PlantCategory.HERB, SunlightLevel.PARTIAL_SUN, PlantingMethod.SEED, LEAFY,
            30, 45, HarvestKind.LEAVES,
            listOf(w(Season.RABI, 10, 11)),
            listOf("Lightly crush seeds before sowing.", "Bolts (flowers) quickly in hot weather.", CHECK_SOIL),
        ),
        CatalogPlant(
            "mint", "Mint", PlantCategory.HERB, SunlightLevel.PARTIAL_SUN, PlantingMethod.CUTTING, CUTTING_HERB,
            30, 60, HarvestKind.LEAVES,
            listOf(w(Season.ZAID, 2, 3), w(Season.KHARIF, 7, 8)),
            listOf("Spreads fast: grow it in its own pot.", "Likes soil that stays slightly moist, but not waterlogged.", "Pinch tips to keep it bushy."),
        ),
        CatalogPlant(
            "curry_leaves", "Curry Leaves", PlantCategory.HERB, SunlightLevel.FULL_SUN, PlantingMethod.SAPLING, CUTTING_HERB,
            180, 365, HarvestKind.LEAVES,
            listOf(w(Season.KHARIF, 6, 8)),
            listOf("Slow to establish; harvest lightly in the first year.", CHECK_SOIL, "Avoid overwatering in winter when growth slows."),
        ),
        CatalogPlant(
            "rose", "Rose", PlantCategory.FLOWER, SunlightLevel.FULL_SUN, PlantingMethod.SAPLING, FLOWER,
            60, 120, HarvestKind.FIRST_FLOWERS,
            listOf(w(Season.RABI, 10, 11)),
            listOf("At least six hours of direct sun for good blooms.", "Remove spent flowers to encourage new buds.", CHECK_SOIL, LABEL),
        ),
        CatalogPlant(
            "jasmine", "Jasmine", PlantCategory.FLOWER, SunlightLevel.FULL_SUN, PlantingMethod.SAPLING, FLOWER,
            90, 180, HarvestKind.FIRST_FLOWERS,
            listOf(w(Season.KHARIF, 6, 8)),
            listOf("Prune after the flowering season to shape the plant.", CHECK_SOIL, DRAINAGE),
        ),
        CatalogPlant(
            "hibiscus", "Hibiscus", PlantCategory.FLOWER, SunlightLevel.FULL_SUN, PlantingMethod.SAPLING, FLOWER,
            90, 180, HarvestKind.FIRST_FLOWERS,
            listOf(w(Season.KHARIF, 6, 8)),
            listOf("Check leaf undersides for small insects regularly.", CHECK_SOIL, "Bud drop is common after moving the pot; let it settle."),
        ),
        CatalogPlant(
            "marigold", "Marigold", PlantCategory.FLOWER, SunlightLevel.FULL_SUN, PlantingMethod.SEED, SEED_FLOWER,
            50, 70, HarvestKind.FIRST_FLOWERS,
            listOf(w(Season.RABI, 9, 10), w(Season.KHARIF, 6, 7), w(Season.ZAID, 1, 2)),
            listOf("Pinch the first buds for a bushier plant.", CHECK_SOIL, "Often grown alongside vegetables."),
        ),
        CatalogPlant(
            "aloe_vera", "Aloe Vera", PlantCategory.SUCCULENT, SunlightLevel.FULL_SUN, PlantingMethod.SAPLING, SUCCULENT,
            null, null, HarvestKind.NONE,
            listOf(w(Season.ZAID, 3, 4), w(Season.KHARIF, 6, 7)),
            listOf("Let the soil dry out completely between waterings.", "Use a gritty, fast-draining mix.", "Mature outer leaves can be cut once the plant is well established."),
        ),
        CatalogPlant(
            "tulsi", "Tulsi", PlantCategory.HERB, SunlightLevel.FULL_SUN, PlantingMethod.SEED, LEAFY,
            40, 60, HarvestKind.LEAVES,
            listOf(w(Season.ZAID, 3, 4), w(Season.KHARIF, 6, 6)),
            listOf("Pinch off flower spikes to keep leaves coming.", CHECK_SOIL, "Protect from cold winds in winter."),
        ),
    )

    private val byKey = all.associateBy { it.key }

    fun find(key: String?): CatalogPlant? = key?.let(byKey::get)

    /** The quick-pick chips shown first on Add plant. */
    val quickPicks: List<String> = listOf("tomato", "chilli", "okra", "brinjal", "spinach", "coriander", "tulsi", "rose")

    /** Default stages for a custom plant. */
    val defaultStages: List<String> = listOf("Seed", "Germination", "Seedling", "Vegetative", "Flowering", "Harvest")
}
