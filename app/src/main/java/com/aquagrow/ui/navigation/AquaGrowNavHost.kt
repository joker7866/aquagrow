package com.aquagrow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.aquagrow.domain.model.CareModule
import com.aquagrow.ui.screens.addaquarium.AddAquariumScreen
import com.aquagrow.ui.screens.addfish.AddFishScreen
import com.aquagrow.ui.screens.addplant.AddPlantScreen
import com.aquagrow.ui.screens.analytics.AnalyticsScreen
import com.aquagrow.ui.screens.aquarium.AquariumActions
import com.aquagrow.ui.screens.aquarium.AquariumScreen
import com.aquagrow.ui.screens.aquarium.FeedingScreen
import com.aquagrow.ui.screens.aquarium.MaintenanceScreen
import com.aquagrow.ui.screens.aquarium.WaterTestsScreen
import com.aquagrow.ui.screens.assistant.AssistantScreen
import com.aquagrow.ui.screens.calendar.CalendarActions
import com.aquagrow.ui.screens.calendar.CalendarScreen
import com.aquagrow.ui.screens.garden.FertilizerScreen
import com.aquagrow.ui.screens.garden.GardenActions
import com.aquagrow.ui.screens.garden.GardenScreen
import com.aquagrow.ui.screens.garden.PlannerScreen
import com.aquagrow.ui.screens.garden.WateringScreen
import com.aquagrow.ui.screens.home.HomeActions
import com.aquagrow.ui.screens.home.HomeScreen
import com.aquagrow.ui.screens.inventory.InventoryScreen
import com.aquagrow.ui.screens.onboarding.OnboardingScreen
import com.aquagrow.ui.screens.plantdetail.PlantDetailScreen
import com.aquagrow.ui.screens.profile.ProfileActions
import com.aquagrow.ui.screens.profile.ProfileScreen
import com.aquagrow.ui.screens.reminders.NotificationPermissionScreen
import com.aquagrow.ui.screens.reminders.ReminderHistoryScreen
import com.aquagrow.ui.screens.reminders.ReminderSettingsScreen
import com.aquagrow.ui.screens.reminders.notificationsAllowed
import com.aquagrow.ui.screens.tankdetail.TankDetailScreen
import com.aquagrow.ui.screens.task.EditTaskScreen
import com.aquagrow.ui.screens.tools.BackupScreen
import com.aquagrow.ui.screens.tools.GalleryScreen
import com.aquagrow.ui.screens.tools.PrivacyScreen
import com.aquagrow.ui.screens.tools.SearchActions
import com.aquagrow.ui.screens.tools.SearchScreen
import com.aquagrow.ui.screens.welcome.WelcomeScreen

@Composable
fun AquaGrowNavHost(
    navController: NavHostController,
    startDestination: Any,
    onSelectTopLevel: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onGetStarted = { navController.navigate(OnboardingRoute) },
                onRestoreBackup = { navController.navigate(BackupRoute("restore")) },
            )
        }
        composable<OnboardingRoute> {
            val context = androidx.compose.ui.platform.LocalContext.current
            OnboardingScreen(
                onFinished = {
                    // Step 2 (reminders permission) only when it is actually needed.
                    val needsPermission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !context.notificationsAllowed()
                    navController.navigate(if (needsPermission) NotificationPermissionRoute else HomeRoute) {
                        // Leaving onboarding is one-way: back from Home exits the app.
                        popUpTo<WelcomeRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<NotificationPermissionRoute> {
            NotificationPermissionScreen(onDone = {
                navController.navigate(HomeRoute) { popUpTo<NotificationPermissionRoute> { inclusive = true } }
            })
        }

        composable<HomeRoute> {
            HomeScreen(
                actions = HomeActions(
                    onAddPlant = { navController.navigate(AddPlantRoute()) },
                    onAddFish = { navController.navigate(AddFishRoute()) },
                    onAddTask = { navController.navigate(EditTaskRoute()) },
                    onLogWatering = { navController.navigate(WateringRoute) },
                    onLogFeeding = { navController.navigate(FeedingRoute) },
                    onAddAquarium = { navController.navigate(AddAquariumRoute()) },
                    onOpenGarden = { onSelectTopLevel(TopLevelDestination.GARDEN) },
                    onOpenAquarium = { onSelectTopLevel(TopLevelDestination.AQUARIUM) },
                    onOpenCalendar = { onSelectTopLevel(TopLevelDestination.CALENDAR) },
                    onOpenReminders = { navController.navigate(ReminderHistoryRoute) },
                ),
            )
        }
        composable<GardenRoute> {
            GardenScreen(
                actions = GardenActions(
                    onAddPlant = { navController.navigate(AddPlantRoute()) },
                    onOpenPlant = { id -> navController.navigate(PlantDetailRoute(id)) },
                    onOpenWatering = { navController.navigate(WateringRoute) },
                    onOpenFertilizer = { navController.navigate(FertilizerRoute) },
                    onOpenPlanner = { navController.navigate(PlannerRoute) },
                    onOpenInventory = { navController.navigate(InventoryRoute(CareModule.GARDEN.name)) },
                ),
            )
        }
        composable<AquariumRoute> {
            AquariumScreen(
                actions = AquariumActions(
                    onAddAquarium = { navController.navigate(AddAquariumRoute()) },
                    onOpenTank = { id -> navController.navigate(TankDetailRoute(id)) },
                    onAddFish = { navController.navigate(AddFishRoute()) },
                    onAddFishToTank = { id -> navController.navigate(AddFishRoute(aquariumId = id)) },
                    onEditFish = { id -> navController.navigate(AddFishRoute(fishId = id)) },
                    onOpenFeeding = { navController.navigate(FeedingRoute) },
                    onOpenWaterTests = { navController.navigate(WaterTestsRoute) },
                    onOpenMaintenance = { navController.navigate(MaintenanceRoute) },
                    onOpenInventory = { navController.navigate(InventoryRoute(CareModule.AQUARIUM.name)) },
                    onSearch = { navController.navigate(SearchRoute) },
                ),
            )
        }
        composable<CalendarRoute> {
            CalendarScreen(
                actions = CalendarActions(
                    onAddTask = { d -> navController.navigate(EditTaskRoute(dateEpochDay = d.toEpochDay())) },
                    onEditTask = { id -> navController.navigate(EditTaskRoute(taskId = id)) },
                    onOpenPlant = { id -> navController.navigate(PlantDetailRoute(id)) },
                    onOpenTank = { id -> navController.navigate(TankDetailRoute(id)) },
                ),
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                actions = ProfileActions(
                    onReminders = { navController.navigate(ReminderSettingsRoute) },
                    onReminderHistory = { navController.navigate(ReminderHistoryRoute) },
                    onInventory = { navController.navigate(InventoryRoute()) },
                    onAnalytics = { navController.navigate(AnalyticsRoute) },
                    onPlanner = { navController.navigate(PlannerRoute) },
                    onBackup = { navController.navigate(BackupRoute("backup")) },
                    onRestore = { navController.navigate(BackupRoute("restore")) },
                    onExportCalendar = { navController.navigate(BackupRoute("ics")) },
                    onPrivacy = { navController.navigate(PrivacyRoute) },
                    onSearch = { navController.navigate(SearchRoute) },
                    onGallery = { navController.navigate(GalleryRoute) },
                    onGardenAssistant = { navController.navigate(AssistantRoute(CareModule.GARDEN.name)) },
                    onAquariumAssistant = { navController.navigate(AssistantRoute(CareModule.AQUARIUM.name)) },
                ),
            )
        }
        composable<EditTaskRoute> { EditTaskScreen(onDone = back) }
        composable<ReminderSettingsRoute> { ReminderSettingsScreen(onBack = back, onOpenHistory = { navController.navigate(ReminderHistoryRoute) }) }
        composable<ReminderHistoryRoute> { ReminderHistoryScreen(onBack = back) }

        composable<PlantDetailRoute> {
            PlantDetailScreen(
                onBack = back,
                onEdit = { id -> navController.navigate(AddPlantRoute(id)) },
                onAskAssistant = { id -> navController.navigate(AssistantRoute(CareModule.GARDEN.name, plantId = id)) },
            )
        }
        composable<AddPlantRoute> { entry ->
            val isEdit = entry.toRoute<AddPlantRoute>().plantId != null
            AddPlantScreen(
                onDone = { savedId ->
                    if (savedId != null && !isEdit) {
                        // New plant: land on its detail page instead of going back.
                        navController.navigate(PlantDetailRoute(savedId)) { popUpTo<AddPlantRoute> { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable<WateringRoute> { WateringScreen(onBack = back, onOpenPlant = { navController.navigate(PlantDetailRoute(it)) }) }
        composable<FertilizerRoute> { FertilizerScreen(onBack = back, onOpenPlant = { navController.navigate(PlantDetailRoute(it)) }) }
        composable<PlannerRoute> { PlannerScreen(onBack = back) }
        composable<TankDetailRoute> {
            TankDetailScreen(
                onBack = back,
                onEdit = { id -> navController.navigate(AddAquariumRoute(id)) },
                onAddFish = { id -> navController.navigate(AddFishRoute(aquariumId = id)) },
                onEditFish = { id -> navController.navigate(AddFishRoute(fishId = id)) },
                onAskAssistant = { id -> navController.navigate(AssistantRoute(CareModule.AQUARIUM.name, aquariumId = id)) },
            )
        }
        composable<AddAquariumRoute> { entry ->
            val isEdit = entry.toRoute<AddAquariumRoute>().aquariumId != null
            AddAquariumScreen(
                onDone = { savedId ->
                    if (savedId != null && !isEdit) {
                        navController.navigate(TankDetailRoute(savedId)) { popUpTo<AddAquariumRoute> { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable<AddFishRoute> { AddFishScreen(onDone = back) }
        composable<FeedingRoute> { FeedingScreen(onBack = back) }
        composable<WaterTestsRoute> { WaterTestsScreen(onBack = back, onOpenTank = { navController.navigate(TankDetailRoute(it)) }) }
        composable<MaintenanceRoute> { MaintenanceScreen(onBack = back) }

        composable<InventoryRoute> { InventoryScreen(onBack = back) }
        composable<AnalyticsRoute> { AnalyticsScreen(onBack = back) }
        composable<BackupRoute> {
            val fromWelcome = navController.previousBackStackEntry?.destination?.hasRoute<WelcomeRoute>() == true
            BackupScreen(
                onBack = back,
                onRestored = {
                    // A restore on first launch replaces onboarding: go straight to Home.
                    if (fromWelcome) navController.navigate(HomeRoute) { popUpTo<WelcomeRoute> { inclusive = true } }
                },
            )
        }
        composable<PrivacyRoute> { PrivacyScreen(onBack = back) }
        composable<GalleryRoute> {
            GalleryScreen(
                onBack = back,
                onOpenPlant = { navController.navigate(PlantDetailRoute(it)) },
                onOpenTank = { navController.navigate(TankDetailRoute(it)) },
            )
        }
        composable<AssistantRoute> {
            AssistantScreen(
                onBack = back,
                onOpenProfile = { onSelectTopLevel(TopLevelDestination.PROFILE) },
            )
        }
        composable<SearchRoute> {
            SearchScreen(
                onBack = back,
                actions = SearchActions(
                    onPlant = { navController.navigate(PlantDetailRoute(it)) },
                    onTank = { navController.navigate(TankDetailRoute(it)) },
                    onFish = { navController.navigate(AddFishRoute(fishId = it)) },
                    onTask = { navController.navigate(EditTaskRoute(taskId = it)) },
                ),
            )
        }
    }
}
