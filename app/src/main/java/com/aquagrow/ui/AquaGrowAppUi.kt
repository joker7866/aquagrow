package com.aquagrow.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aquagrow.domain.model.UserPreferences
import com.aquagrow.ui.components.AppBottomNav
import com.aquagrow.ui.navigation.AquaGrowNavHost
import com.aquagrow.ui.navigation.HomeRoute
import com.aquagrow.ui.navigation.InventoryRoute
import com.aquagrow.ui.navigation.NavRequest
import com.aquagrow.ui.navigation.NavigationRequests
import com.aquagrow.ui.navigation.PlantDetailRoute
import com.aquagrow.ui.navigation.TankDetailRoute
import com.aquagrow.ui.navigation.TopLevelDestination
import com.aquagrow.ui.navigation.WelcomeRoute

@Composable
fun AquaGrowAppUi(preferences: UserPreferences, navigationRequests: NavigationRequests? = null) {
    val navController = rememberNavController()
    // Decided once: changing a NavHost's start destination later would rebuild the graph.
    val startDestination: Any = remember {
        if (preferences.onboardingCompleted) HomeRoute else WelcomeRoute
    }
    // Hide the Garden / Aquarium tab when that module is switched off.
    val destinations = TopLevelDestination.entries.filter {
        when (it) {
            TopLevelDestination.GARDEN -> preferences.gardenEnabled
            TopLevelDestination.AQUARIUM -> preferences.aquariumEnabled
            else -> true
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selected = destinations.firstOrNull { currentDestination.isOn(it) }
    val navigateTopLevel: (TopLevelDestination) -> Unit = { navController.navigateTopLevel(it) }

    // Navigation requested from outside Compose (notification taps). Only once onboarded.
    if (navigationRequests != null && preferences.onboardingCompleted) {
        val pending by navigationRequests.pending.collectAsStateWithLifecycle()
        LaunchedEffect(pending, backStackEntry != null) {
            val request = pending ?: return@LaunchedEffect
            if (backStackEntry == null) return@LaunchedEffect
            when (request) {
                is NavRequest.OpenPlant -> navController.navigate(PlantDetailRoute(request.plantId)) { launchSingleTop = true }
                is NavRequest.OpenTank -> navController.navigate(TankDetailRoute(request.aquariumId)) { launchSingleTop = true }
                is NavRequest.OpenCalendar -> navController.navigateTopLevel(TopLevelDestination.CALENDAR)
                NavRequest.OpenInventory -> navController.navigate(InventoryRoute()) { launchSingleTop = true }
            }
            navigationRequests.consumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Screens handle their own status-bar insets (some draw photos behind it).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (selected != null) AppBottomNav(destinations, selected, navigateTopLevel)
        },
    ) { innerPadding ->
        AquaGrowNavHost(
            navController = navController,
            startDestination = startDestination,
            onSelectTopLevel = navigateTopLevel,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private fun NavHostController.navigateTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        // Standard bottom-nav behaviour: one copy of each tab, state kept per tab.
        // Home is always the root once onboarding is done (Welcome is popped).
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean = this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
