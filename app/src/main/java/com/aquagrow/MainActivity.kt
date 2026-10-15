package com.aquagrow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.domain.model.ThemeMode
import com.aquagrow.ui.AquaGrowAppUi
import com.aquagrow.ui.MainUiState
import com.aquagrow.ui.MainViewModel
import com.aquagrow.ui.navigation.NavRequest
import com.aquagrow.ui.navigation.NavigationRequests
import com.aquagrow.ui.theme.AquaGrowTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var navigationRequests: NavigationRequests

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash up until preferences are read, so the first frame is the right screen.
        installSplashScreen().setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val themeMode = (state as? MainUiState.Ready)?.preferences?.themeMode ?: ThemeMode.SYSTEM
            AquaGrowTheme(themeMode = themeMode) {
                when (val s = state) {
                    // Preferences load in a few ms; show the plain background meanwhile so
                    // we never flash Welcome to a user who already onboarded.
                    MainUiState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

                    is MainUiState.Ready -> AquaGrowAppUi(preferences = s.preferences, navigationRequests = navigationRequests)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Notification taps: open the plant / tank, or the calendar on that day. */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val plantId = intent.getLongExtra(EXTRA_PLANT_ID, -1)
        val aquariumId = intent.getLongExtra(EXTRA_AQUARIUM_ID, -1)
        val date = intent.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        val request = when {
            intent.getBooleanExtra(EXTRA_OPEN_INVENTORY, false) -> NavRequest.OpenInventory
            plantId > 0 -> NavRequest.OpenPlant(plantId)
            aquariumId > 0 -> NavRequest.OpenTank(aquariumId)
            date != Long.MIN_VALUE -> NavRequest.OpenCalendar(LocalDate.ofEpochDay(date))
            else -> null
        }
        request?.let(navigationRequests::request)
    }

    companion object {
        const val EXTRA_PLANT_ID = "open_plant_id"
        const val EXTRA_AQUARIUM_ID = "open_aquarium_id"
        const val EXTRA_DATE = "open_date"
        const val EXTRA_MODULE = "open_module"
        const val EXTRA_OPEN_INVENTORY = "open_inventory"
    }
}
