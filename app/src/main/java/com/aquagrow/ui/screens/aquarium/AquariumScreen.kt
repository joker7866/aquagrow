package com.aquagrow.ui.screens.aquarium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.TaskType
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppFilterChip
import com.aquagrow.ui.components.AppPhoto
import com.aquagrow.ui.components.ChipTone
import com.aquagrow.ui.components.EmptyState
import com.aquagrow.ui.components.IconTile
import com.aquagrow.ui.components.PhotoPlaceholder
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.QuickActionTile
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.StatusChip
import com.aquagrow.ui.components.formatReading
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.preview.PreviewData
import com.aquagrow.ui.preview.ScreenPreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class AquariumActions(
    val onAddAquarium: () -> Unit = {},
    val onOpenTank: (Long) -> Unit = {},
    val onAddFish: () -> Unit = {},
    /** Add a fish straight into this tank. */
    val onAddFishToTank: (Long) -> Unit = {},
    val onEditFish: (Long) -> Unit = {},
    val onOpenFeeding: () -> Unit = {},
    val onOpenWaterTests: () -> Unit = {},
    val onOpenMaintenance: () -> Unit = {},
    val onOpenInventory: () -> Unit = {},
    val onSearch: () -> Unit = {},
)

@Composable
fun AquariumScreen(actions: AquariumActions, viewModel: AquariumViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AquariumContent(state, actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquariumContent(state: AquariumUiState, actions: AquariumActions) = ModuleTheme(CareModule.AQUARIUM) {
    val module = AquaGrowTheme.module
    var addMenu by remember { mutableStateOf(false) }
    // null = all tanks; otherwise the tank picked in the "Your tanks" switcher.
    var selectedTank by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = state.tanks.firstOrNull { it.summary.aquarium.id == selectedTank }
    Scaffold(
        containerColor = module.pageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.tanks.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { addMenu = true },
                    containerColor = module.primary,
                    contentColor = module.onPrimary,
                    icon = { Icon(painterResource(R.drawable.ic_add), null) },
                    text = { Text(stringResource(R.string.aquarium_add_short), style = MaterialTheme.typography.titleMedium) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.aquarium_title),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, AquaGrowTheme.colors.cardBorder)) {
                        IconButton(onClick = actions.onSearch, modifier = Modifier.size(56.dp)) {
                            Icon(painterResource(R.drawable.ic_search), stringResource(R.string.action_search))
                        }
                    }
                }
            }
            item {
                Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(R.drawable.ic_food, stringResource(R.string.aquarium_tool_feeding), module, actions.onOpenFeeding, Modifier.weight(1f).fillMaxHeight())
                    QuickActionTile(R.drawable.ic_experiment, stringResource(R.string.aquarium_tool_water), module, actions.onOpenWaterTests, Modifier.weight(1f).fillMaxHeight())
                    QuickActionTile(R.drawable.ic_autorenew, stringResource(R.string.aquarium_tool_maintain), module, actions.onOpenMaintenance, Modifier.weight(1f).fillMaxHeight())
                    QuickActionTile(R.drawable.ic_deployed_code, stringResource(R.string.aquarium_tool_inventory), module, actions.onOpenInventory, Modifier.weight(1f).fillMaxHeight())
                }
            }
            when {
                state.loading -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }

                state.tanks.isEmpty() -> item {
                    EmptyState(
                        title = stringResource(R.string.aquarium_empty_title),
                        body = stringResource(R.string.aquarium_empty_body),
                        illustration = { PhotoPlaceholder(R.drawable.ic_fish, module, Modifier.size(96.dp), iconSize = 48.dp) },
                    ) { PrimaryButton(stringResource(R.string.aquarium_add_tank), actions.onAddAquarium, icon = R.drawable.ic_add) }
                }

                else -> {
                    item(key = "tank-switcher") {
                        TankSwitcher(
                            tanks = state.tanks,
                            selected = selected?.summary?.aquarium?.id,
                            onSelect = { selectedTank = it },
                            onAddTank = actions.onAddAquarium,
                        )
                    }
                    if (selected != null) {
                        val id = selected.summary.aquarium.id
                        val tankFish = state.fish.filter { it.aquariumId == id }
                        item(key = "selected-$id") { FeaturedTankCard(selected, state.today) { actions.onOpenTank(id) } }
                        item {
                            SectionHeader(
                                stringResource(R.string.aquarium_fish_in_selected, selected.summary.aquarium.name),
                                actionLabel = stringResource(R.string.aquarium_add_fish),
                                onAction = { actions.onAddFishToTank(id) },
                            )
                        }
                        if (tankFish.isEmpty()) {
                            item { Text(stringResource(R.string.aquarium_no_fish_in_tank), color = AquaGrowTheme.colors.mutedText) }
                        }
                        item { FishRow(tankFish, state.tanks, actions.onEditFish) { actions.onAddFishToTank(id) } }
                    } else {
                        itemsIndexed(state.tanks, key = { _, it -> it.summary.aquarium.id }) { index, card ->
                            if (index == 0) {
                                FeaturedTankCard(card, state.today) { actions.onOpenTank(card.summary.aquarium.id) }
                            } else {
                                TankCard(card) { actions.onOpenTank(card.summary.aquarium.id) }
                            }
                        }
                        item { SectionHeader(stringResource(R.string.aquarium_fish_collection), actionLabel = stringResource(R.string.aquarium_add_fish), onAction = actions.onAddFish) }
                        item { FishRow(state.fish, state.tanks, actions.onEditFish, actions.onAddFish) }
                    }
                }
            }
        }
    }
    if (addMenu) {
        ModalBottomSheet(onDismissRequest = { addMenu = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.aquarium_add_tank)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_web_asset), null) },
                    modifier = Modifier.clickable {
                        addMenu = false
                        actions.onAddAquarium()
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.aquarium_add_fish)) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_fish), null) },
                    modifier = Modifier.clickable {
                        addMenu = false
                        actions.onAddFish()
                    },
                )
            }
        }
    }
}

@Composable
private fun statusChip(card: TankCardUi): Pair<String, ChipTone> = when (card.status) {
    TankStatus.OK -> stringResource(R.string.aquarium_all_in_range) to ChipTone.SUCCESS
    TankStatus.CHECK -> stringResource(R.string.tank_status_check) to ChipTone.WARNING
    TankStatus.NO_TEST -> stringResource(R.string.tank_status_no_test) to ChipTone.NEUTRAL
    TankStatus.NO_RANGES -> stringResource(R.string.tank_status_no_ranges) to ChipTone.NEUTRAL
}

@Composable
private fun overdueText(card: TankCardUi): String? {
    val o = card.overdue.firstOrNull() ?: return null
    val what = if (o.task.type == TaskType.WATER_TEST) stringResource(R.string.maint_water_testing) else o.task.maintenanceType?.label() ?: o.task.title
    return stringResource(R.string.aquarium_overdue, what)
}

/** Mockup: first tank as a large card with image header, metric boxes and a footer line. */
@Composable
private fun FeaturedTankCard(card: TankCardUi, today: LocalDate, onClick: () -> Unit) {
    val tank = card.summary.aquarium
    val test = card.summary.latestTest
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, ext.cardBorder)) {
        Column {
            Box(Modifier.fillMaxWidth().height(160.dp)) {
                AppPhoto(tank.coverPhotoPath, R.drawable.ic_image, ext.aquarium, Modifier.fillMaxSize(), stringResource(R.string.photo_content_description, tank.name), shape = RoundedCornerShape(0.dp))
                val overdue = overdueText(card)
                val (text, tone) = if (overdue != null) overdue to ChipTone.WARNING else statusChip(card)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (tone == ChipTone.WARNING) ext.warningContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.labelLarge,
                        color = when (tone) {
                            ChipTone.SUCCESS -> ext.success
                            ChipTone.WARNING -> ext.onWarningContainer
                            else -> ext.mutedText
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tank.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(tank.volumeLiters?.let { stringResource(R.string.aquarium_volume, formatReading(it)) }, tank.tankType.label()).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.mutedText,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricBox(stringResource(R.string.aquarium_metric_temp), test?.temperatureC?.let { "${formatReading(it)} °C" } ?: "—", Modifier.weight(1f))
                    MetricBox(stringResource(R.string.aquarium_metric_ph), test?.ph?.let(::formatReading) ?: "—", Modifier.weight(1f))
                    MetricBox(stringResource(R.string.aquarium_metric_fish), card.summary.fishCount.toString(), Modifier.weight(1f))
                }
                val footer = listOfNotNull(
                    card.nextFeeding?.let { stringResource(R.string.aquarium_next_feeding, f.time(it)) },
                    test?.let {
                        val days = ChronoUnit.DAYS.between(it.testedAt.atZone(ZoneId.systemDefault()).toLocalDate(), today).toInt()
                        when (days) {
                            0 -> stringResource(R.string.aquarium_tested_today)
                            1 -> stringResource(R.string.aquarium_tested_yesterday)
                            else -> pluralStringResource(R.plurals.aquarium_tested_days_ago, days, days)
                        }
                    },
                )
                if (footer.isNotEmpty()) Text(footer.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = ext.mutedText)
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = AquaGrowTheme.colors.aquarium.pageBackground, modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Mockup: compact tank row with image, details, one chip and a chevron. */
@Composable
fun TankCard(card: TankCardUi, onClick: () -> Unit) {
    val tank = card.summary.aquarium
    val ext = AquaGrowTheme.colors
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = 16.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppPhoto(tank.coverPhotoPath, R.drawable.ic_image, ext.aquarium, Modifier.size(84.dp), stringResource(R.string.photo_content_description, tank.name))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tank.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        tank.volumeLiters?.let { stringResource(R.string.aquarium_volume, formatReading(it)) },
                        pluralStringResource(R.plurals.aquarium_fish_count, card.summary.fishCount, card.summary.fishCount),
                        stringResource(R.string.aquarium_heated).takeIf { !tank.heater.isNullOrBlank() && !tank.heater.equals("none", ignoreCase = true) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.mutedText,
                )
                val overdue = overdueText(card)
                if (overdue != null) {
                    StatusChip(overdue, ChipTone.WARNING)
                } else {
                    val (text, tone) = statusChip(card)
                    StatusChip(text, tone)
                }
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = ext.mutedText)
        }
    }
}

/** "Your tanks": All tanks, then one button per tank (Tank 1, Tank 2, …). Add tank stays visible in the header. */
@Composable
private fun TankSwitcher(tanks: List<TankCardUi>, selected: Long?, onSelect: (Long?) -> Unit, onAddTank: () -> Unit) {
    val ext = AquaGrowTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.aquarium_tanks_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = onAddTank, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(painterResource(R.drawable.ic_add), null, Modifier.size(18.dp), tint = AquaGrowTheme.module.primary)
                Text(stringResource(R.string.aquarium_add_tank_chip), color = AquaGrowTheme.module.primary, modifier = Modifier.padding(start = 6.dp))
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "all") {
                AppFilterChip(selected == null, stringResource(R.string.aquarium_all_tanks, tanks.size), { onSelect(null) }, selectedColor = MaterialTheme.colorScheme.onSurface)
            }
            items(tanks, key = { it.summary.aquarium.id }) { card ->
                val id = card.summary.aquarium.id
                val dot = when (card.status) {
                    TankStatus.CHECK -> ext.onWarningContainer
                    TankStatus.OK -> ext.success
                    else -> null
                }
                AppFilterChip(selected == id, card.summary.aquarium.name, { onSelect(id) }, leadingDot = dot)
            }
        }
    }
}

@Composable
private fun FishRow(fish: List<Fish>, tanks: List<TankCardUi>, onEdit: (Long) -> Unit, onAdd: () -> Unit) {
    val ext = AquaGrowTheme.colors
    val tankNames = tanks.associate { it.summary.aquarium.id to it.summary.aquarium.name }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(fish, key = { it.id }) { f ->
            AppCard(Modifier.width(150.dp), onClick = { onEdit(f.id) }, contentPadding = 16.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (f.photoPath != null) {
                        AppPhoto(f.photoPath, R.drawable.ic_fish, ext.aquarium, Modifier.size(52.dp), shape = CircleShape)
                    } else {
                        Surface(shape = CircleShape, color = ext.aquarium.container, modifier = Modifier.size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_fish), null, tint = ext.aquarium.primary) }
                        }
                    }
                    Text(f.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.aquarium_fish_in_tank, f.count, tankNames[f.aquariumId].orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item {
            AppCard(Modifier.width(150.dp), onClick = onAdd, contentPadding = 16.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconTile(R.drawable.ic_add, ext.aquarium, size = 52.dp)
                    Text(stringResource(R.string.aquarium_add_fish), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@ScreenPreviews
@Composable
private fun AquariumPreview() {
    AquaGrowTheme { AquariumContent(PreviewData.aquarium, AquariumActions()) }
}

@ScreenPreviews
@Composable
private fun AquariumEmptyPreview() {
    AquaGrowTheme { AquariumContent(AquariumUiState(loading = false), AquariumActions()) }
}
