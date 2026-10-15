package com.aquagrow.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aquagrow.BuildConfig
import com.aquagrow.R
import com.aquagrow.domain.model.NotificationSettings
import com.aquagrow.domain.model.ThemeMode
import com.aquagrow.domain.model.UserPreferences
import com.aquagrow.domain.repository.SettingsRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.screens.reminders.NotificationsBlockedBanner
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(val prefs: UserPreferences, val notifications: NotificationSettings)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<ProfileUiState?> = combine(prefs.preferences, settings.notificationSettings, ::ProfileUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setGarden(on: Boolean) = viewModelScope.launch { prefs.setGardenEnabled(on) }
    fun setAquarium(on: Boolean) = viewModelScope.launch { prefs.setAquariumEnabled(on) }
    fun setAi(on: Boolean) = viewModelScope.launch { prefs.setAiAssistantEnabled(on) }
    fun setTheme(m: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(m) }
    fun setCurrency(code: String) = viewModelScope.launch { prefs.setCurrencyCode(code) }
    fun setName(name: String) = viewModelScope.launch { prefs.setDisplayName(name) }
}

data class ProfileActions(
    val onReminders: () -> Unit = {},
    val onReminderHistory: () -> Unit = {},
    val onInventory: () -> Unit = {},
    val onAnalytics: () -> Unit = {},
    val onPlanner: () -> Unit = {},
    val onBackup: () -> Unit = {},
    val onRestore: () -> Unit = {},
    val onExportCalendar: () -> Unit = {},
    val onPrivacy: () -> Unit = {},
    val onSearch: () -> Unit = {},
    val onGallery: () -> Unit = {},
    val onGardenAssistant: () -> Unit = {},
    val onAquariumAssistant: () -> Unit = {},
)

/** Currencies offered in settings (₹ INR is the default). */
private val CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "AUD", "CAD", "LKR", "NPR", "BDT")

@Composable
fun ProfileScreen(actions: ProfileActions, vm: ProfileViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val state = s ?: return
    val p = state.prefs
    val n = state.notifications
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors
    var themeDialog by remember { mutableStateOf(false) }
    var currencyDialog by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf(false) }
    var aiConsent by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.clickable { nameDialog = true }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Surface(shape = CircleShape, color = ext.garden.container, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            val initial = p.displayName.trim().firstOrNull()?.uppercaseChar()
                            if (initial != null) {
                                Text(initial.toString(), style = MaterialTheme.typography.headlineLarge, color = ext.garden.primary)
                            } else {
                                Icon(painterResource(R.drawable.ic_sprout), null, tint = ext.garden.primary, modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                    Column {
                        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                        Text(stringResource(R.string.profile_subtitle), style = MaterialTheme.typography.bodyLarge, color = ext.mutedText)
                    }
                }
            }
            item { NotificationsBlockedBanner() }

            item { SectionLabel(stringResource(R.string.profile_section_modules)) }
            item {
                SettingsCard {
                    ToggleRow(stringResource(R.string.profile_garden), stringResource(R.string.profile_garden_body), p.gardenEnabled, ext.garden.primary, enabled = !(p.gardenEnabled && !p.aquariumEnabled)) { vm.setGarden(it) }
                    Divider()
                    ToggleRow(stringResource(R.string.profile_aquarium), stringResource(R.string.profile_aquarium_body), p.aquariumEnabled, ext.aquarium.primary, enabled = !(p.aquariumEnabled && !p.gardenEnabled)) { vm.setAquarium(it) }
                    Divider()
                    ToggleRow(stringResource(R.string.profile_ai), stringResource(R.string.profile_ai_body), p.aiAssistantEnabled, ext.garden.primary) { on ->
                        if (on) aiConsent = true else vm.setAi(false)
                    }
                    if (p.aiAssistantEnabled && p.gardenEnabled) {
                        Divider()
                        NavRow(stringResource(R.string.assistant_garden_title), null, actions.onGardenAssistant)
                    }
                    if (p.aiAssistantEnabled && p.aquariumEnabled) {
                        Divider()
                        NavRow(stringResource(R.string.assistant_aquarium_title), null, actions.onAquariumAssistant)
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.profile_section_reminders)) }
            item {
                SettingsCard {
                    NavRow(
                        stringResource(R.string.profile_quiet_hours),
                        if (n.quietHoursEnabled) "${f.time(n.quietHoursStart)} – ${f.time(n.quietHoursEnd)}" else stringResource(R.string.profile_quiet_off),
                        actions.onReminders,
                    )
                    Divider()
                    NavRow(stringResource(R.string.profile_default_time), f.time(n.defaultReminderTime), actions.onReminders)
                    Divider()
                    NavRow(stringResource(R.string.profile_reminder_history), null, actions.onReminderHistory)
                }
            }

            item { SectionLabel(stringResource(R.string.profile_section_tools)) }
            item {
                SettingsCard {
                    NavRow(stringResource(R.string.profile_inventory), null, actions.onInventory)
                    Divider()
                    NavRow(stringResource(R.string.profile_analytics), null, actions.onAnalytics)
                    Divider()
                    NavRow(stringResource(R.string.profile_gallery), null, actions.onGallery)
                    Divider()
                    NavRow(stringResource(R.string.profile_search), null, actions.onSearch)
                    if (p.gardenEnabled) {
                        Divider()
                        NavRow(stringResource(R.string.profile_planner), null, actions.onPlanner)
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.profile_section_data)) }
            item {
                SettingsCard {
                    NavRow(stringResource(R.string.profile_backup), null, actions.onBackup)
                    Divider()
                    NavRow(stringResource(R.string.profile_restore), null, actions.onRestore)
                    Divider()
                    NavRow(stringResource(R.string.profile_export_calendar), null, actions.onExportCalendar)
                    Divider()
                    NavRow(stringResource(R.string.profile_theme), stringResource(themeLabel(p.themeMode))) { themeDialog = true }
                    Divider()
                    NavRow(stringResource(R.string.profile_currency), currencyLabel(p.currencyCode)) { currencyDialog = true }
                    Divider()
                    NavRow(stringResource(R.string.profile_privacy), null, actions.onPrivacy)
                }
            }
            item {
                Text(
                    stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.mutedText,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    if (themeDialog) {
        ChoiceDialog(stringResource(R.string.profile_theme), ThemeMode.entries, p.themeMode, { stringResource(themeLabel(it)) }, {
            vm.setTheme(it)
            themeDialog = false
        }, { themeDialog = false })
    }
    if (currencyDialog) {
        ChoiceDialog(stringResource(R.string.profile_currency), CURRENCIES, p.currencyCode, { currencyLabel(it) }, {
            vm.setCurrency(it)
            currencyDialog = false
        }, { currencyDialog = false })
    }
    if (nameDialog) {
        var name by remember { mutableStateOf(p.displayName) }
        AlertDialog(
            onDismissRequest = { nameDialog = false },
            title = { Text(stringResource(R.string.profile_edit_name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.profile_edit_name_body), style = MaterialTheme.typography.bodySmall)
                    AppTextField(name, { name = it }, stringResource(R.string.profile_edit_name), maxLength = 40)
                }
            },
            confirmButton = {
                TextButton({
                    vm.setName(name)
                    nameDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton({ nameDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (aiConsent) {
        AlertDialog(
            onDismissRequest = { aiConsent = false },
            title = { Text(stringResource(R.string.ai_consent_title)) },
            text = { Text(stringResource(R.string.ai_consent_body)) },
            confirmButton = {
                TextButton({
                    vm.setAi(true)
                    aiConsent = false
                }) { Text(stringResource(R.string.ai_consent_turn_on)) }
            },
            dismissButton = { TextButton({ aiConsent = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun themeLabel(m: ThemeMode) = when (m) {
    ThemeMode.SYSTEM -> R.string.profile_theme_system
    ThemeMode.LIGHT -> R.string.profile_theme_light
    ThemeMode.DARK -> R.string.profile_theme_dark
}

fun currencyLabel(code: String): String = runCatching {
    val c = Currency.getInstance(code)
    val symbol = if (code == "INR") "₹" else c.symbol
    if (symbol == code) code else "$symbol $code"
}.getOrDefault(code)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold),
        color = AquaGrowTheme.colors.mutedText,
        modifier = Modifier.padding(top = 16.dp, start = 4.dp).semantics { heading() },
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaGrowTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) { Column(content = content) }
}

@Composable
private fun Divider() = HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, color: Color, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = color, disabledCheckedTrackColor = color.copy(alpha = 0.5f)))
    }
}

@Composable
private fun NavRow(title: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp), modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = MaterialTheme.typography.bodyLarge, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(end = 8.dp))
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = AquaGrowTheme.colors.mutedText)
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { ChoiceChips(options, selected, label, onPick) },
        confirmButton = {},
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
