package com.aquagrow.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.garden.DueStatus
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.Season
import com.aquagrow.domain.model.SowingGuide
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.ChipTone
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.ConfirmDialog
import com.aquagrow.ui.components.FormSection
import com.aquagrow.ui.components.IconTile
import com.aquagrow.ui.components.RescheduleDialog
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.SegmentedControl
import com.aquagrow.ui.components.StatusChip
import com.aquagrow.ui.components.TipCard
import com.aquagrow.ui.components.currentLocale
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.components.taskIcon
import com.aquagrow.ui.screens.plantdetail.trimNumber
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WateringScreen(onBack: () -> Unit, onOpenPlant: (Long) -> Unit, viewModel: WateringViewModel = hiltViewModel()) = ModuleTheme(CareModule.GARDEN) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var recordFor by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var choosePlant by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf<TaskOccurrence?>(null) }
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()

    Scaffold(
        containerColor = AquaGrowTheme.module.pageBackground,
        topBar = { AppTopBar(stringResource(R.string.watering_title), onBack) },
        floatingActionButton = {
            if (state.plants.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { choosePlant = true },
                    containerColor = AquaGrowTheme.module.primary,
                    contentColor = AquaGrowTheme.module.onPrimary,
                    icon = { Icon(painterResource(R.drawable.ic_water_drop), null) },
                    text = { Text(stringResource(R.string.watering_log_any)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { TipCard(stringResource(R.string.home_tip_title), stringResource(R.string.watering_check_soil)) }
            val nothingDue = state.overdue.isEmpty() && state.dueToday.isEmpty() && state.upcoming.isEmpty()
            if (nothingDue && !state.loading) {
                item { Text(stringResource(R.string.watering_none_due), color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(vertical = 8.dp)) }
            }
            occurrenceSection(R.string.watering_overdue, state.overdue, state.today, { recordFor = it.task.plantId!! to (it.task.subjectName ?: "") }, { moving = it }, viewModel::undo)
            occurrenceSection(R.string.watering_today, state.dueToday, state.today, { recordFor = it.task.plantId!! to (it.task.subjectName ?: "") }, { moving = it }, viewModel::undo)
            occurrenceSection(R.string.watering_upcoming, state.upcoming, state.today, { recordFor = it.task.plantId!! to (it.task.subjectName ?: "") }, { moving = it }, viewModel::undo)

            item { SectionHeader(stringResource(R.string.watering_history)) }
            if (state.history.isEmpty()) {
                item { Text(stringResource(R.string.watering_no_history), color = AquaGrowTheme.colors.mutedText) }
            }
            items(state.history, key = { it.first.id }) { (log, name) ->
                AppCard(Modifier.fillMaxWidth(), onClick = { onOpenPlant(log.plantId) }, contentPadding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconTile(R.drawable.ic_water_drop, AquaGrowTheme.module, size = 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(
                                    stringResource(if (log.watered) R.string.timeline_watered else R.string.timeline_not_watered),
                                    log.soilMoisture?.let { stringResource(R.string.timeline_soil, it.label()) },
                                    log.amountMl?.let { "$it ml" },
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = AquaGrowTheme.colors.mutedText,
                            )
                        }
                        val dt = log.loggedAt.atZone(zone)
                        Text("${f.dayMonth.format(dt)}\n${f.time(dt.toLocalTime())}", style = MaterialTheme.typography.labelSmall, color = AquaGrowTheme.colors.mutedText)
                    }
                }
            }
        }
    }

    recordFor?.let { (id, name) ->
        RecordWateringSheet(name, {
            viewModel.record(id, it)
            recordFor = null
        }, { recordFor = null })
    }
    if (choosePlant) {
        PlantPickerSheet(state.plants.map { it.id to it.name }, onPick = {
            recordFor = it
            choosePlant = false
        }, onDismiss = { choosePlant = false })
    }
    moving?.let { occ ->
        RescheduleDialog(state.today, occ.date, {
            viewModel.move(occ, it)
            moving = null
        }, { moving = null })
    }
}

private fun LazyListScope.occurrenceSection(
    title: Int,
    list: List<TaskOccurrence>,
    today: java.time.LocalDate,
    onDone: (TaskOccurrence) -> Unit,
    onMove: (TaskOccurrence) -> Unit,
    onUndo: (TaskOccurrence) -> Unit,
) {
    if (list.isEmpty()) return
    item(key = "h-$title") { SectionHeader(stringResource(title), trailingText = list.size.toString()) }
    items(list, key = { "o-$title-${it.task.id}-${it.originalDate}" }) { occ ->
        OccurrenceCard(occ, today, onDone, onMove, onUndo)
    }
}

@Composable
fun OccurrenceCard(
    occ: TaskOccurrence,
    today: java.time.LocalDate,
    onDone: (TaskOccurrence) -> Unit,
    onMove: (TaskOccurrence) -> Unit,
    onUndo: (TaskOccurrence) -> Unit,
) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(taskIcon(occ.task.type, occ.task.maintenanceType), AquaGrowTheme.module, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(occ.task.subjectName ?: occ.task.title, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    if (occ.isHandled) {
                        StatusChip(stringResource(R.string.task_done_label), ChipTone.SUCCESS)
                    } else {
                        DueChip(occ.task.title, DueStatus.of(occ.date, today))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (occ.isHandled) {
                TextButton(onClick = { onUndo(occ) }) { Text(stringResource(R.string.action_undo)) }
            } else {
                TextButton(onClick = { onMove(occ) }) { Text(stringResource(R.string.watering_move)) }
                TextButton(onClick = { onDone(occ) }) { Text(stringResource(R.string.action_done)) }
            }
        }
    }
}

@Composable
fun PlantPickerSheet(
    plants: List<Pair<Long, String>>,
    onPick: (Pair<Long, String>) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.watering_choose_plant),
) {
    var selected by remember { mutableStateOf<Long?>(null) }
    FormSheet(
        title = title,
        confirmLabel = stringResource(R.string.action_continue),
        confirmEnabled = selected != null,
        onConfirm = { plants.firstOrNull { it.first == selected }?.let(onPick) },
        onDismiss = onDismiss,
    ) {
        ChoiceChips(plants.map { it.first }, selected, { id -> plants.first { it.first == id }.second }, { selected = it })
    }
}

@Composable
fun FertilizerScreen(onBack: () -> Unit, onOpenPlant: (Long) -> Unit, viewModel: FertilizerViewModel = hiltViewModel()) = ModuleTheme(CareModule.GARDEN) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var logging by remember { mutableStateOf<Long?>(null) }
    var logAny by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf<TaskOccurrence?>(null) }
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()

    Scaffold(
        containerColor = AquaGrowTheme.module.pageBackground,
        topBar = { AppTopBar(stringResource(R.string.fertilizer_title), onBack) },
        floatingActionButton = {
            if (state.plants.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { logAny = true },
                    containerColor = AquaGrowTheme.module.primary,
                    contentColor = AquaGrowTheme.module.onPrimary,
                    icon = { Icon(painterResource(R.drawable.ic_compost), null) },
                    text = { Text(stringResource(R.string.plant_log_fertilizer)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { TipCard(stringResource(R.string.home_tip_title), stringResource(R.string.fertilizer_label_note)) }
            item { SectionHeader(stringResource(R.string.fertilizer_upcoming)) }
            if (state.upcoming.isEmpty()) {
                item { Text(stringResource(R.string.fertilizer_none_upcoming), color = AquaGrowTheme.colors.mutedText) }
            }
            items(state.upcoming, key = { "u-${it.task.id}-${it.originalDate}" }) { occ ->
                OccurrenceCard(occ, state.today, { logging = it.task.plantId }, { moving = it }, {})
            }
            item { SectionHeader(stringResource(R.string.fertilizer_history)) }
            if (state.history.isEmpty()) {
                item { Text(stringResource(R.string.fertilizer_none), color = AquaGrowTheme.colors.mutedText) }
            }
            items(state.history, key = { "h-${it.first.id}" }) { (log, name) ->
                AppCard(Modifier.fillMaxWidth(), onClick = { onOpenPlant(log.plantId) }, contentPadding = 12.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(R.drawable.ic_compost, AquaGrowTheme.module, size = 36.dp)
                        Column(Modifier.weight(1f)) {
                            Text("$name · ${log.productName ?: log.type.label()}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(
                                    log.type.label(),
                                    log.quantity?.let { "${trimNumber(it)} ${log.unit.orEmpty()}".trim() },
                                    log.method?.label(),
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = AquaGrowTheme.colors.mutedText,
                            )
                        }
                        Text(f.dayMonth.format(log.appliedAt.atZone(zone)), style = MaterialTheme.typography.labelSmall, color = AquaGrowTheme.colors.mutedText)
                    }
                }
            }
        }
    }

    if (logAny || logging != null) {
        FertilizerLogSheet(
            plantId = logging,
            plants = if (logAny) state.plants else null,
            onSave = {
                viewModel.log(it)
                logging = null
                logAny = false
            },
            onDismiss = {
                logging = null
                logAny = false
            },
        )
    }
    moving?.let { occ ->
        RescheduleDialog(state.today, occ.date, {
            viewModel.move(occ, it)
            moving = null
        }, { moving = null })
    }
}

@Composable
fun PlannerScreen(onBack: () -> Unit, viewModel: PlannerViewModel = hiltViewModel()) = ModuleTheme(CareModule.GARDEN) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<SowingGuide?>(null) }
    var deleting by remember { mutableStateOf<SowingGuide?>(null) }
    val f = rememberFormatters()
    val locale = currentLocale()
    fun monthName(m: Int) = Month.of(m).getDisplayName(TextStyle.SHORT, locale)

    Scaffold(
        containerColor = AquaGrowTheme.module.pageBackground,
        topBar = { AppTopBar(stringResource(R.string.planner_title), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = SowingGuide(plantName = "", season = state.season, startMonth = state.month, endMonth = state.month) },
                containerColor = AquaGrowTheme.module.primary,
                contentColor = AquaGrowTheme.module.onPrimary,
                icon = { Icon(painterResource(R.drawable.ic_add), null) },
                text = { Text(stringResource(R.string.planner_add)) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(R.string.planner_intro), style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText) }
            state.lastReminder?.let { (name, date) ->
                item { StatusChip(stringResource(R.string.planner_reminded, "$name · ${f.weekdayDayMonth.format(date)}"), ChipTone.SUCCESS) }
            }
            item {
                AppCard(Modifier.fillMaxWidth(), color = AquaGrowTheme.module.surfaceAlt) {
                    Text(stringResource(R.string.planner_now), style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.sowNow.joinToString(", ") { it.plantName }.ifEmpty { stringResource(R.string.planner_now_none) },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item {
                SegmentedControl(Season.entries, state.season, { stringResource(seasonShort(it)) }, viewModel::selectSeason)
            }
            items(state.guides, key = { it.id }) { g ->
                AppCard(Modifier.fillMaxWidth(), onClick = { editing = g }, contentPadding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.plantName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.planner_window, monthName(g.startMonth), monthName(g.endMonth)),
                                style = MaterialTheme.typography.bodySmall,
                                color = AquaGrowTheme.colors.mutedText,
                            )
                            g.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        if (g.userEdited) StatusChip(stringResource(R.string.planner_edited), ChipTone.NEUTRAL)
                        TextButton(onClick = { viewModel.remind(g) }) { Text(stringResource(R.string.planner_remind)) }
                    }
                }
            }
        }
    }

    editing?.let { g ->
        SowingGuideSheet(
            guide = g,
            monthName = ::monthName,
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = if (g.id != 0L) {
                (
                    {
                        deleting = g
                        editing = null
                    }
                    )
            } else {
                null
            },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { g ->
        ConfirmDialog(
            title = stringResource(R.string.action_delete),
            body = g.plantName,
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(g)
                deleting = null
            },
            onDismiss = { deleting = null },
            destructive = true,
        )
    }
}

private fun seasonShort(s: Season) = when (s) {
    Season.KHARIF -> R.string.season_kharif_short
    Season.RABI -> R.string.season_rabi_short
    Season.ZAID -> R.string.season_zaid_short
}

@Composable
private fun SowingGuideSheet(
    guide: SowingGuide,
    monthName: (Int) -> String,
    onSave: (SowingGuide) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var g by remember { mutableStateOf(guide) }
    FormSheet(
        title = stringResource(if (guide.id == 0L) R.string.planner_add else R.string.planner_edit),
        confirmLabel = stringResource(R.string.action_save),
        confirmEnabled = g.plantName.isNotBlank(),
        onConfirm = { onSave(g) },
        onDismiss = onDismiss,
    ) {
        AppTextField(g.plantName, { g = g.copy(plantName = it) }, stringResource(R.string.planner_plant_name), required = true, maxLength = 40)
        FormSection(stringResource(R.string.planner_season)) {
            ChoiceChips(Season.entries, g.season, { it.label() }, { g = g.copy(season = it) })
        }
        FormSection(stringResource(R.string.planner_start_month)) {
            ChoiceChips((1..12).toList(), g.startMonth, { monthName(it) }, { g = g.copy(startMonth = it) })
        }
        FormSection(stringResource(R.string.planner_end_month)) {
            ChoiceChips((1..12).toList(), g.endMonth, { monthName(it) }, { g = g.copy(endMonth = it) })
        }
        AppTextField(g.notes.orEmpty(), { g = g.copy(notes = it) }, stringResource(R.string.planner_notes), singleLine = false, maxLength = 300)
        if (onDelete != null) {
            HorizontalDivider()
            TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
        }
    }
}
