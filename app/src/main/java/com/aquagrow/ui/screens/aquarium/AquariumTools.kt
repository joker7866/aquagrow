package com.aquagrow.ui.screens.aquarium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aquagrow.R
import com.aquagrow.domain.model.AquariumSummary
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.FeedingLog
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.MaintenanceLog
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.LogFeedingUseCase
import com.aquagrow.domain.usecase.LogMaintenanceUseCase
import com.aquagrow.domain.usecase.LogWaterTestUseCase
import com.aquagrow.domain.usecase.OccurrenceActions
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.ChipTone
import com.aquagrow.ui.components.RescheduleDialog
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.StatusChip
import com.aquagrow.ui.components.TipCard
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.screens.garden.OccurrenceCard
import com.aquagrow.ui.screens.garden.PlantPickerSheet
import com.aquagrow.ui.screens.tankdetail.LogFeedingSheet
import com.aquagrow.ui.screens.tankdetail.LogMaintenanceSheet
import com.aquagrow.ui.screens.tankdetail.WaterTestSheet
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AquariumToolsState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val tanks: List<AquariumSummary> = emptyList(),
    val fish: List<Fish> = emptyList(),
    val occurrences: List<TaskOccurrence> = emptyList(),
    val feedingLogs: List<FeedingLog> = emptyList(),
    val maintenanceLogs: List<MaintenanceLog> = emptyList(),
)

/** Shared by the Feeding, Water tests and Maintenance tool screens. */
@HiltViewModel
class AquariumToolsViewModel @Inject constructor(
    private val repo: AquariumRepository,
    tasks: TaskRepository,
    private val actions: OccurrenceActions,
    private val logFeeding: LogFeedingUseCase,
    private val logWaterTest: LogWaterTestUseCase,
    private val logMaintenance: LogMaintenanceUseCase,
    private val time: TimeProvider,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AquariumToolsState> = time.observeToday().flatMapLatest { today ->
        combine(
            combine(repo.observeSummaries(), repo.observeActiveFish(), ::Pair),
            tasks.observeOverdue(today),
            tasks.observeOccurrences(today, today.plusDays(14)),
            repo.observeRecentFeeding(30),
            repo.observeAllMaintenanceLogs(),
        ) { (tanks, fish), overdue, upcoming, feeding, maint ->
            AquariumToolsState(
                loading = false,
                today = today,
                tanks = tanks,
                fish = fish,
                occurrences = (overdue + upcoming).filter { it.task.module == CareModule.AQUARIUM }.distinctBy { it.task.id to it.originalDate },
                feedingLogs = feeding,
                maintenanceLogs = maint.take(30),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AquariumToolsState())

    fun done(o: TaskOccurrence) = viewModelScope.launch { actions.markDone(o.task.id, o.originalDate) }
    fun skip(o: TaskOccurrence) = viewModelScope.launch { actions.skip(o.task.id, o.originalDate) }
    fun undo(o: TaskOccurrence) = viewModelScope.launch { actions.undo(o.task.id, o.originalDate) }
    fun move(o: TaskOccurrence, d: LocalDate) = viewModelScope.launch { actions.reschedule(o.task.id, o.originalDate, d) }

    fun feed(tankId: Long, fishId: Long?, food: String?, amount: String?, note: String?) = viewModelScope.launch {
        logFeeding(FeedingLog(aquariumId = tankId, fishId = fishId, fedAt = time.now(), foodName = food, amount = amount, note = note))
    }

    fun test(tankId: Long, test: WaterTest) = viewModelScope.launch { logWaterTest(test.copy(aquariumId = tankId)) }

    fun maintain(tankId: Long, type: MaintenanceType, pct: Int?, note: String?) = viewModelScope.launch {
        logMaintenance(MaintenanceLog(aquariumId = tankId, performedAt = time.now(), type = type, waterChangePercent = pct, note = note))
    }
}

@Composable
private fun ToolScaffold(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) = ModuleTheme(CareModule.AQUARIUM) {
    Scaffold(containerColor = AquaGrowTheme.module.pageBackground, topBar = { AppTopBar(title, onBack) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** Ask which tank first when there are several; skip the question when there is one. */
@Composable
private fun rememberTankPicker(tanks: List<AquariumSummary>, onPicked: (Long) -> Unit): () -> Unit {
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        PlantPickerSheet(tanks.map { it.aquarium.id to it.aquarium.name }, {
            onPicked(it.first)
            picking = false
        }, { picking = false }, stringResource(R.string.feeding_choose_tank))
    }
    return {
        if (tanks.size == 1) {
            onPicked(tanks.first().aquarium.id)
        } else if (tanks.isNotEmpty()) {
            picking = true
        }
    }
}

@Composable
fun FeedingScreen(onBack: () -> Unit, vm: AquariumToolsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()
    var logTank by remember { mutableStateOf<Long?>(null) }
    val pick = rememberTankPicker(s.tanks) { logTank = it }
    val feedings = s.occurrences.filter { it.task.type == TaskType.FEEDING && it.date == s.today }
    ToolScaffold(stringResource(R.string.feeding_title), onBack) {
        item { TipCard(stringResource(R.string.home_tip_title), stringResource(R.string.feeding_portion_help)) }
        item {
            SectionHeader(stringResource(R.string.feeding_today), actionLabel = stringResource(R.string.tank_log_feeding), onAction = pick)
        }
        if (feedings.isEmpty()) item { Text(stringResource(R.string.feeding_none_today), color = AquaGrowTheme.colors.mutedText) }
        items(feedings, key = { "${it.task.id}-${it.originalDate}" }) { o ->
            val tank = s.tanks.firstOrNull { it.aquarium.id == o.task.aquariumId }?.aquarium?.name
            AppCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(listOfNotNull(o.task.reminderTime?.let(f::time), o.task.subjectName ?: tank).joinToString(" · "), style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull(tank.takeIf { o.task.fishId != null }, o.task.itemName, o.task.amountText).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                    }
                    when (o.status) {
                        OccurrenceStatus.PENDING -> {
                            TextButton({ vm.skip(o) }) { Text(stringResource(R.string.action_skip)) }
                            TextButton({ vm.done(o) }) { Text(stringResource(R.string.action_done)) }
                        }

                        else -> {
                            StatusChip(stringResource(if (o.status == OccurrenceStatus.DONE) R.string.feeding_fed else R.string.feeding_skipped), if (o.status == OccurrenceStatus.DONE) ChipTone.SUCCESS else ChipTone.NEUTRAL)
                            TextButton({ vm.undo(o) }) { Text(stringResource(R.string.action_undo)) }
                        }
                    }
                }
            }
        }
        item { SectionHeader(stringResource(R.string.feeding_history)) }
        if (s.feedingLogs.isEmpty()) item { Text(stringResource(R.string.feeding_history_none), color = AquaGrowTheme.colors.mutedText) }
        items(s.feedingLogs, key = { "log-${it.id}" }) { log ->
            val tank = s.tanks.firstOrNull { it.aquarium.id == log.aquariumId }?.aquarium?.name
            val fish = s.fish.firstOrNull { it.id == log.fishId }?.displayName
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    listOfNotNull(stringResource(if (log.skipped) R.string.feeding_skipped else R.string.feeding_fed), fish ?: tank, log.foodName).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                val dt = log.fedAt.atZone(zone)
                Text("${f.dayMonth.format(dt)} ${f.time(dt.toLocalTime())}", style = MaterialTheme.typography.labelSmall, color = AquaGrowTheme.colors.mutedText)
            }
        }
    }
    logTank?.let { id ->
        LogFeedingSheet(s.fish.filter { it.aquariumId == id }, { fish, food, amount, note ->
            vm.feed(id, fish, food, amount, note)
            logTank = null
        }, { logTank = null })
    }
}

@Composable
fun WaterTestsScreen(onBack: () -> Unit, onOpenTank: (Long) -> Unit, vm: AquariumToolsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var testTank by remember { mutableStateOf<Long?>(null) }
    ToolScaffold(stringResource(R.string.water_tests_title), onBack) {
        item { Text(stringResource(R.string.water_tests_choose), color = AquaGrowTheme.colors.mutedText) }
        val due = s.occurrences.filter { it.task.type == TaskType.WATER_TEST && !it.isHandled && !it.date.isAfter(s.today) }
        items(s.tanks, key = { it.aquarium.id }) { summary ->
            val (status, _) = tankStatus(summary)
            TankCard(TankCardUi(summary, status, emptyList())) { onOpenTank(summary.aquarium.id) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (due.any { it.task.aquariumId == summary.aquarium.id }) StatusChip(stringResource(R.string.maint_due), ChipTone.WARNING)
                TextButton(onClick = { testTank = summary.aquarium.id }) { Text(stringResource(R.string.tank_log_water_test)) }
            }
        }
    }
    testTank?.let { id ->
        WaterTestSheet({
            vm.test(id, it)
            testTank = null
        }, { testTank = null })
    }
}

@Composable
fun MaintenanceScreen(onBack: () -> Unit, vm: AquariumToolsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()
    var logTank by remember { mutableStateOf<Long?>(null) }
    var moving by remember { mutableStateOf<TaskOccurrence?>(null) }
    val pick = rememberTankPicker(s.tanks) { logTank = it }
    val list = s.occurrences.filter { (it.task.type == TaskType.MAINTENANCE || it.task.type == TaskType.WATER_TEST) && !it.isHandled }
    ToolScaffold(stringResource(R.string.maint_title), onBack) {
        item { SectionHeader(stringResource(R.string.fertilizer_upcoming), actionLabel = stringResource(R.string.tank_log_maintenance), onAction = pick) }
        if (list.isEmpty()) item { Text(stringResource(R.string.tank_maintenance_none), color = AquaGrowTheme.colors.mutedText) }
        items(list, key = { "${it.task.id}-${it.originalDate}" }) { o -> OccurrenceCard(o, s.today, vm::done, { moving = it }, vm::undo) }

        // Completion over the last 30 days, from occurrence history (real data only).
        item { SectionHeader(stringResource(R.string.tank_history)) }
        if (s.maintenanceLogs.isEmpty()) item { Text(stringResource(R.string.feeding_history_none), color = AquaGrowTheme.colors.mutedText) }
        items(s.maintenanceLogs, key = { "m-${it.id}" }) { log ->
            val tank = s.tanks.firstOrNull { it.aquarium.id == log.aquariumId }?.aquarium?.name
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(listOfNotNull(log.type.label(), log.waterChangePercent?.let { "$it%" }, tank).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(f.dayMonth.format(log.performedAt.atZone(zone)), style = MaterialTheme.typography.labelSmall, color = AquaGrowTheme.colors.mutedText)
            }
        }
    }
    logTank?.let { id ->
        LogMaintenanceSheet(null, { t, p, n ->
            vm.maintain(id, t, p, n)
            logTank = null
        }, { logTank = null })
    }
    moving?.let { o ->
        RescheduleDialog(s.today, o.date, {
            vm.move(o, it)
            moving = null
        }, { moving = null })
    }
}
