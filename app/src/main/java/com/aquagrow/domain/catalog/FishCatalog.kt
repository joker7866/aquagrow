package com.aquagrow.domain.catalog

import com.aquagrow.domain.model.Temperament

/**
 * Reference data for common freshwater fish. Ranges are general guidance from typical
 * hobby sources; individual fish, strains and local water vary. Used for defaults and
 * soft compatibility hints, never for diagnosis.
 */
data class CatalogFish(
    val key: String,
    val name: String,
    val scientificName: String,
    val temperament: Temperament,
    /** Typical adult length, cm. */
    val adultSizeCm: Double,
    val tempMinC: Double,
    val tempMaxC: Double,
    val phMin: Double,
    val phMax: Double,
    /** Rough minimum tank volume for a small group, litres. */
    val minTankLiters: Int,
    /** Schooling/shoaling fish do best in groups of at least this many (1 = fine alone). */
    val minGroup: Int,
    val diet: String,
    val careNotes: List<String>,
    val longFinned: Boolean = false,
    val finNipper: Boolean = false,
    val coldWater: Boolean = false,
    /** Same-species males may fight (e.g. betta). */
    val malesTerritorial: Boolean = false,
    val labyrinth: Boolean = false,
    /** Large enough to eat very small tankmates as an adult. */
    val mayEatSmallFish: Boolean = false,
    val bottomDweller: Boolean = false,
)

object FishCatalog {
    val all: List<CatalogFish> = listOf(
        CatalogFish(
            "guppy", "Guppy", "Poecilia reticulata", Temperament.PEACEFUL, 4.0, 22.0, 28.0, 7.0, 8.0, 40, 3,
            "Omnivore: small flakes or pellets, with occasional live or frozen foods.",
            listOf("Livebearer: expect fry if males and females are kept together.", "Males have long, colourful fins."),
            longFinned = true,
        ),
        CatalogFish(
            "molly", "Molly", "Poecilia sphenops", Temperament.PEACEFUL, 10.0, 24.0, 28.0, 7.5, 8.5, 80, 3,
            "Omnivore that grazes on algae; include plant-based foods.",
            listOf("Prefers harder, more alkaline water.", "Livebearer: grows larger than guppies."),
        ),
        CatalogFish(
            "platy", "Platy", "Xiphophorus maculatus", Temperament.PEACEFUL, 6.0, 20.0, 26.0, 7.0, 8.2, 40, 3,
            "Omnivore: flakes plus some vegetable matter.",
            listOf("Hardy and good for community tanks.", "Livebearer."),
        ),
        CatalogFish(
            "betta", "Betta", "Betta splendens", Temperament.SEMI_AGGRESSIVE, 6.5, 24.0, 28.0, 6.5, 7.5, 20, 1,
            "Carnivore: betta pellets and small frozen or live foods.",
            listOf("Keep only one male per tank.", "Breathes air at the surface; leave a gap under the lid.", "Prefers gentle water flow."),
            longFinned = true, malesTerritorial = true, labyrinth = true,
        ),
        CatalogFish(
            "goldfish", "Goldfish", "Carassius auratus", Temperament.PEACEFUL, 20.0, 18.0, 23.0, 7.0, 8.0, 120, 2,
            "Omnivore: sinking pellets and vegetables.",
            listOf("A cool-water fish: not suited to heated tropical tanks.", "Produces a lot of waste; strong filtration helps.", "Grows much larger than many people expect."),
            coldWater = true,
        ),
        CatalogFish(
            "angelfish", "Angelfish", "Pterophyllum scalare", Temperament.SEMI_AGGRESSIVE, 15.0, 24.0, 29.0, 6.0, 7.5, 120, 1,
            "Omnivore: flakes, pellets and frozen foods.",
            listOf("Needs a tall tank.", "Adults may eat very small fish such as neon tetras.", "Pairs can become territorial when breeding."),
            longFinned = true, mayEatSmallFish = true,
        ),
        CatalogFish(
            "neon_tetra", "Neon Tetra", "Paracheirodon innesi", Temperament.PEACEFUL, 3.5, 21.0, 26.0, 5.5, 7.0, 40, 6,
            "Omnivore: small flakes and micro pellets.",
            listOf("Shoaling fish: keep a group.", "Prefers a planted, established tank and stable water."),
        ),
        CatalogFish(
            "zebra_danio", "Zebra Danio", "Danio rerio", Temperament.PEACEFUL, 5.0, 18.0, 25.0, 6.5, 7.5, 60, 6,
            "Omnivore: flakes and small live or frozen foods.",
            listOf("Very active swimmer: give open space.", "May nip long fins, especially in small groups."),
            finNipper = true,
        ),
        CatalogFish(
            "corydoras", "Corydoras", "Corydoras spp.", Temperament.PEACEFUL, 6.0, 22.0, 26.0, 6.0, 7.8, 60, 6,
            "Omnivore bottom feeder: sinking pellets or wafers.",
            listOf("Keep in a group.", "Soft sand protects their barbels.", "Not a clean-up crew: they need their own food."),
            bottomDweller = true,
        ),
        CatalogFish(
            "gourami", "Gourami", "Trichogaster lalius", Temperament.SEMI_AGGRESSIVE, 8.5, 24.0, 28.0, 6.0, 7.5, 60, 1,
            "Omnivore: flakes, pellets and small frozen foods.",
            listOf("Males can be territorial with each other.", "Breathes air at the surface.", "Likes floating plants."),
            malesTerritorial = true, labyrinth = true,
        ),
    )

    private val byKey = all.associateBy { it.key }
    fun find(key: String?): CatalogFish? = key?.let(byKey::get)
}
