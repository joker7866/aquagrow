package com.aquagrow.ui.screens.plantdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.catalog.HarvestKind
import com.aquagrow.domain.garden.DueStatus
import com.aquagrow.domain.garden.HarvestEstimate
import com.aquagrow.domain.garden.HarvestPhase
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.Photo
import com.aquagrow.domain.model.Plant
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppPhoto
import com.aquagrow.ui.components.ChipTone
import com.aquagrow.ui.components.ConfirmDialog
import com.aquagrow.ui.components.IconTile
import com.aquagrow.ui.components.PhotoField
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.SecondaryButton
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.SelectionBadge
import com.aquagrow.ui.components.StatusChip
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.components.scheduleSummary
import com.aquagrow.ui.components.taskIcon
import com.aquagrow.ui.components.tone
import com.aquagrow.ui.screens.garden.DueChip
import com.aquagrow.ui.screens.garden.FertilizerLogSheet
import com.aquagrow.ui.screens.garden.FormSheet
import com.aquagrow.ui.screens.garden.GrowthLogSheet
import com.aquagrow.ui.screens.garden.HarvestLogSheet
import com.aquagrow.ui.screens.garden.RecordWateringSheet
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private enum class PlantTab(val label: Int) {
    OVERVIEW(R.string.plant_tab_overview),
    TIMELINE(R.string.plant_tab_timeline),
    PHOTOS(R.string.plant_tab_photos),
    HARVEST(R.string.plant_tab_harvest),
}

private enum class Sheet { WATERING, GROWTH, FERTILIZER, HARVEST, PHOTO }

@Composable
fun PlantDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAskAssistant: (Long) -> Unit = {},
    viewModel: PlantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    LaunchedEffect(deleted) { if (deleted) onBack() }

    ModuleTheme(CareModule.GARDEN) {
        when (val s = state) {
            PlantDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            PlantDetailUiState.NotFound -> Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.plant_not_found))
                TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            }

            is PlantDetailUiState.Ready -> PlantDetailContent(s, viewModel, onBack, onEdit, onAskAssistant)
        }
    }
}

@Composable
private fun PlantDetailContent(
    state: PlantDetailUiState.Ready,
    vm: PlantDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAskAssistant: (Long) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(PlantTab.OVERVIEW) }
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    val plant = state.plant

    Scaffold(
        containerColor = AquaGrowTheme.module.pageBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
                    Row(
                        Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (tab) {
                            PlantTab.OVERVIEW -> {
                                SecondaryButton(stringResource(R.string.plant_add_growth_log), { sheet = Sheet.GROWTH }, Modifier.weight(1f), emphasized = true)
                                PrimaryButton(stringResource(R.string.plant_record_watering), { sheet = Sheet.WATERING }, Modifier.weight(1f))
                            }

                            PlantTab.TIMELINE -> {
                                SecondaryButton(stringResource(R.string.plant_log_fertilizer), { sheet = Sheet.FERTILIZER }, Modifier.weight(1f))
                                PrimaryButton(stringResource(R.string.plant_record_watering), { sheet = Sheet.WATERING }, Modifier.weight(1f))
                            }

                            PlantTab.PHOTOS -> PrimaryButton(stringResource(R.string.plant_add_photo), { sheet = Sheet.PHOTO }, icon = R.drawable.ic_add_photo_alternate)

                            PlantTab.HARVEST -> PrimaryButton(stringResource(R.string.plant_log_harvest), { sheet = Sheet.HARVEST }, icon = R.drawable.ic_add)
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "header") {
                Header(
                    plant = plant,
                    onBack = onBack,
                    onAddPhoto = { sheet = Sheet.PHOTO },
                    onEdit = { onEdit(plant.id) },
                    onAskAssistant = { onAskAssistant(plant.id) },
                    onArchive = { confirmArchive = true },
                    onDelete = { confirmDelete = true },
                )
            }
            item(key = "title") { TitleBlock(state) }
            item(key = "tabs") {
                PrimaryTabRow(
                    selectedTabIndex = tab.ordinal,
                    containerColor = AquaGrowTheme.module.pageBackground,
                    contentColor = AquaGrowTheme.module.primary,
                ) {
                    PlantTab.entries.forEach { t ->
                        Tab(
                            selected = tab == t,
                            onClick = { tab = t },
                            text = { Text(stringResource(t.label), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selectedContentColor = AquaGrowTheme.module.primary,
                            unselectedContentColor = AquaGrowTheme.colors.mutedText,
                        )
                    }
                }
            }
            when (tab) {
                PlantTab.OVERVIEW -> item(key = "overview") { OverviewTab(state, vm::onSetStage, onEditStages = { onEdit(plant.id) }) }
                PlantTab.TIMELINE -> timelineItems(state)
                PlantTab.PHOTOS -> item(key = "photos") { PhotosTab(state.photos, plant, vm::onSetCover, vm::onDeletePhoto) }
                PlantTab.HARVEST -> item(key = "harvest") { HarvestTab(state) }
            }
        }
    }

    when (sheet) {
        Sheet.WATERING -> RecordWateringSheet(plant.name, {
            vm.onRecordWatering(it)
            sheet = null
        }, { sheet = null })

        Sheet.GROWTH -> GrowthLogSheet(plant, {
            vm.onAddGrowthLog(it)
            sheet = null
        }, { sheet = null })

        Sheet.FERTILIZER -> FertilizerLogSheet(plant.id, null, {
            vm.onAddFertilizer(it)
            sheet = null
        }, { sheet = null })

        Sheet.HARVEST -> HarvestLogSheet({
            vm.onAddHarvest(it)
            sheet = null
        }, { sheet = null })

        Sheet.PHOTO -> AddPhotoSheet(onSave = {
            vm.onAddPhoto(it)
            sheet = null
        }, onDismiss = { sheet = null })

        null -> Unit
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.plant_delete_title),
            body = stringResource(R.string.plant_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                vm.onDelete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
    if (confirmArchive) {
        ConfirmDialog(
            title = stringResource(R.string.plant_menu_archive),
            body = stringResource(R.string.plant_archive_body),
            confirmLabel = stringResource(R.string.plant_menu_archive),
            onConfirm = {
                confirmArchive = false
                vm.onArchive()
            },
            onDismiss = { confirmArchive = false },
        )
    }
}

@Composable
private fun AddPhotoSheet(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var path by rememberSaveable { mutableStateOf<String?>(null) }
    FormSheet(
        title = stringResource(R.string.plant_add_photo),
        confirmLabel = stringResource(R.string.action_save),
        confirmEnabled = path != null,
        onConfirm = { path?.let(onSave) },
        onDismiss = onDismiss,
    ) {
        PhotoField(path, { path = it }, R.drawable.ic_sprout, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Header(
    plant: Plant,
    onBack: () -> Unit,
    onAddPhoto: () -> Unit,
    onEdit: () -> Unit,
    onAskAssistant: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        AppPhoto(
            path = plant.coverPhotoPath,
            placeholderIcon = R.drawable.ic_image,
            colors = AquaGrowTheme.module,
            contentDescription = stringResource(R.string.photo_content_description, plant.name),
            modifier = Modifier.fillMaxSize(),
            shape = androidx.compose.ui.graphics.RectangleShape,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(R.drawable.ic_chevron_left, stringResource(R.string.action_back), onBack)
            Spacer(Modifier.weight(1f))
            CircleIconButton(R.drawable.ic_photo_camera, stringResource(R.string.plant_add_photo), onAddPhoto)
            Spacer(Modifier.width(8.dp))
            Box {
                CircleIconButton(R.drawable.ic_more_vert, stringResource(R.string.action_more_options)) { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.plant_menu_edit)) }, onClick = {
                        menu = false
                        onEdit()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.assistant_ask_menu)) }, onClick = {
                        menu = false
                        onAskAssistant()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.plant_menu_archive)) }, onClick = {
                        menu = false
                        onArchive()
                    })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.plant_menu_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(icon: Int, description: String, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
        IconButton(onClick = onClick) { Icon(painterResource(icon), description) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleBlock(state: PlantDetailUiState.Ready) {
    val plant = state.plant
    Column(Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            listOfNotNull(plant.name, plant.variety).joinToString(" · "),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(plant.category.singularLabel(), ChipTone.GARDEN)
            plant.condition?.let { StatusChip(it.label(), it.tone()) }
            OutlinedChip(stringResource(R.string.garden_day_n, plant.dayNumber(state.today)))
        }
    }
}

@Composable
private fun OutlinedChip(text: String) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.ChipRadius - 2.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AquaGrowTheme.colors.cardBorder),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun com.aquagrow.domain.model.PlantCategory.singularLabel(): String = label().removeSuffix("s")

@Composable
private fun OverviewTab(state: PlantDetailUiState.Ready, onSetStage: (Int) -> Unit, onEditStages: () -> Unit) {
    val plant = state.plant
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors
    val zone = ZoneId.systemDefault()
    Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (plant.growthStages.isNotEmpty()) {
            AppCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.plant_growth_stage), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onEditStages) { Text(stringResource(R.string.plant_edit_stages), style = MaterialTheme.typography.labelLarge) }
                }
                Spacer(Modifier.height(12.dp))
                StageStepper(plant.growthStages, plant.currentStageIndex, onSetStage)
                Text(
                    stringResource(R.string.plant_stage_estimate_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.mutedText,
                    modifier = Modifier.padding(top = 8.dp),
                )
                state.harvestEstimate?.let { est ->
                    Spacer(Modifier.height(16.dp))
                    Surface(shape = MaterialTheme.shapes.medium, color = ext.garden.surfaceAlt, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            val kind = state.catalog?.harvestKind ?: HarvestKind.FIRST_HARVEST
                            val days = est.daysToEarliest.toInt()
                            Text(
                                when {
                                    est.phase != HarvestPhase.UPCOMING -> stringResource(R.string.plant_harvest_now)
                                    kind == HarvestKind.FIRST_FLOWERS -> pluralStringResource(R.plurals.plant_flowers_about, days, days)
                                    kind == HarvestKind.LEAVES -> pluralStringResource(R.plurals.plant_picking_about, days, days)
                                    else -> pluralStringResource(R.plurals.plant_harvest_about, days, days)
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(stringResource(R.string.plant_estimate_note), style = MaterialTheme.typography.bodyMedium, color = ext.mutedText)
                            Text(
                                "${f.dayMonth.format(est.earliest)} – ${f.dayMonthYear.format(est.latest)} · ${stringResource(R.string.chip_estimate)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.mutedText,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        val info = buildList {
            add(stringResource(R.string.plant_info_planted) to f.date(plant.plantingDate))
            plant.potSize?.let { add(stringResource(R.string.plant_info_container) to it) }
            plant.sunlight?.let { add(stringResource(R.string.plant_info_sunlight) to it.label()) }
            plant.location?.let { add(stringResource(R.string.plant_info_location) to it) }
            plant.soilMix?.let { add(stringResource(R.string.plant_info_soil) to it) }
            plant.sowingDate?.let { add(stringResource(R.string.plant_info_sown) to f.date(it)) }
            plant.plantingMethod?.let { add(stringResource(R.string.plant_info_method) to it.label()) }
        }
        info.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                row.forEach { (label, value) ->
                    AppCard(Modifier.weight(1f).fillMaxHeight(), contentPadding = 16.dp) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = ext.mutedText)
                        Text(value, style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        SectionHeader(stringResource(R.string.plant_care_schedule))
        AppCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            if (state.schedules.isEmpty()) {
                Text(stringResource(R.string.plant_care_none), style = MaterialTheme.typography.bodyMedium, color = ext.mutedText)
            }
            val lastWatered = state.timeline.filterIsInstance<TimelineEntry.Watering>().filter { it.log.watered }.maxByOrNull { it.at }?.at
            val lastFed = state.timeline.filterIsInstance<TimelineEntry.Fertilizer>().maxByOrNull { it.at }?.at
            state.schedules.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(color = ext.cardBorder)
                val isWater = s.task.type == com.aquagrow.domain.model.TaskType.WATERING
                val name = when {
                    isWater -> stringResource(R.string.plant_watering_label)
                    s.task.type == com.aquagrow.domain.model.TaskType.FERTILIZING -> s.task.itemName ?: s.task.fertilizerType?.label() ?: stringResource(R.string.plant_fertilizer_label)
                    else -> s.task.title
                }
                val nextText = s.next?.let { occ ->
                    val day = if (occ.date == state.today) stringResource(R.string.plant_next_today) else f.dayMonth.format(occ.date)
                    stringResource(R.string.plant_next, listOfNotNull(day, occ.task.reminderTime?.let(f::time)).joinToString(", "))
                }
                val last = (if (isWater) lastWatered else lastFed)?.let { at ->
                    val d = f.dayMonth.format(at.atZone(zone))
                    if (isWater) stringResource(R.string.plant_last_watered, d) else stringResource(R.string.plant_last_on, d)
                }
                Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconTile(if (isWater) R.drawable.ic_water_drop else R.drawable.ic_sprout, if (isWater) ext.garden else ext.garden.copy(container = ext.garden.surfaceAlt), size = 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text("$name · ${s.task.scheduleSummary().replaceFirstChar { it.lowercase() }}", style = MaterialTheme.typography.titleMedium)
                        Text(listOfNotNull(nextText, last).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = ext.mutedText)
                    }
                }
            }
        }

        plant.notes?.let {
            SectionHeader(stringResource(R.string.plant_notes))
            AppCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
        state.catalog?.let { cat ->
            AppCard(Modifier.fillMaxWidth(), color = AquaGrowTheme.module.surfaceAlt) {
                Text(stringResource(R.string.plant_care_tips), style = MaterialTheme.typography.titleMedium)
                cat.careTips.forEach { tip -> Text("• $tip", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
    }
}

/** Short labels like the mockup stepper ("Germ.", "Veg.", "Flower"). */
private fun shortStage(name: String): String = when (name.lowercase()) {
    "germination" -> "Germ."
    "vegetative" -> "Veg."
    "flowering" -> "Flower"
    "fruiting" -> "Fruit"
    "leafy growth" -> "Leafy"
    "tuber forming" -> "Tubers"
    "bulb forming" -> "Bulbs"
    "establishing" -> "Estab."
    else -> if (name.length > 9) name.take(6) + "." else name
}

@Composable
private fun StageStepper(stages: List<String>, current: Int, onSelect: (Int) -> Unit) {
    val module = AquaGrowTheme.module
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.Top,
    ) {
        stages.forEachIndexed { index, stage ->
            val reached = index <= current
            val description = stringResource(R.string.plant_stage_set, stage)
            Column(
                Modifier
                    .width(64.dp)
                    .clickable(role = Role.RadioButton, onClick = { onSelect(index) })
                    .semantics {
                        selected = index == current
                        contentDescription = description
                    }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (index == 0) {
                                    Color.Transparent
                                } else if (reached) {
                                    module.primary
                                } else {
                                    module.container
                                },
                            ),
                    )
                    // Fixed 28dp slot so every label lines up whatever the dot size.
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = if (reached) module.primary else MaterialTheme.colorScheme.surface,
                            border = if (reached) null else BorderStroke(2.dp, module.accent),
                            modifier = Modifier.size(if (index == current) 28.dp else 20.dp),
                        ) {
                            if (index < current) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.ic_check), null, tint = module.onPrimary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (index == stages.lastIndex) {
                                    Color.Transparent
                                } else if (index < current) {
                                    module.primary
                                } else {
                                    module.container
                                },
                            ),
                    )
                }
                Text(
                    shortStage(stage),
                    style = if (index == current) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                    color = if (index == current) module.primary else AquaGrowTheme.colors.mutedText,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun HarvestEstimateCard(estimate: HarvestEstimate, kind: HarvestKind) {
    val f = rememberFormatters()
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    when (kind) {
                        HarvestKind.FIRST_FLOWERS -> R.string.plant_flowering_estimate
                        HarvestKind.LEAVES -> R.string.plant_leaves_estimate
                        else -> R.string.plant_harvest_estimate
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            StatusChip(stringResource(R.string.chip_estimate), ChipTone.NEUTRAL)
        }
        Text(
            stringResource(R.string.plant_harvest_window, f.dayMonth.format(estimate.earliest), f.dayMonthYear.format(estimate.latest)),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            when (estimate.phase) {
                HarvestPhase.UPCOMING -> pluralStringResource(R.plurals.plant_harvest_in_days, estimate.daysToEarliest.toInt(), estimate.daysToEarliest.toInt())
                HarvestPhase.IN_WINDOW -> stringResource(R.string.plant_harvest_in_window)
                HarvestPhase.PAST_WINDOW -> stringResource(R.string.plant_harvest_past)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { estimate.progress },
            color = AquaGrowTheme.module.primary,
            trackColor = AquaGrowTheme.module.container,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
        Text(stringResource(R.string.plant_harvest_disclaimer), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
    }
}

@Composable
fun InfoGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = AquaGrowTheme.colors.mutedText)
                        Text(value, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.timelineItems(state: PlantDetailUiState.Ready) {
    if (state.timeline.isEmpty()) {
        item(key = "timeline-empty") {
            Text(
                stringResource(R.string.plant_timeline_empty),
                color = AquaGrowTheme.colors.mutedText,
                modifier = Modifier.padding(Dimens.ScreenPadding),
            )
        }
        return
    }
    items(state.timeline, key = { "${it::class.simpleName}-${it.at}-${it.hashCode()}" }) { entry ->
        TimelineRow(entry, state.plant)
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry, plant: Plant) {
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors
    val zone = ZoneId.systemDefault()
    val (icon, title, detail) = when (entry) {
        is TimelineEntry.Watering -> Triple(
            R.drawable.ic_water_drop,
            stringResource(if (entry.log.watered) R.string.timeline_watered else R.string.timeline_not_watered),
            listOfNotNull(entry.log.soilMoisture?.let { stringResource(R.string.timeline_soil, it.label()) }, entry.log.amountMl?.let { "$it ml" }, entry.log.note).joinToString(" · "),
        )

        is TimelineEntry.Growth -> Triple(
            R.drawable.ic_eco,
            entry.log.stageName?.let { stringResource(R.string.timeline_stage, it) } ?: stringResource(R.string.timeline_growth),
            listOfNotNull(entry.log.heightCm?.let { stringResource(R.string.timeline_height, trimNumber(it)) }, entry.log.condition?.label(), entry.log.note).joinToString(" · "),
        )

        is TimelineEntry.Fertilizer -> Triple(
            R.drawable.ic_compost,
            stringResource(R.string.timeline_fertilized, entry.log.productName ?: entry.log.type.label()),
            listOfNotNull(
                entry.log.quantity?.let { "${trimNumber(it)} ${entry.log.unit.orEmpty()}".trim() },
                entry.log.method?.label(),
                entry.log.note,
            ).joinToString(" · "),
        )

        is TimelineEntry.Harvest -> Triple(
            R.drawable.ic_agriculture,
            stringResource(R.string.timeline_harvested, trimNumber(entry.log.quantity), entry.log.unit),
            entry.log.note.orEmpty(),
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, ext.garden, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = ext.mutedText)
            val dt = entry.at.atZone(zone)
            Text("${f.weekdayDayMonth.format(dt)} · ${f.time(dt.toLocalTime())}", style = MaterialTheme.typography.labelSmall, color = ext.mutedText)
        }
    }
}

fun trimNumber(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else "%.1f".format(v)

@Composable
private fun PhotosTab(photos: List<Photo>, plant: Plant, onSetCover: (String) -> Unit, onDelete: (Photo) -> Unit) {
    val f = rememberFormatters()
    var comparing by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf(listOf<Long>()) }
    var viewing by remember { mutableStateOf<Photo?>(null) }
    val zone = ZoneId.systemDefault()

    Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (photos.isEmpty()) {
            Text(stringResource(R.string.plant_photos_empty), color = AquaGrowTheme.colors.mutedText)
            return@Column
        }
        if (photos.size >= 2) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (comparing) stringResource(R.string.plant_photos_compare_hint) else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AquaGrowTheme.colors.mutedText,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    comparing = !comparing
                    selected = emptyList()
                }) {
                    Icon(painterResource(R.drawable.ic_compare), null)
                    Text(" " + stringResource(if (comparing) R.string.action_cancel else R.string.plant_photos_compare))
                }
            }
        }
        if (comparing && selected.size == 2) {
            val (a, b) = selected.mapNotNull { id -> photos.firstOrNull { it.id == id } }.sortedBy { it.takenAt }.let { it[0] to it[1] }
            val days = ChronoUnit.DAYS.between(a.takenAt.atZone(zone).toLocalDate(), b.takenAt.atZone(zone).toLocalDate())
            AppCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(a, b).forEach { p ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            AppPhoto(p.filePath, R.drawable.ic_sprout, AquaGrowTheme.module, Modifier.fillMaxWidth().aspectRatio(0.8f))
                            Text(f.dayMonthYear.format(p.takenAt.atZone(zone)), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Text(
                    pluralStringResource(R.plurals.plant_photos_compare_days, days.toInt(), days.toInt()),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                )
            }
        }
        photos.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { p ->
                    val isSelected = p.id in selected
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable {
                                if (comparing) {
                                    selected = when {
                                        isSelected -> selected - p.id
                                        selected.size < 2 -> selected + p.id
                                        else -> listOf(selected[1], p.id)
                                    }
                                } else {
                                    viewing = p
                                }
                            },
                    ) {
                        AppPhoto(
                            p.filePath,
                            R.drawable.ic_sprout,
                            AquaGrowTheme.module,
                            Modifier.fillMaxSize(),
                            contentDescription = f.dayMonthYear.format(p.takenAt.atZone(zone)),
                        )
                        if (comparing) SelectionBadge(isSelected, Modifier.align(Alignment.TopEnd).padding(6.dp))
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    viewing?.let { p ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { viewing = null }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(12.dp)) {
                    AppPhoto(p.filePath, R.drawable.ic_sprout, AquaGrowTheme.module, Modifier.fillMaxWidth().heightIn(max = 480.dp).aspectRatio(0.8f), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                    Text(f.dayMonthYear.format(p.takenAt.atZone(zone)), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            onDelete(p)
                            viewing = null
                        }) {
                            Text(stringResource(R.string.plant_photos_delete), color = MaterialTheme.colorScheme.error)
                        }
                        if (plant.coverPhotoPath != p.filePath) {
                            TextButton(onClick = {
                                onSetCover(p.filePath)
                                viewing = null
                            }) { Text(stringResource(R.string.photo_change)) }
                        }
                        TextButton(onClick = { viewing = null }) { Text(stringResource(R.string.action_close)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HarvestTab(state: PlantDetailUiState.Ready) {
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()
    Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.harvestEstimate?.let { HarvestEstimateCard(it, state.catalog?.harvestKind ?: HarvestKind.FIRST_HARVEST) }
        if (state.harvests.isEmpty()) {
            Text(stringResource(R.string.plant_harvest_empty), color = AquaGrowTheme.colors.mutedText)
            return@Column
        }
        AppCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.plant_harvest_total), style = MaterialTheme.typography.labelMedium, color = AquaGrowTheme.colors.mutedText)
            Text(
                state.harvestTotals.entries.joinToString("  ·  ") { (unit, total) -> "${trimNumber(total)} $unit" },
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        SectionHeader(stringResource(R.string.plant_tab_harvest))
        AppCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            state.harvests.forEachIndexed { i, h ->
                if (i > 0) HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${trimNumber(h.quantity)} ${h.unit}", style = MaterialTheme.typography.titleSmall)
                        h.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText) }
                    }
                    Text(f.dayMonthYear.format(h.harvestedAt.atZone(zone)), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                }
            }
        }
    }
}
