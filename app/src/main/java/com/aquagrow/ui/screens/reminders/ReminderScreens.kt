package com.aquagrow.ui.screens.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aquagrow.R
import com.aquagrow.domain.model.NotificationSettings
import com.aquagrow.domain.model.ReminderKind
import com.aquagrow.domain.model.ReminderLogEntry
import com.aquagrow.domain.reminders.ReminderChannel
import com.aquagrow.domain.repository.ReminderLogRepository
import com.aquagrow.domain.repository.SettingsRepository
import com.aquagrow.notifications.ReminderNotifier
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.ConfirmDialog
import com.aquagrow.ui.components.IconTile
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.SwitchRow
import com.aquagrow.ui.components.TimeField
import com.aquagrow.ui.components.TipCard
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<NotificationSettings?> = settings.notificationSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(transform: (NotificationSettings) -> NotificationSettings) = viewModelScope.launch { settings.updateNotificationSettings(transform) }
}

@Composable
fun NotificationsBlockedBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(context.notificationsAllowed()) }
    // Re-check when the user comes back from system settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = context.notificationsAllowed() }
    if (allowed) return
    val ext = AquaGrowTheme.colors
    Surface(
        onClick = { context.openAppNotificationSettings() },
        shape = MaterialTheme.shapes.large,
        color = ext.warningContainer,
        contentColor = ext.onWarningContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_notifications), null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.perm_denied_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.perm_denied_body), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ReminderSettingsScreen(onBack: () -> Unit, onOpenHistory: () -> Unit, vm: ReminderSettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { AppTopBar(stringResource(R.string.reminders_title), onBack) }) { padding ->
        val settings = s ?: return@Scaffold
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { NotificationsBlockedBanner() }
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    SwitchRow(stringResource(R.string.reminders_enabled), settings.remindersEnabled, { on -> vm.update { it.copy(remindersEnabled = on) } }, supporting = stringResource(R.string.reminders_enabled_body))
                    HorizontalDivider(color = AquaGrowTheme.colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                    Text(stringResource(R.string.reminders_default_time_body), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                    TimeField(stringResource(R.string.reminders_default_time), settings.defaultReminderTime, { t -> t?.let { vm.update { s -> s.copy(defaultReminderTime = it) } } })
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    SwitchRow(stringResource(R.string.reminders_quiet), settings.quietHoursEnabled, { on -> vm.update { it.copy(quietHoursEnabled = on) } }, supporting = stringResource(R.string.reminders_quiet_body))
                    if (settings.quietHoursEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            TimeField(stringResource(R.string.reminders_quiet_from), settings.quietHoursStart, { t -> t?.let { vm.update { s -> s.copy(quietHoursStart = it) } } }, Modifier.weight(1f))
                            TimeField(stringResource(R.string.reminders_quiet_to), settings.quietHoursEnd, { t -> t?.let { vm.update { s -> s.copy(quietHoursEnd = it) } } }, Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.reminders_types)) }
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    ReminderChannel.userToggleable.forEach { ch ->
                        SwitchRow(
                            stringResource(ReminderNotifier.channelName(ch)),
                            ch.name !in settings.disabledChannels,
                            { on -> vm.update { it.copy(disabledChannels = if (on) it.disabledChannels - ch.name else it.disabledChannels + ch.name) } },
                            enabled = settings.remindersEnabled,
                        )
                    }
                    HorizontalDivider(color = AquaGrowTheme.colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow(stringResource(R.string.reminders_missed), settings.missedRemindersEnabled, { on -> vm.update { it.copy(missedRemindersEnabled = on) } }, supporting = stringResource(R.string.reminders_missed_body), enabled = settings.remindersEnabled)
                    SwitchRow(stringResource(R.string.reminders_inventory), settings.inventoryRemindersEnabled, { on -> vm.update { it.copy(inventoryRemindersEnabled = on) } }, supporting = stringResource(R.string.reminders_inventory_body), enabled = settings.remindersEnabled)
                }
            }
            item { TipCard(null, stringResource(R.string.reminders_timing_note)) }
            item {
                AppCard(Modifier.fillMaxWidth(), onClick = onOpenHistory) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.reminders_history), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = AquaGrowTheme.colors.mutedText)
                    }
                }
            }
        }
    }
}

@HiltViewModel
class ReminderHistoryViewModel @Inject constructor(private val log: ReminderLogRepository) : ViewModel() {
    val entries: StateFlow<List<ReminderLogEntry>?> = log.observeRecent(200).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun clear() = viewModelScope.launch { log.clear() }
}

@Composable
fun ReminderHistoryScreen(onBack: () -> Unit, vm: ReminderHistoryViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    var confirm by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(stringResource(R.string.reminders_history), onBack) {
                if (!entries.isNullOrEmpty()) {
                    IconButton(onClick = { confirm = true }) { Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.reminders_history_clear)) }
                }
            }
        },
    ) { padding ->
        val list = entries ?: return@Scaffold
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { NotificationsBlockedBanner() }
            if (list.isEmpty()) item { Text(stringResource(R.string.reminders_history_empty), color = AquaGrowTheme.colors.mutedText) }
            items(list, key = { it.id }) { e ->
                AppCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(
                            when (e.kind) {
                                ReminderKind.LOW_STOCK, ReminderKind.EXPIRY -> R.drawable.ic_deployed_code
                                ReminderKind.MISSED -> R.drawable.ic_warning
                                else -> R.drawable.ic_notifications
                            },
                            AquaGrowTheme.colors.garden,
                            size = 40.dp,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.titleSmall)
                            Text(e.text, style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                        }
                        val dt = e.postedAt.atZone(zone)
                        Text("${f.dayMonth.format(dt)}\n${f.time(dt.toLocalTime())}", style = MaterialTheme.typography.labelSmall, color = AquaGrowTheme.colors.mutedText)
                    }
                }
            }
        }
    }
    if (confirm) {
        ConfirmDialog(
            stringResource(R.string.reminders_history_clear),
            stringResource(R.string.reminders_history_clear),
            stringResource(R.string.action_delete),
            {
                confirm = false
                vm.clear()
            },
            { confirm = false },
            destructive = true,
        )
    }
}
