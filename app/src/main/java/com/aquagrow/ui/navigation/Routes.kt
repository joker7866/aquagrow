package com.aquagrow.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aquagrow.R
import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes.

@Serializable data object WelcomeRoute

@Serializable data object OnboardingRoute

@Serializable data object NotificationPermissionRoute

// Top level (bottom nav)
@Serializable data object HomeRoute

@Serializable data object GardenRoute

@Serializable data object AquariumRoute

@Serializable data object CalendarRoute

@Serializable data object ProfileRoute

// Garden
@Serializable data class PlantDetailRoute(val plantId: Long)

/** [plantId] null = add, otherwise edit. */
@Serializable data class AddPlantRoute(val plantId: Long? = null)

@Serializable data object WateringRoute

@Serializable data object FertilizerRoute

@Serializable data object PlannerRoute

// Aquarium
@Serializable data class TankDetailRoute(val aquariumId: Long)

/** [aquariumId] null = add, otherwise edit. */
@Serializable data class AddAquariumRoute(val aquariumId: Long? = null)

/** [fishId] null = add (optionally into [aquariumId]), otherwise edit. */
@Serializable data class AddFishRoute(val fishId: Long? = null, val aquariumId: Long? = null)

@Serializable data object FeedingRoute

@Serializable data object WaterTestsRoute

@Serializable data object MaintenanceRoute

// Shared

/** [taskId] null = new task; [dateEpochDay] pre-fills the start date. */
@Serializable data class EditTaskRoute(val taskId: Long? = null, val dateEpochDay: Long? = null)

@Serializable data object ReminderHistoryRoute

/** [action] = "backup", "restore" or "ics". */
@Serializable data class BackupRoute(val action: String = "backup")

@Serializable data object ReminderSettingsRoute

@Serializable data object AnalyticsRoute

@Serializable data object PrivacyRoute

@Serializable data object SearchRoute

@Serializable data class AssistantRoute(val module: String, val plantId: Long? = null, val aquariumId: Long? = null)

@Serializable data object GalleryRoute

/** [module] = CareModule name to pre-filter, or null for everything. */
@Serializable data class InventoryRoute(val module: String? = null)

enum class TopLevelDestination(
    val route: Any,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    HOME(HomeRoute, R.string.nav_home, R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled),
    GARDEN(GardenRoute, R.string.nav_garden, R.drawable.ic_nav_garden, R.drawable.ic_nav_garden_filled),
    AQUARIUM(AquariumRoute, R.string.nav_aquarium, R.drawable.ic_nav_aquarium, R.drawable.ic_nav_aquarium_filled),
    CALENDAR(CalendarRoute, R.string.nav_calendar, R.drawable.ic_nav_calendar, R.drawable.ic_nav_calendar_filled),
    PROFILE(ProfileRoute, R.string.nav_profile, R.drawable.ic_nav_profile, R.drawable.ic_nav_profile_filled),
}
