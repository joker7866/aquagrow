package com.aquagrow.ui.screens.garden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.garden.DueStatus
import com.aquagrow.domain.model.CareModule
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppFilterChip
import com.aquagrow.ui.components.AppPhoto
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.ChipTone
import com.aquagrow.ui.components.EmptyState
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.QuickActionTile
import com.aquagrow.ui.components.StatusChip
import com.aquagrow.ui.components.label
import com.aquagrow.ui.preview.PreviewData
import com.aquagrow.ui.preview.ScreenPreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme

data class GardenActions(
    val onAddPlant: () -> Unit = {},
    val onOpenPlant: (Long) -> Unit = {},
    val onOpenWatering: () -> Unit = {},
    val onOpenFertilizer: () -> Unit = {},
    val onOpenPlanner: () -> Unit = {},
    val onOpenInventory: () -> Unit = {},
)

@Composable
fun GardenScreen(
    actions: GardenActions,
    modifier: Modifier = Modifier,
    viewModel: GardenViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GardenContent(state, actions, viewModel::onQueryChange, viewModel::onCategoryChange, modifier)
}

@Composable
fun GardenContent(
    state: GardenUiState,
    actions: GardenActions,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (com.aquagrow.domain.model.PlantCategory?) -> Unit,
    modifier: Modifier = Modifier,
) = ModuleTheme(CareModule.GARDEN) {
    val module = AquaGrowTheme.module
    var searching by rememberSaveable { mutableStateOf(state.query.isNotEmpty()) }
    Scaffold(
        modifier = modifier,
        containerColor = module.pageBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.totalPlants > 0) {
                ExtendedFloatingActionButton(
                    onClick = actions.onAddPlant,
                    containerColor = module.primary,
                    contentColor = module.onPrimary,
                    icon = { Icon(painterResource(R.drawable.ic_add), null) },
                    text = { Text(stringResource(R.string.garden_add_plant)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.garden_title),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() },
                    )
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, AquaGrowTheme.colors.cardBorder)) {
                        IconButton(
                            onClick = {
                                if (searching) onQueryChange("")
                                searching = !searching
                            },
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                painterResource(if (searching) R.drawable.ic_close else R.drawable.ic_search),
                                stringResource(if (searching) R.string.garden_search_clear else R.string.action_search),
                            )
                        }
                    }
                }
            }
            if (searching) {
                item(key = "search") {
                    AppTextField(state.query, onQueryChange, stringResource(R.string.garden_search_hint))
                }
            }
            if (state.needsWaterCount > 0) {
                item(key = "alert") { WateringAlert(state.needsWaterCount, actions.onOpenWatering) }
            }
            item(key = "tools") {
                Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val c = AquaGrowTheme.colors.garden
                    QuickActionTile(R.drawable.ic_water_drop, stringResource(R.string.garden_tool_watering), c, actions.onOpenWatering, Modifier.weight(1f).fillMaxHeight())
                    QuickActionTile(R.drawable.ic_sprout, stringResource(R.string.garden_tool_fertilizer), c, actions.onOpenFertilizer, Modifier.weight(1f).fillMaxHeight())
                    QuickActionTile(R.drawable.ic_calendar_today, stringResource(R.string.garden_tool_planner), c, actions.onOpenPlanner, Modifier.weight(1f).fillMaxHeight())
                    QuickActionTile(R.drawable.ic_deployed_code, stringResource(R.string.garden_tool_inventory), c, actions.onOpenInventory, Modifier.weight(1f).fillMaxHeight())
                }
            }
            if (state.categories.size > 1) {
                item(key = "chips") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { AppFilterChip(state.category == null, stringResource(R.string.garden_filter_all), { onCategoryChange(null) }, selectedColor = MaterialTheme.colorScheme.onSurface) }
                        items(state.categories, key = { it.name }) { cat ->
                            AppFilterChip(state.category == cat, cat.label(), { onCategoryChange(if (state.category == cat) null else cat) }, selectedColor = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }

                state.totalPlants == 0 -> item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.garden_empty_title),
                        body = stringResource(R.string.garden_empty_body),
                        illustration = {
                            com.aquagrow.ui.components.PhotoPlaceholder(R.drawable.ic_sprout, module, Modifier.size(96.dp), iconSize = 48.dp)
                        },
                    ) {
                        PrimaryButton(stringResource(R.string.garden_add_plant), actions.onAddPlant, icon = R.drawable.ic_add)
                    }
                }

                state.plants.isEmpty() -> item(key = "no-results") {
                    Text(
                        stringResource(R.string.garden_no_results),
                        color = AquaGrowTheme.colors.mutedText,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                else -> items(state.plants, key = { it.plant.id }) { card ->
                    PlantCard(card, onClick = { actions.onOpenPlant(card.plant.id) })
                }
            }
        }
    }
}

/** Mockup: solid green banner; the whole banner opens the Watering tool. */
@Composable
private fun WateringAlert(count: Int, onView: () -> Unit) {
    val g = AquaGrowTheme.colors.garden
    Surface(onClick = onView, shape = MaterialTheme.shapes.large, color = g.primary, contentColor = g.onPrimary) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = g.onPrimary.copy(alpha = 0.15f), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_water_drop), null) }
            }
            Column(Modifier.weight(1f)) {
                Text(pluralStringResource(R.plurals.garden_alert_title, count, count), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.garden_alert_body), style = MaterialTheme.typography.bodyMedium, color = g.onPrimary.copy(alpha = 0.9f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlantCard(card: PlantCardUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = card.plant
    val ext = AquaGrowTheme.colors
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppPhoto(
                path = p.coverPhotoPath,
                placeholderIcon = R.drawable.ic_image,
                colors = ext.garden,
                contentDescription = stringResource(R.string.photo_content_description, p.name),
                modifier = Modifier.size(84.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    listOfNotNull(p.name, p.variety).joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(p.potSize, p.location, stringResource(R.string.garden_day_n, card.dayNumber)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    p.currentStage?.let { StatusChip(it, ChipTone.GARDEN) }
                    card.wateringStatus?.let { DueChip(stringResource(R.string.garden_water_label), it) }
                }
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = ext.mutedText)
        }
    }
}

@Composable
fun DueChip(what: String, status: DueStatus) {
    when (status) {
        is DueStatus.Overdue -> StatusChip(
            pluralStringResource(R.plurals.due_overdue_days, status.days.toInt(), what, status.days.toInt()),
            ChipTone.WARNING,
            icon = R.drawable.ic_water_drop,
        )

        DueStatus.Today -> StatusChip(stringResource(R.string.due_today, what), ChipTone.WARNING, icon = R.drawable.ic_water_drop)

        is DueStatus.Upcoming -> StatusChip(
            if (status.days == 1L) {
                stringResource(R.string.due_tomorrow, what)
            } else {
                pluralStringResource(R.plurals.due_in_days, status.days.toInt(), what, status.days.toInt())
            },
            ChipTone.NEUTRAL,
        )
    }
}

@ScreenPreviews
@Composable
private fun GardenPreview() {
    AquaGrowTheme { GardenContent(PreviewData.garden, GardenActions(), {}, {}) }
}

@ScreenPreviews
@Composable
private fun GardenEmptyPreview() {
    AquaGrowTheme { GardenContent(GardenUiState(loading = false), GardenActions(), {}, {}) }
}
