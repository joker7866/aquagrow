package com.aquagrow.ui

import android.Manifest
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.aquagrow.MainActivity
import com.aquagrow.R
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * End-to-end flows on the real app (real database, real navigation). The test orchestrator
 * clears app data before each test (clearPackageData), so every test starts at Welcome.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowsTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    // Grant notifications up front so onboarding skips the system permission dialog.
    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(
            if (Build.VERSION.SDK_INT >= 33) {
                GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                GrantPermissionRule.grant()
            },
        )
        .around(compose)

    private fun str(@StringRes id: Int, vararg args: Any): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun waitForText(text: String, substring: Boolean = false) = compose.waitUntilAtLeastOneExists(hasText(text, substring = substring), timeoutMillis = 15_000)

    private fun field(label: String): SemanticsNodeInteraction = compose.onNode(hasSetTextAction() and (hasText(label, substring = true) or hasContentDescription(label)))

    private fun onboardBoth() {
        waitForText(str(R.string.welcome_get_started))
        compose.onNodeWithText(str(R.string.welcome_get_started)).performClick()
        waitForText(str(R.string.onboarding_both_title))
        compose.onNodeWithText(str(R.string.onboarding_both_title)).performClick()
        compose.onNodeWithText(str(R.string.action_continue)).performClick()
        waitForText(str(R.string.home_empty_title))
    }

    @Test
    fun onboarding_endsOnEmptyHome() {
        onboardBoth()
        compose.onNodeWithText(str(R.string.home_empty_add_plant)).assertExists()
        compose.onNodeWithText(str(R.string.home_empty_add_aquarium)).assertExists()
    }

    @Test
    fun addPlant_opensItsDetailPage() {
        onboardBoth()
        compose.onNodeWithText(str(R.string.home_empty_add_plant)).performClick()
        waitForText(str(R.string.add_plant_title))
        field(str(R.string.add_plant_name)).performTextInput("Test basil")
        compose.onNodeWithText(str(R.string.action_save)).performClick()
        // Watering is on by default but has no interval (we never invent one), so Save must ask for it.
        waitForText(str(R.string.schedule_error_interval))
        // The "fix the fields" snackbar may cover the button, so trigger its click action directly.
        compose.onNodeWithContentDescription(str(R.string.form_increase, str(R.string.add_plant_days)))
            .performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        waitForText("1", substring = true)
        compose.onNodeWithText(str(R.string.action_save)).performClick()
        // The form closes and the new plant's page opens.
        compose.waitUntilDoesNotExist(hasText(str(R.string.add_plant_title)), timeoutMillis = 15_000)
        compose.onAllNodesWithText("Test basil")[0].assertExists()
    }

    @Test
    fun markTaskDone_onHome() {
        onboardBoth()
        compose.onNodeWithText(str(R.string.home_empty_add_plant)).performClick()
        waitForText(str(R.string.add_plant_title))
        field(str(R.string.add_plant_name)).performTextInput("Test mint")
        // Every 1 day starting today: one watering task due today.
        compose.onNodeWithContentDescription(str(R.string.form_increase, str(R.string.add_plant_days))).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.action_save)).performClick()
        compose.waitUntilDoesNotExist(hasText(str(R.string.add_plant_title)), timeoutMillis = 15_000)
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        val task = hasContentDescription("Test mint", substring = true) and isToggleable()
        compose.waitUntilAtLeastOneExists(task, timeoutMillis = 15_000)
        compose.onNode(task).assertIsOff().performClick()
        // Saved to the database, then the list updates.
        compose.waitUntilAtLeastOneExists(task and isOn(), timeoutMillis = 10_000)
    }

    @Test
    fun logWaterTest_showsReadingOnTank() {
        onboardBoth()
        compose.onNodeWithText(str(R.string.home_empty_add_aquarium)).performClick()
        compose.waitUntilAtLeastOneExists(hasSetTextAction(), 15_000)
        field(str(R.string.add_tank_name)).performTextInput("Test tank")
        compose.onNodeWithText(str(R.string.add_tank_save)).performClick()
        waitForText(str(R.string.tank_log_water_test))
        compose.onAllNodesWithText(str(R.string.tank_log_water_test))[0].performClick()
        waitForText(str(R.string.water_sheet_title))
        field(str(R.string.param_ph)).performTextInput("7.4")
        compose.onNodeWithText(str(R.string.action_save)).performClick()
        // The sheet closes and the tank shows the reading instead of the empty message.
        compose.waitUntilDoesNotExist(hasText(str(R.string.tank_no_tests)), timeoutMillis = 15_000)
        compose.onAllNodesWithText("7.4", substring = true)[0].assertExists()
    }

    @Test
    fun backupScreen_opensFromProfile() {
        onboardBoth()
        compose.onNodeWithText(str(R.string.nav_profile)).performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(str(R.string.profile_backup)))
        compose.onNodeWithText(str(R.string.profile_backup)).performClick()
        waitForText(str(R.string.backup_export_title))
        compose.onNodeWithText(str(R.string.backup_restore_button)).assertExists()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(str(R.string.backup_ics_button)))
        compose.onNodeWithText(str(R.string.backup_ics_button)).assertExists()
        // The system file picker itself is outside the app and can't be driven from Compose tests;
        // the export → JSON → restore path is covered by BackupRoundTripTest on a real database.
    }
}
