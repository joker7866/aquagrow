package com.aquagrow.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.IconTile
import com.aquagrow.ui.components.PillSegmented
import com.aquagrow.ui.components.RescheduleDialog
import com.aquagrow.ui.components.displayTitle
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.components.taskIcon
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.forModule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CalendarActions(
    val onAddTask: (LocalDate) -> Unit = {},
    val onEditTask: (Long) -> Unit = {},
    val onOpenPlant: (Long) -> Unit = {},
    val onOpenTank: (Long) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(actions: CalendarActions, viewModel: CalendarViewModel = hiltViewModel()) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    var optionsFor by remember { mutableStateOf<TaskOccurrence?>(null) }
    var moving by remember { mutableStateOf<TaskOccurrence?>(null) }
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f).semantics { heading() })
                    Surface(
                        onClick = { actions.onAddTask(s.selected) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.calendar_add), Modifier.size(30.dp)) }
                    }
                }
            }
            item {
                PillSegmented(
                    CalendarMode.entries,
                    s.mode,
                    {
                        stringResource(
                            when (it) {
                                CalendarMode.DAY -> R.string.calendar_day
                                CalendarMode.WEEK -> R.string.calendar_week
                                CalendarMode.MONTH -> R.string.calendar_month
                            },
                        )
                    },
                    viewModel::setMode,
                )
            }
            if (s.gardenEnabled && s.aquariumEnabled) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DotChip(stringResource(R.string.calendar_both), MaterialTheme.colorScheme.onSurface, s.filter == CalendarFilter.BOTH) { viewModel.setFilter(CalendarFilter.BOTH) }
                        DotChip(stringResource(R.string.calendar_garden), ext.garden.primary, s.filter == CalendarFilter.GARDEN) { viewModel.setFilter(CalendarFilter.GARDEN) }
                        DotChip(stringResource(R.string.calendar_aquarium), AquariumDot, s.filter == CalendarFilter.AQUARIUM) { viewModel.setFilter(CalendarFilter.AQUARIUM) }
                    }
                }
            }
            item {
                when (s.mode) {
                    CalendarMode.MONTH -> MonthCard(s, viewModel::select, viewModel::step)
                    CalendarMode.WEEK -> WeekCard(s, viewModel::select, viewModel::step)
                    CalendarMode.DAY -> DayHeader(s, viewModel::step)
                }
            }
            if (s.overdue.isNotEmpty()) {
                item { Text(stringResource(R.string.calendar_overdue), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
                items(s.overdue, key = { "o-${it.task.id}-${it.originalDate}" }) { occ -> OverdueCard(occ, s.today, { viewModel.toggleDone(occ) }, { optionsFor = occ }) }
            }
            item {
                Text(f.fullDate.format(s.selected), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            }
            item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = 8.dp) {
                    if (s.selectedTasks.isEmpty()) {
                        Text(stringResource(R.string.calendar_nothing), color = ext.mutedText, modifier = Modifier.padding(16.dp))
                    }
                    s.selectedTasks.forEachIndexed { i, occ ->
                        if (i > 0) HorizontalDivider(color = ext.cardBorder)
                        CalendarTaskRow(occ, s.defaultTime) { optionsFor = occ }
                    }
                }
            }
        }
    }

    optionsFor?.let { occ ->
        ModalBottomSheet(onDismissRequest = { optionsFor = null }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(occ.task.displayTitle(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp))
                SheetItem(if (occ.isHandled) R.string.calendar_mark_not_done else R.string.calendar_mark_done, R.drawable.ic_check) {
                    viewModel.toggleDone(occ)
                    optionsFor = null
                }
                if (!occ.isHandled) {
                    SheetItem(R.string.calendar_skip, R.drawable.ic_close) {
                        viewModel.skip(occ)
                        optionsFor = null
                    }
                    SheetItem(R.string.calendar_move, R.drawable.ic_event) {
                        moving = occ
                        optionsFor = null
                    }
                }
                SheetItem(R.string.calendar_edit_task, R.drawable.ic_edit) {
                    actions.onEditTask(occ.task.id)
                    optionsFor = null
                }
                occ.task.plantId?.let { id ->
                    SheetItem(R.string.calendar_open_plant, R.drawable.ic_sprout) {
                        actions.onOpenPlant(id)
                        optionsFor = null
                    }
                }
                occ.task.aquariumId?.let { id ->
                    SheetItem(R.string.calendar_open_tank, R.drawable.ic_fish) {
                        actions.onOpenTank(id)
                        optionsFor = null
                    }
                }
            }
        }
    }
    moving?.let { occ ->
        RescheduleDialog(s.today, occ.date, {
            viewModel.move(occ, it)
            moving = null
        }, { moving = null })
    }
}

/** Mockup blue for aquarium dots in the calendar (lighter than the primary for 6dp dots). */
private val AquariumDot = Color(0xFF1E88C7)

@Composable
private fun SheetItem(label: Int, icon: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        leadingContent = { Icon(painterResource(icon), null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun DotChip(text: String, dot: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else AquaGrowTheme.colors.cardBorder),
        modifier = Modifier.heightIn(min = Dimens.MinTouchTarget).semantics {
            this.selected = selected
            role = Role.RadioButton
        },
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(dot, CircleShape))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ModuleDots(list: List<TaskOccurrence>) {
    val garden = list.any { it.task.module == CareModule.GARDEN }
    val aqua = list.any { it.task.module == CareModule.AQUARIUM }
    Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (garden) Box(Modifier.size(7.dp).background(AquaGrowTheme.colors.garden.primary, CircleShape))
        if (aqua) Box(Modifier.size(7.dp).background(AquariumDot, CircleShape))
    }
}

@Composable
private fun NavRow(title: String, onStep: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onStep(false) }) { Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.calendar_prev)) }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = { onStep(true) }) { Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.calendar_next)) }
    }
}

@Composable
private fun WeekdayHeader(start: LocalDate) {
    val f = rememberFormatters()
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
        (0L..6L).forEach { i ->
            Text(
                f.weekdayNarrow.format(start.plusDays(i)),
                style = MaterialTheme.typography.labelLarge,
                color = AquaGrowTheme.colors.mutedText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, s: CalendarUiState, inMonth: Boolean, onSelect: (LocalDate) -> Unit, modifier: Modifier) {
    val f = rememberFormatters()
    val list = s.byDate[date].orEmpty()
    val isSelected = date == s.selected
    val past = date.isBefore(s.today)
    val label = pluralStringResource(R.plurals.calendar_day_tasks, list.size, f.fullDate.format(date), list.size)
    Column(
        modifier
            .heightIn(min = 56.dp)
            .clickable(enabled = inMonth, role = Role.Button) { onSelect(date) }
            .semantics {
                contentDescription = label
                this.selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (inMonth) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.surface
                        past -> AquaGrowTheme.colors.mutedText.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            ModuleDots(list)
        }
    }
}

@Composable
private fun MonthCard(s: CalendarUiState, onSelect: (LocalDate) -> Unit, onStep: (Boolean) -> Unit) {
    val f = rememberFormatters()
    AppCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        NavRow(f.monthYear.format(s.month), onStep)
        Spacer(Modifier.height(12.dp))
        WeekdayHeader(s.gridStart)
        Spacer(Modifier.height(8.dp))
        val weeks = ((ChronoUnit.DAYS.between(s.gridStart, s.month.atEndOfMonth()) / 7) + 1).toInt()
        (0 until weeks).forEach { w ->
            Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { d ->
                    val date = s.gridStart.plusDays((w * 7 + d).toLong())
                    DayCell(date, s, date.month == s.month.month, onSelect, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WeekCard(s: CalendarUiState, onSelect: (LocalDate) -> Unit, onStep: (Boolean) -> Unit) {
    val f = rememberFormatters()
    AppCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        NavRow("${f.dayMonth.format(s.weekStart)} – ${f.dayMonth.format(s.weekStart.plusDays(6))}", onStep)
        Spacer(Modifier.height(12.dp))
        WeekdayHeader(s.weekStart)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            (0L..6L).forEach { i -> DayCell(s.weekStart.plusDays(i), s, true, onSelect, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun DayHeader(s: CalendarUiState, onStep: (Boolean) -> Unit) {
    val f = rememberFormatters()
    AppCard(Modifier.fillMaxWidth(), contentPadding = 8.dp) {
        NavRow(if (s.selected == s.today) stringResource(R.string.calendar_today) else f.weekdayDayMonth.format(s.selected), onStep)
    }
}

@Composable
private fun OverdueCard(occ: TaskOccurrence, today: LocalDate, onDone: () -> Unit, onOptions: () -> Unit) {
    val ext = AquaGrowTheme.colors
    val days = ChronoUnit.DAYS.between(occ.date, today).toInt()
    val due = if (days == 1) stringResource(R.string.calendar_due_yesterday) else pluralStringResource(R.plurals.calendar_due_days_ago, days, days)
    Surface(
        onClick = onOptions,
        shape = MaterialTheme.shapes.large,
        color = ext.warningContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, ext.warningContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(taskIcon(occ.task.type, occ.task.maintenanceType), ext.forModule(occ.task.module), size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(occ.task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(occ.task.subjectName, due).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = ext.onWarningContainer)
            }
            Surface(onClick = onDone, shape = CircleShape, color = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface, modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.calendar_done), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarTaskRow(occ: TaskOccurrence, defaultTime: java.time.LocalTime, onClick: () -> Unit) {
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors
    val done = occ.status == OccurrenceStatus.DONE
    val meta = listOfNotNull(
        stringResource(if (occ.task.module == CareModule.GARDEN) R.string.home_module_garden else R.string.home_module_aquarium),
        when (occ.task.type) {
            TaskType.WATERING -> stringResource(R.string.home_check_soil)
            else -> occ.task.itemName ?: occ.task.amountText
        },
        when (occ.status) {
            OccurrenceStatus.DONE -> stringResource(R.string.calendar_completed)
            OccurrenceStatus.SKIPPED -> stringResource(R.string.feeding_skipped)
            else -> null
        },
    ).joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            f.time(occ.task.reminderTime ?: defaultTime),
            style = MaterialTheme.typography.bodyLarge,
            color = ext.mutedText,
            modifier = Modifier.width(64.dp),
        )
        IconTile(taskIcon(occ.task.type, occ.task.maintenanceType), ext.forModule(occ.task.module), size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text(
                occ.task.displayTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = if (occ.isHandled) ext.mutedText else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = if (done) ext.success else ext.mutedText, maxLines = 2)
        }
    }
}
