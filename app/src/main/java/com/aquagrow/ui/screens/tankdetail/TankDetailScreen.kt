package com.aquagrow.ui.screens.tankdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.RectangleShape
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
import com.aquagrow.domain.aquarium.RangeStatus
import com.aquagrow.domain.garden.DueStatus
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterParameter
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppFilterChip
import com.aquagrow.ui.components.AppPhoto
import com.aquagrow.ui.components.ChartPoint
import com.aquagrow.ui.components.ChipTone
import com.aquagrow.ui.components.ConfirmDialog
import com.aquagrow.ui.components.IconTile
import com.aquagrow.ui.components.LineChart
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.SecondaryButton
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.StatusChip
import com.aquagrow.ui.components.displayTitle
import com.aquagrow.ui.components.formatReading
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.components.scheduleSummary
import com.aquagrow.ui.components.taskIcon
import com.aquagrow.ui.components.unit
import com.aquagrow.ui.screens.garden.DueChip
import com.aquagrow.ui.screens.plantdetail.InfoGrid
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import java.time.ZoneId

private enum class TankTab(val label: Int) {
    OVERVIEW(R.string.tank_tab_overview),
    FISH(R.string.tank_tab_fish),
    WATER(R.string.tank_tab_water),
    EQUIPMENT(R.string.tank_tab_equipment),
}

private enum class TankSheet { WATER_TEST, RANGES, FEEDING_SCHEDULE, LOG_FEEDING, MAINT_SCHEDULE, LOG_MAINT, EQUIPMENT }

@Composable
fun TankDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAddFish: (Long) -> Unit,
    onEditFish: (Long) -> Unit,
    onAskAssistant: (Long) -> Unit = {},
    viewModel: TankDetailViewModel = hiltViewModel(),
) = ModuleTheme(CareModule.AQUARIUM) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val closed by viewModel.closed.collectAsStateWithLifecycle()
    LaunchedEffect(closed) { if (closed) onBack() }
    when (val s = state) {
        TankDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        TankDetailUiState.NotFound -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.tank_not_found))
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        }

        is TankDetailUiState.Ready -> TankDetailContent(s, viewModel, onBack, onEdit, onAddFish, onEditFish, onAskAssistant)
    }
}

@Composable
private fun TankDetailContent(
    state: TankDetailUiState.Ready,
    vm: TankDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAddFish: (Long) -> Unit,
    onEditFish: (Long) -> Unit,
    onAskAssistant: (Long) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(TankTab.OVERVIEW) }
    var sheet by remember { mutableStateOf<TankSheet?>(null) }
    var feedingTarget by remember { mutableStateOf<Long?>(null) }
    var maintTarget by remember { mutableStateOf<MaintTarget?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    val tank = state.aquarium

    Scaffold(
        containerColor = AquaGrowTheme.module.pageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (tab != TankTab.OVERVIEW) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
                        Row(Modifier.navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (tab) {
                                TankTab.OVERVIEW -> Unit

                                TankTab.FISH -> PrimaryButton(stringResource(R.string.aquarium_add_fish), { onAddFish(tank.id) }, icon = R.drawable.ic_add)

                                TankTab.WATER -> {
                                    SecondaryButton(stringResource(R.string.tank_edit_ranges), { sheet = TankSheet.RANGES }, Modifier.weight(1f))
                                    PrimaryButton(stringResource(R.string.tank_log_water_test), { sheet = TankSheet.WATER_TEST }, Modifier.weight(1f))
                                }

                                TankTab.EQUIPMENT -> PrimaryButton(stringResource(R.string.tank_add_equipment), { sheet = TankSheet.EQUIPMENT }, icon = R.drawable.ic_add)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "header") {
                Box(Modifier.fillMaxWidth().height(240.dp)) {
                    AppPhoto(tank.coverPhotoPath, R.drawable.ic_image, AquaGrowTheme.module, Modifier.fillMaxSize(), stringResource(R.string.photo_content_description, tank.name), shape = RectangleShape)
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.action_back)) }
                        }
                        Spacer(Modifier.weight(1f))
                        Box {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                                IconButton(onClick = { menu = true }) { Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.action_more_options)) }
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.plant_menu_edit)) }, onClick = {
                                    menu = false
                                    onEdit(tank.id)
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.assistant_ask_menu)) }, onClick = {
                                    menu = false
                                    onAskAssistant(tank.id)
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.plant_menu_archive)) }, onClick = {
                                    menu = false
                                    confirmArchive = true
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.plant_menu_delete), color = MaterialTheme.colorScheme.error) }, onClick = {
                                    menu = false
                                    confirmDelete = true
                                })
                            }
                        }
                    }
                }
            }
            item(key = "title") {
                Column(Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tank.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                    val f = rememberFormatters()
                    val dims = if (tank.lengthCm != null && tank.widthCm != null && tank.heightCm != null) {
                        "${formatReading(tank.lengthCm)}×${formatReading(tank.widthCm)}×${formatReading(tank.heightCm)} cm"
                    } else {
                        null
                    }
                    Text(
                        listOfNotNull(
                            tank.volumeLiters?.let { stringResource(R.string.aquarium_volume, formatReading(it)) },
                            dims,
                            tank.setupDate?.let { stringResource(R.string.tank_set_up, f.date(it)) } ?: tank.tankType.label(),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AquaGrowTheme.colors.mutedText,
                    )
                }
            }
            item(key = "tabs") {
                PrimaryTabRow(selectedTabIndex = tab.ordinal, containerColor = AquaGrowTheme.module.pageBackground, contentColor = AquaGrowTheme.module.primary) {
                    TankTab.entries.forEach { t ->
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
            item(key = "tab-${tab.name}") {
                Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (tab) {
                        TankTab.OVERVIEW -> OverviewTab(
                            state,
                            vm,
                            onLogWaterTest = { sheet = TankSheet.WATER_TEST },
                            onLogFeeding = { sheet = TankSheet.LOG_FEEDING },
                            onEditFeeding = {
                                feedingTarget = it
                                sheet = TankSheet.FEEDING_SCHEDULE
                            },
                            onEditMaint = {
                                maintTarget = it
                                sheet = TankSheet.MAINT_SCHEDULE
                            },
                            onLogMaint = { sheet = TankSheet.LOG_MAINT },
                            onOpenWater = { tab = TankTab.WATER },
                        )

                        TankTab.FISH -> FishTab(state, onEditFish)

                        TankTab.WATER -> WaterTab(state)

                        TankTab.EQUIPMENT -> EquipmentTab(state, vm::onRemoveEquipment)
                    }
                }
            }
        }
    }

    when (sheet) {
        TankSheet.WATER_TEST -> WaterTestSheet({
            vm.onLogWaterTest(it)
            sheet = null
        }, { sheet = null })

        TankSheet.RANGES -> RangesSheet(tank.ranges, state.fish, {
            vm.onSaveRanges(it)
            sheet = null
        }, { sheet = null })

        TankSheet.FEEDING_SCHEDULE -> FeedingScheduleSheet(
            fish = state.fish,
            existingForTarget = { target -> state.feeding.map { it.task }.filter { it.fishId == target } },
            initialTarget = feedingTarget,
            onSave = {
                vm.onSaveFeeding(it)
                sheet = null
            },
            onRemove = {
                vm.onRemoveFeeding(it)
                sheet = null
            },
            onDismiss = { sheet = null },
        )

        TankSheet.LOG_FEEDING -> LogFeedingSheet(state.fish, { fish, food, amount, note ->
            vm.onLogFeeding(fish, food, amount, note)
            sheet = null
        }, { sheet = null })

        TankSheet.MAINT_SCHEDULE -> MaintenanceScheduleSheet(
            existing = state.maintenance.map { it.task },
            initial = maintTarget,
            onSave = { target, schedule ->
                when (target) {
                    MaintTarget.WaterTest -> vm.onSaveMaintenanceSchedule(null, true, schedule)
                    is MaintTarget.Type -> vm.onSaveMaintenanceSchedule(target.type, false, schedule)
                }
                sheet = null
            },
            onDismiss = { sheet = null },
        )

        TankSheet.LOG_MAINT -> LogMaintenanceSheet(null, { type, pct, note ->
            vm.onLogMaintenance(type, pct, note)
            sheet = null
        }, { sheet = null })

        TankSheet.EQUIPMENT -> EquipmentSheet({ n, c, d, notes ->
            vm.onAddEquipment(n, c, d, notes)
            sheet = null
        }, { sheet = null })

        null -> Unit
    }
    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.tank_delete_title),
            stringResource(R.string.tank_delete_body),
            stringResource(R.string.action_delete),
            {
                confirmDelete = false
                vm.onDelete()
            },
            { confirmDelete = false },
            destructive = true,
        )
    }
    if (confirmArchive) {
        ConfirmDialog(
            stringResource(R.string.plant_menu_archive),
            stringResource(R.string.tank_archive_body),
            stringResource(R.string.plant_menu_archive),
            {
                confirmArchive = false
                vm.onArchive()
            },
            { confirmArchive = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewTab(
    state: TankDetailUiState.Ready,
    vm: TankDetailViewModel,
    onLogWaterTest: () -> Unit,
    onLogFeeding: () -> Unit,
    onEditFeeding: (Long?) -> Unit,
    onEditMaint: (MaintTarget?) -> Unit,
    onLogMaint: () -> Unit,
    onOpenWater: () -> Unit,
) {
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()
    val ext = AquaGrowTheme.colors
    val latest = state.latestTest

    // Water quality card with nitrate trend (mockup).
    AppCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tank_water_quality), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            latest?.let { Text(stringResource(R.string.tank_tested_on, f.dayMonth.format(it.testedAt.atZone(zone))), style = MaterialTheme.typography.bodyMedium, color = ext.mutedText) }
        }
        if (latest == null) {
            Text(stringResource(R.string.tank_no_tests), style = MaterialTheme.typography.bodyMedium, color = ext.mutedText, modifier = Modifier.padding(top = 8.dp))
        } else {
            Spacer(Modifier.height(14.dp))
            val checks = state.checks.associateBy { it.parameter }
            val cells = buildList<@Composable (Modifier) -> Unit> {
                for (p in listOf(WaterParameter.TEMPERATURE, WaterParameter.PH, WaterParameter.AMMONIA, WaterParameter.NITRITE, WaterParameter.NITRATE)) {
                    val c = checks[p]
                    if (c?.value != null) add { m -> ParameterCell(p.label(), "${formatReading(c.value)} ${p.unit()}".trim(), c.status, m) }
                }
                val gh = latest.ghDgh
                val kh = latest.khDkh
                if (gh != null || kh != null) {
                    val worst = listOfNotNull(checks[WaterParameter.GH]?.status, checks[WaterParameter.KH]?.status)
                        .firstOrNull { it == RangeStatus.ABOVE || it == RangeStatus.BELOW }
                        ?: listOfNotNull(checks[WaterParameter.GH]?.status, checks[WaterParameter.KH]?.status).firstOrNull { it == RangeStatus.IN_RANGE }
                        ?: RangeStatus.NOT_SET
                    add { m -> ParameterCell("${stringResource(R.string.param_gh)} / ${stringResource(R.string.param_kh)}", "${gh?.let(::formatReading) ?: "—"} / ${kh?.let(::formatReading) ?: "—"} ${stringResource(R.string.unit_dgh)}", worst, m) }
                }
            }
            cells.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    row.forEach { cell -> cell(Modifier.weight(1f)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            val nitrateTests = state.tests.filter { it.nitratePpm != null }.sortedBy { it.testedAt }.takeLast(6)
            if (nitrateTests.size >= 2) {
                val alert = state.aquarium.ranges.nitrateMaxPpm
                Text(pluralStringResource(R.plurals.tank_nitrate_last, nitrateTests.size, nitrateTests.size), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                LineChart(
                    points = nitrateTests.mapIndexed { i, t -> ChartPoint(i.toFloat(), t.nitratePpm!!.toFloat()) },
                    alertLines = listOfNotNull(alert?.toFloat()),
                    height = 120.dp,
                    description = stringResource(R.string.param_nitrate) + ": " + nitrateTests.joinToString("; ") { "${f.dayMonth.format(it.testedAt.atZone(zone))} ${formatReading(it.nitratePpm!!)}" },
                )
                if (alert != null) {
                    Text(
                        stringResource(R.string.tank_chart_caption, formatReading(alert)),
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.mutedText,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        val out = state.checks.filter { it.status == RangeStatus.ABOVE || it.status == RangeStatus.BELOW }
        if (out.isNotEmpty()) {
            val parts = mutableListOf<String>()
            for (c in out) parts += "${c.parameter.label()} ${formatReading(c.value!!)} ${c.parameter.unit()}".trim()
            Surface(shape = MaterialTheme.shapes.medium, color = ext.warningContainer, contentColor = ext.onWarningContainer, modifier = Modifier.padding(top = 12.dp)) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(painterResource(R.drawable.ic_warning), null)
                    Column {
                        Text(stringResource(R.string.tank_alert_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.tank_alert_body, parts.joinToString(", ")), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.tank_log_water_test), onLogWaterTest)
    }

    // Feeding (mockup: time column, food · portion, who, Done chip or Skip pill).
    SectionHeader(stringResource(R.string.tank_feeding_title), actionLabel = stringResource(R.string.tank_add_schedule), onAction = { onEditFeeding(null) })
    AppCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        val rows = state.feedingToday.sortedBy { it.task.reminderTime }
        if (state.feeding.isEmpty()) {
            Text(stringResource(R.string.tank_feeding_none), color = ext.mutedText, modifier = Modifier.padding(4.dp))
        } else if (rows.isEmpty()) {
            Text(stringResource(R.string.feeding_none_today), color = ext.mutedText, modifier = Modifier.padding(4.dp))
        }
        rows.forEachIndexed { i, occ ->
            if (i > 0) HorizontalDivider(color = ext.cardBorder)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(occ.task.reminderTime?.let(f::time) ?: "—", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(72.dp))
                Column(Modifier.weight(1f).clickable { onEditFeeding(occ.task.fishId) }) {
                    Text(listOfNotNull(occ.task.itemName, occ.task.amountText).joinToString(" · ").ifEmpty { stringResource(R.string.task_type_feeding) }, style = MaterialTheme.typography.titleMedium)
                    Text(occ.task.subjectName ?: stringResource(R.string.feeding_whole_tank), style = MaterialTheme.typography.bodyMedium, color = ext.mutedText)
                }
                when (occ.status) {
                    OccurrenceStatus.DONE -> Surface(onClick = { vm.onUndoOccurrence(occ) }, shape = RoundedCornerShape(12.dp), color = ext.garden.container) {
                        Text(stringResource(R.string.task_done_label), style = MaterialTheme.typography.labelLarge, color = ext.success, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }

                    OccurrenceStatus.SKIPPED -> Surface(onClick = { vm.onUndoOccurrence(occ) }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(stringResource(R.string.feeding_skipped), style = MaterialTheme.typography.labelLarge, color = ext.mutedText, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                    }

                    OccurrenceStatus.PENDING -> Row {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { vm.onSkipFeeding(occ) },
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, AquaGrowTheme.module.primary),
                        ) { Text(stringResource(R.string.action_skip)) }
                        IconButton(onClick = { vm.onDoneOccurrence(occ) }) {
                            Icon(painterResource(R.drawable.ic_check), stringResource(R.string.task_mark_done, occ.task.displayTitle()), tint = AquaGrowTheme.module.primary)
                        }
                    }
                }
            }
        }
        androidx.compose.material3.TextButton(onClick = onLogFeeding) { Text(stringResource(R.string.tank_log_feeding)) }
    }

    // Maintenance (mockup rows).
    SectionHeader(stringResource(R.string.tank_maintenance), actionLabel = stringResource(R.string.tank_add_schedule), onAction = { onEditMaint(null) })
    AppCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        if (state.maintenance.isEmpty()) Text(stringResource(R.string.tank_maintenance_none), color = ext.mutedText, modifier = Modifier.padding(4.dp))
        state.maintenance.forEachIndexed { i, m ->
            if (i > 0) HorizontalDivider(color = ext.cardBorder)
            val name = if (m.task.type == TaskType.WATER_TEST) stringResource(R.string.maint_water_testing) else m.task.maintenanceType?.label() ?: m.task.title
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onEditMaint(if (m.task.type == TaskType.WATER_TEST) MaintTarget.WaterTest else MaintTarget.Type(m.task.maintenanceType ?: com.aquagrow.domain.model.MaintenanceType.OTHER)) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconTile(taskIcon(m.task.type, m.task.maintenanceType), AquaGrowTheme.module, size = 48.dp)
                Column(Modifier.weight(1f)) {
                    Text("$name · ${m.task.scheduleSummary().replaceFirstChar { it.lowercase() }}", style = MaterialTheme.typography.titleMedium)
                    val next = m.next?.let { stringResource(R.string.plant_next, f.weekdayDayMonth.format(it.date)) }
                    val overdue = m.next?.takeIf { it.isOverdue(state.today) }
                    Text(
                        next ?: (m.lastDone?.let { stringResource(R.string.tank_last_done, f.dayMonth.format(it.atZone(zone))) } ?: stringResource(R.string.tank_never_done)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdue != null) ext.onWarningContainer else ext.mutedText,
                    )
                }
            }
        }
        androidx.compose.material3.TextButton(onClick = onLogMaint) { Text(stringResource(R.string.tank_log_maintenance)) }
    }

    if (state.maintenanceLogs.isNotEmpty() || state.feedingLogs.isNotEmpty()) {
        SectionHeader(stringResource(R.string.tank_history))
        AppCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
            val entries = (
                state.maintenanceLogs.map { it.performedAt to "${it.type.label()}${it.waterChangePercent?.let { p -> " · $p%" }.orEmpty()}" } +
                    state.feedingLogs.map { log ->
                        log.fedAt to listOfNotNull(
                            stringResource(if (log.skipped) R.string.feeding_skipped else R.string.feeding_fed),
                            state.fish.firstOrNull { it.id == log.fishId }?.displayName,
                            log.foodName,
                        ).joinToString(" · ")
                    }
                ).sortedByDescending { it.first }.take(12)
            entries.forEach { (at, text) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(f.dayMonth.format(at.atZone(zone)), style = MaterialTheme.typography.labelSmall, color = ext.mutedText)
                }
            }
        }
    }
}

@Composable
private fun ParameterCell(label: String, value: String, status: RangeStatus, modifier: Modifier) {
    val ext = AquaGrowTheme.colors
    Surface(shape = MaterialTheme.shapes.medium, color = ext.aquarium.pageBackground, modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = ext.mutedText, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            val (text, color) = when (status) {
                RangeStatus.IN_RANGE -> stringResource(R.string.tank_in_range) to ext.success
                RangeStatus.BELOW -> stringResource(R.string.tank_below_short) to ext.onWarningContainer
                RangeStatus.ABOVE -> stringResource(R.string.tank_above_short) to ext.onWarningContainer
                else -> stringResource(R.string.tank_no_range) to ext.mutedText
            }
            Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun FishTab(state: TankDetailUiState.Ready, onEditFish: (Long) -> Unit) {
    if (state.fish.isEmpty()) {
        Text(stringResource(R.string.tank_fish_empty), color = AquaGrowTheme.colors.mutedText)
        return
    }
    state.fish.forEach { fish ->
        AppCard(Modifier.fillMaxWidth(), onClick = { onEditFish(fish.id) }, contentPadding = 12.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppPhoto(fish.photoPath, R.drawable.ic_fish, AquaGrowTheme.module, Modifier.size(64.dp), shape = CircleShape)
                Column(Modifier.weight(1f)) {
                    Text(fish.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(listOfNotNull(fish.speciesName.takeIf { fish.nickname != null }, "× ${fish.count}", fish.ageText).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                    fish.healthObservations?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaterTab(state: TankDetailUiState.Ready) {
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()
    var param by rememberSaveable { mutableStateOf(WaterParameter.NITRATE) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TankDetailViewModel.CHART_PARAMS) { p -> AppFilterChip(param == p, p.label(), { param = p }) }
    }
    val points = state.tests.mapNotNull { t -> t.value(param)?.let { ChartPoint(t.testedAt.atZone(zone).toLocalDate().toEpochDay().toFloat() + t.testedAt.atZone(zone).toLocalTime().toSecondOfDay() / 86400f, it.toFloat()) } }
    val (min, max) = state.aquarium.ranges.bounds(param)
    val alerts = listOfNotNull(min, max).map { it.toFloat() }
    AppCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.tank_trend, param.label()), style = MaterialTheme.typography.titleMedium)
        if (alerts.isNotEmpty()) {
            Text(
                stringResource(R.string.tank_trend_alert, alerts.joinToString(" – ") { formatReading(it.toDouble()) } + " " + param.unit()),
                style = MaterialTheme.typography.bodySmall,
                color = AquaGrowTheme.colors.onWarningContainer,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (points.size < 2) {
            Text(stringResource(R.string.tank_trend_need_more), color = AquaGrowTheme.colors.mutedText)
        } else {
            val sorted = state.tests.filter { it.value(param) != null }.sortedBy { it.testedAt }
            LineChart(
                points = points,
                alertLines = alerts,
                description = "${param.label()}: " + sorted.joinToString("; ") { "${f.dayMonth.format(it.testedAt.atZone(zone))} ${formatReading(it.value(param)!!)}" },
                startLabel = f.dayMonth.format(sorted.first().testedAt.atZone(zone)),
                endLabel = f.dayMonth.format(sorted.last().testedAt.atZone(zone)),
            )
        }
    }
    SectionHeader(stringResource(R.string.tank_history))
    if (state.tests.isEmpty()) Text(stringResource(R.string.tank_no_tests), color = AquaGrowTheme.colors.mutedText)
    state.tests.take(30).forEach { t ->
        AppCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Text(f.weekdayDayMonth.format(t.testedAt.atZone(zone)) + " · " + f.time(t.testedAt.atZone(zone).toLocalTime()), style = MaterialTheme.typography.titleSmall)
            Text(
                WaterParameter.entries.mapNotNull { p -> t.value(p)?.let { "${p.label()} ${formatReading(it)}" } }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = AquaGrowTheme.colors.mutedText,
            )
            t.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun EquipmentTab(state: TankDetailUiState.Ready, onRemove: (com.aquagrow.domain.model.InventoryItem) -> Unit) {
    val a = state.aquarium
    AppCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.tank_equipment_setup), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val info = buildList {
            a.filterType?.let { add(stringResource(R.string.add_tank_filter) to it) }
            a.heater?.let { add(stringResource(R.string.add_tank_heater) to it) }
            a.lighting?.let { add(stringResource(R.string.add_tank_lighting) to it) }
            a.substrate?.let { add(stringResource(R.string.add_tank_substrate) to it) }
            if (a.hasLivePlants) add(stringResource(R.string.add_tank_live_plants) to (a.livePlantsNote ?: "✓"))
            a.setupDate?.let { add(stringResource(R.string.add_tank_setup_date) to rememberFormatters().date(it)) }
        }
        if (info.isEmpty()) Text(stringResource(R.string.form_not_set), color = AquaGrowTheme.colors.mutedText) else InfoGrid(info)
        a.otherEquipment?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
    }
    SectionHeader(stringResource(R.string.tank_equipment_items))
    if (state.equipment.isEmpty()) Text(stringResource(R.string.tank_equipment_none), color = AquaGrowTheme.colors.mutedText)
    state.equipment.forEach { item ->
        AppCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                    Text(listOfNotNull(item.category.label(), item.notes).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                }
                IconButton(onClick = { onRemove(item) }) { Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.action_delete)) }
            }
        }
    }
}
