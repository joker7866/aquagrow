package com.aquagrow.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.EmptyState
import com.aquagrow.ui.components.FilledStatCard
import com.aquagrow.ui.components.PotAndTankIllustration
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.QuickActionTile
import com.aquagrow.ui.components.SecondaryButton
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.TaskRow
import com.aquagrow.ui.components.TipCard
import com.aquagrow.ui.components.displayTitle
import com.aquagrow.ui.components.homeMeta
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.components.taskIcon
import com.aquagrow.ui.preview.PreviewData
import com.aquagrow.ui.preview.ScreenPreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleColors
import com.aquagrow.ui.theme.ModuleTheme
import java.time.LocalDate

/** Navigation callbacks from Home. */
data class HomeActions(
    val onAddPlant: () -> Unit = {},
    val onAddFish: () -> Unit = {},
    val onAddTask: () -> Unit = {},
    val onLogWatering: () -> Unit = {},
    val onLogFeeding: () -> Unit = {},
    val onAddAquarium: () -> Unit = {},
    val onOpenGarden: () -> Unit = {},
    val onOpenAquarium: () -> Unit = {},
    val onOpenCalendar: () -> Unit = {},
    val onOpenReminders: () -> Unit = {},
)

@Composable
fun HomeScreen(
    actions: HomeActions,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state = state, actions = actions, onToggleTask = viewModel::onToggleTask, modifier = modifier)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    actions: HomeActions,
    onToggleTask: (TaskOccurrence, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state) {
            HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is HomeUiState.Empty -> HomeEmptyContent(state, actions)
            is HomeUiState.Dashboard -> DashboardContent(state, actions, onToggleTask)
        }
    }
}

@Composable
private fun Header(date: LocalDate, title: String, onBell: (() -> Unit)?) {
    val f = rememberFormatters()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(f.fullDate.format(date), style = MaterialTheme.typography.bodyLarge, color = AquaGrowTheme.colors.mutedText)
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        }
        if (onBell != null) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, AquaGrowTheme.colors.cardBorder)) {
                IconButton(onClick = onBell, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.ic_notifications), stringResource(R.string.home_notifications))
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(state: HomeUiState.Dashboard, actions: HomeActions, onToggleTask: (TaskOccurrence, Boolean) -> Unit) {
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            Header(
                date = state.today,
                title = stringResource(
                    when (state.greeting) {
                        Greeting.MORNING -> R.string.home_greeting_morning
                        Greeting.AFTERNOON -> R.string.home_greeting_afternoon
                        Greeting.EVENING -> R.string.home_greeting_evening
                    },
                ),
                onBell = actions.onOpenReminders,
            )
        }

        if (state.gardenEnabled || state.aquariumEnabled) {
            item(key = "stats") {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (state.gardenEnabled) {
                        FilledStatCard(
                            label = stringResource(R.string.home_stat_plants),
                            value = state.plantCount.toString(),
                            supporting = pluralStringResource(R.plurals.home_growing, state.growingCount, state.growingCount),
                            colors = ext.garden,
                            onClick = actions.onOpenGarden,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (state.aquariumEnabled) {
                        FilledStatCard(
                            label = stringResource(R.string.home_stat_fish),
                            value = state.fishCount.toString(),
                            supporting = pluralStringResource(R.plurals.home_in_aquariums, state.tankCount, state.tankCount),
                            colors = ext.aquarium,
                            onClick = actions.onOpenAquarium,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }

        item(key = "today-header") {
            SectionHeader(
                title = stringResource(R.string.home_today),
                actionLabel = stringResource(R.string.home_calendar_link),
                onAction = actions.onOpenCalendar,
            )
        }
        item(key = "today") {
            AppCard(contentPadding = 16.dp) {
                if (state.tasks.isEmpty()) {
                    Text(stringResource(R.string.home_today_none), style = MaterialTheme.typography.bodyLarge, color = ext.mutedText, modifier = Modifier.padding(8.dp))
                } else {
                    state.tasks.forEachIndexed { index, occ ->
                        if (index > 0) HorizontalDivider(color = ext.cardBorder)
                        TaskRow(
                            icon = taskIcon(occ.task.type, occ.task.maintenanceType),
                            title = occ.task.displayTitle(),
                            meta = occ.homeMeta(state.today, f),
                            done = occ.status == OccurrenceStatus.DONE,
                            onToggleDone = { onToggleTask(occ, it) },
                            module = occ.task.module,
                            metaIsWarning = occ.isOverdue(state.today),
                        )
                    }
                    if (state.tasksLeft == 0) {
                        Text(
                            stringResource(R.string.home_today_all_done),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.success,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }
                }
            }
        }

        item(key = "week") { WeeklyCareCard(state) }
        item(key = "quick-header") { SectionHeader(stringResource(R.string.home_quick_add)) }
        item(key = "quick") { QuickAddGrid(state.gardenEnabled, state.aquariumEnabled, actions) }
    }
}

@Composable
private fun QuickAddGrid(garden: Boolean, aquarium: Boolean, actions: HomeActions) {
    val ext = AquaGrowTheme.colors
    data class Tile(val icon: Int, val label: Int, val colors: ModuleColors, val onClick: () -> Unit)
    val tiles = buildList {
        if (garden) add(Tile(R.drawable.ic_sprout, R.string.quick_add_plant, ext.garden, actions.onAddPlant))
        if (aquarium) add(Tile(R.drawable.ic_fish, R.string.quick_add_fish, ext.aquarium, actions.onAddFish))
        add(Tile(R.drawable.ic_calendar_today, R.string.quick_add_task, ext.garden, actions.onAddTask))
        if (garden) add(Tile(R.drawable.ic_water_drop, R.string.quick_log_watering, ext.garden, actions.onLogWatering))
        if (aquarium) add(Tile(R.drawable.ic_food, R.string.quick_log_feeding, ext.aquarium, actions.onLogFeeding))
        if (aquarium) add(Tile(R.drawable.ic_web_asset, R.string.quick_add_aquarium, ext.aquarium, actions.onAddAquarium))
    }
    // Two columns at large font sizes so labels like "Log watering" fit without truncating.
    val columns = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 2 else 3
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { t -> QuickActionTile(t.icon, stringResource(t.label), t.colors, t.onClick, Modifier.weight(1f), iconTile = false) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HomeEmptyContent(state: HomeUiState.Empty, actions: HomeActions) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPadding + 4.dp, vertical = 16.dp),
    ) {
        Header(date = LocalDate.now(), title = stringResource(R.string.home_hello), onBell = null)
        Spacer(Modifier.height(48.dp))
        val body = when {
            state.gardenEnabled && !state.aquariumEnabled -> R.string.home_empty_body_garden
            state.aquariumEnabled && !state.gardenEnabled -> R.string.home_empty_body_aquarium
            else -> R.string.home_empty_body
        }
        EmptyState(
            title = stringResource(R.string.home_empty_title),
            body = stringResource(body),
            illustration = { PotAndTankIllustration() },
        ) {
            if (state.gardenEnabled) {
                PrimaryButton(stringResource(R.string.home_empty_add_plant), actions.onAddPlant, icon = R.drawable.ic_add)
            }
            if (state.aquariumEnabled) {
                ModuleTheme(CareModule.AQUARIUM) {
                    if (state.gardenEnabled) {
                        SecondaryButton(stringResource(R.string.home_empty_add_aquarium), actions.onAddAquarium, icon = R.drawable.ic_add, emphasized = true)
                    } else {
                        PrimaryButton(stringResource(R.string.home_empty_add_aquarium), actions.onAddAquarium, icon = R.drawable.ic_add)
                    }
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        TipCard(title = null, body = stringResource(R.string.home_tip_body))
    }
}

@ScreenPreviews
@Composable
private fun HomeDashboardPreview() {
    AquaGrowTheme { HomeContent(PreviewData.homeDashboard, HomeActions(), { _, _ -> }) }
}

@ScreenPreviews
@Composable
private fun HomeEmptyPreview() {
    AquaGrowTheme { HomeContent(HomeUiState.Empty(gardenEnabled = true, aquariumEnabled = true), HomeActions(), { _, _ -> }) }
}
