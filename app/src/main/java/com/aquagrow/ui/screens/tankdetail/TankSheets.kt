package com.aquagrow.ui.screens.tankdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.domain.aquarium.FeedingPlan
import com.aquagrow.domain.aquarium.WaterRangeChecker
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.InventoryCategory
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.model.WaterParameter
import com.aquagrow.domain.model.WaterRanges
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.schedule.ScheduleKind
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.DateField
import com.aquagrow.ui.components.FormSection
import com.aquagrow.ui.components.NumberField
import com.aquagrow.ui.components.ScheduleEditor
import com.aquagrow.ui.components.TimeField
import com.aquagrow.ui.components.formatReading
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.components.unit
import com.aquagrow.ui.screens.garden.FormSheet
import com.aquagrow.ui.theme.AquaGrowTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Plausible bounds, only to catch typos (e.g. 250 °C). Not care advice. */
private fun plausible(p: WaterParameter, v: Double): Boolean = when (p) {
    WaterParameter.TEMPERATURE -> v in 0.0..45.0
    WaterParameter.PH -> v in 0.0..14.0
    WaterParameter.AMMONIA, WaterParameter.NITRITE -> v in 0.0..50.0
    WaterParameter.NITRATE -> v in 0.0..500.0
    WaterParameter.GH, WaterParameter.KH -> v in 0.0..60.0
}

@Composable
fun WaterTestSheet(onSave: (WaterTest) -> Unit, onDismiss: () -> Unit) {
    val values = remember { mutableStateOf(WaterParameter.entries.associateWith { "" }) }
    var note by rememberSaveable { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val parsed = values.value.mapValues { (_, v) -> v.toDoubleOrNull() }
    val invalid = values.value.filter { (p, v) -> v.isNotBlank() && (v.toDoubleOrNull()?.let { !plausible(p, it) } ?: true) }.keys
    val any = parsed.values.any { it != null }
    FormSheet(
        title = stringResource(R.string.water_sheet_title),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            tried = true
            if (any && invalid.isEmpty()) {
                onSave(
                    WaterTest(
                        aquariumId = 0, testedAt = Instant.now(),
                        temperatureC = parsed[WaterParameter.TEMPERATURE], ph = parsed[WaterParameter.PH],
                        ammoniaPpm = parsed[WaterParameter.AMMONIA], nitritePpm = parsed[WaterParameter.NITRITE],
                        nitratePpm = parsed[WaterParameter.NITRATE], ghDgh = parsed[WaterParameter.GH], khDkh = parsed[WaterParameter.KH],
                        note = note.trim().ifEmpty { null },
                    ),
                )
            }
        },
        onDismiss = onDismiss,
    ) {
        Text(stringResource(R.string.water_sheet_help), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        WaterParameter.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { p ->
                    NumberField(
                        values.value[p].orEmpty(),
                        { v -> values.value = values.value + (p to v) },
                        listOf(p.label(), p.unit()).filter { it.isNotEmpty() }.joinToString(" "),
                        Modifier.weight(1f),
                        error = if (p in invalid) stringResource(R.string.error_number) else null,
                    )
                }
                if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
        AppTextField(note, { note = it }, stringResource(R.string.add_plant_notes), singleLine = false, maxLength = 500)
        if (tried && !any) Text(stringResource(R.string.water_sheet_need_one), color = MaterialTheme.colorScheme.error)
        if (tried && invalid.isNotEmpty()) Text(stringResource(R.string.water_sheet_invalid), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun RangesSheet(current: WaterRanges, fish: List<Fish>, onSave: (WaterRanges) -> Unit, onDismiss: () -> Unit) {
    fun s(v: Double?) = v?.let(::formatReading).orEmpty()
    var tMin by rememberSaveable { mutableStateOf(s(current.tempMinC)) }
    var tMax by rememberSaveable { mutableStateOf(s(current.tempMaxC)) }
    var pMin by rememberSaveable { mutableStateOf(s(current.phMin)) }
    var pMax by rememberSaveable { mutableStateOf(s(current.phMax)) }
    var amm by rememberSaveable { mutableStateOf(s(current.ammoniaMaxPpm)) }
    var nitrite by rememberSaveable { mutableStateOf(s(current.nitriteMaxPpm)) }
    var nitrate by rememberSaveable { mutableStateOf(s(current.nitrateMaxPpm)) }
    var ghMin by rememberSaveable { mutableStateOf(s(current.ghMin)) }
    var ghMax by rememberSaveable { mutableStateOf(s(current.ghMax)) }
    var khMin by rememberSaveable { mutableStateOf(s(current.khMin)) }
    var khMax by rememberSaveable { mutableStateOf(s(current.khMax)) }
    var noSpeciesMsg by remember { mutableStateOf(false) }
    val minL = stringResource(R.string.ranges_min)
    val maxL = stringResource(R.string.ranges_max)
    val alertL = stringResource(R.string.ranges_alert_max)

    FormSheet(
        title = stringResource(R.string.ranges_title),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            onSave(
                WaterRanges(
                    tMin.toDoubleOrNull(), tMax.toDoubleOrNull(), pMin.toDoubleOrNull(), pMax.toDoubleOrNull(),
                    amm.toDoubleOrNull(), nitrite.toDoubleOrNull(), nitrate.toDoubleOrNull(),
                    ghMin.toDoubleOrNull(), ghMax.toDoubleOrNull(), khMin.toDoubleOrNull(), khMax.toDoubleOrNull(),
                ),
            )
        },
        onDismiss = onDismiss,
    ) {
        Text(stringResource(R.string.ranges_help), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        TextButton(onClick = {
            val d = WaterRangeChecker.defaultsFromSpecies(fish)
            if (d.tempMinC == null && d.phMin == null) {
                noSpeciesMsg = true
            } else {
                noSpeciesMsg = false
                tMin = s(d.tempMinC)
                tMax = s(d.tempMaxC)
                pMin = s(d.phMin)
                pMax = s(d.phMax)
            }
        }) { Text(stringResource(R.string.ranges_from_species)) }
        if (noSpeciesMsg) Text(stringResource(R.string.ranges_from_species_none), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.onWarningContainer)
        TextButton(onClick = {
            val g = WaterRangeChecker.GENERAL_REFERENCE
            amm = s(g.ammoniaMaxPpm)
            nitrite = s(g.nitriteMaxPpm)
            nitrate = s(g.nitrateMaxPpm)
        }) { Text(stringResource(R.string.ranges_general)) }

        @Composable
        fun pair(p: WaterParameter, a: String, onA: (String) -> Unit, b: String, onB: (String) -> Unit) {
            FormSection("${p.label()} ${p.unit()}".trim()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(a, onA, minL, Modifier.weight(1f))
                    NumberField(b, onB, maxL, Modifier.weight(1f))
                }
            }
        }
        pair(WaterParameter.TEMPERATURE, tMin, { tMin = it }, tMax, { tMax = it })
        pair(WaterParameter.PH, pMin, { pMin = it }, pMax, { pMax = it })
        FormSection("${stringResource(R.string.param_ammonia)} / ${stringResource(R.string.param_nitrite)} / ${stringResource(R.string.param_nitrate)} (${stringResource(R.string.unit_ppm)})") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(amm, { amm = it }, "${stringResource(R.string.param_ammonia)} · $alertL", Modifier.weight(1f))
                NumberField(nitrite, { nitrite = it }, "${stringResource(R.string.param_nitrite)} · $alertL", Modifier.weight(1f))
            }
            NumberField(nitrate, { nitrate = it }, "${stringResource(R.string.param_nitrate)} · $alertL")
        }
        pair(WaterParameter.GH, ghMin, { ghMin = it }, ghMax, { ghMax = it })
        pair(WaterParameter.KH, khMin, { khMin = it }, khMax, { khMax = it })
    }
}

/**
 * One feeding schedule for one target: the whole tank, or one fish group.
 * [existing] are the current feeding tasks for that target (used to pre-fill).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedingScheduleSheet(
    fish: List<Fish>,
    existingForTarget: (Long?) -> List<CareTask>,
    initialTarget: Long?,
    onSave: (FeedingPlan) -> Unit,
    onRemove: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val f = rememberFormatters()
    var target by remember { mutableStateOf(initialTarget) }
    val existing = existingForTarget(target)
    var food by remember(target) { mutableStateOf(existing.firstOrNull()?.itemName.orEmpty()) }
    var portion by remember(target) { mutableStateOf(existing.firstOrNull()?.amountText.orEmpty()) }
    var times by remember(target) { mutableStateOf(existing.mapNotNull { it.reminderTime }.ifEmpty { listOf(LocalTime.of(8, 0)) }) }
    var schedule by remember(target) {
        mutableStateOf(
            existing.firstOrNull()?.let(ScheduleInput::fromTask)?.copy(time = null)
                ?: ScheduleInput(kind = ScheduleKind.DAILY, startDate = LocalDate.now()),
        )
    }
    var adding by remember { mutableStateOf(false) }
    var newTime by remember { mutableStateOf<LocalTime?>(null) }
    var tried by remember { mutableStateOf(false) }
    val wholeTank = stringResource(R.string.feeding_whole_tank)

    FormSheet(
        title = stringResource(R.string.feeding_sheet_title),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            tried = true
            if (times.isNotEmpty() && schedule.validate() == null) {
                onSave(
                    FeedingPlan(
                        fishId = target,
                        fishName = fish.firstOrNull { it.id == target }?.displayName,
                        foodName = food,
                        portionText = portion,
                        times = times,
                        rule = schedule.toRule(),
                        startDate = schedule.startDate,
                    ),
                )
            }
        },
        onDismiss = onDismiss,
    ) {
        FormSection(stringResource(R.string.feeding_for)) {
            ChoiceChips(listOf<Long?>(null) + fish.map { it.id }, target, { id -> id?.let { i -> fish.first { it.id == i }.displayName } ?: wholeTank }, { target = it })
        }
        AppTextField(food, { food = it }, stringResource(R.string.feeding_food), placeholder = stringResource(R.string.feeding_food_hint))
        AppTextField(portion, { portion = it }, stringResource(R.string.feeding_portion), placeholder = stringResource(R.string.feeding_portion_hint))
        Text(stringResource(R.string.feeding_portion_help), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        FormSection(stringResource(R.string.feeding_times)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                times.sorted().forEach { t ->
                    AssistChip(
                        onClick = { times = times - t },
                        label = { Text(f.time(t)) },
                        trailingIcon = { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.feeding_remove_time, f.time(t))) },
                    )
                }
                if (times.size < 6) {
                    AssistChip(onClick = { adding = true }, label = { Text(stringResource(R.string.feeding_add_time)) }, leadingIcon = { Icon(painterResource(R.drawable.ic_add), null) })
                }
            }
            if (adding) {
                TimeField(stringResource(R.string.feeding_add_time), newTime, { t ->
                    if (t != null && t !in times) times = times + t
                    adding = false
                    newTime = null
                })
            }
            if (tried && times.isEmpty()) Text(stringResource(R.string.feeding_need_time), color = MaterialTheme.colorScheme.error)
        }
        ScheduleEditor(
            schedule,
            { schedule = it },
            kinds = listOf(ScheduleKind.DAILY, ScheduleKind.EVERY_N_DAYS, ScheduleKind.WEEKLY),
            showError = tried,
        )
        if (existing.isNotEmpty()) {
            TextButton(onClick = { onRemove(target) }) { Text(stringResource(R.string.feeding_remove_schedule), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun LogFeedingSheet(fish: List<Fish>, onSave: (Long?, String?, String?, String?) -> Unit, onDismiss: () -> Unit) {
    var target by remember { mutableStateOf<Long?>(null) }
    var food by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val wholeTank = stringResource(R.string.feeding_whole_tank)
    FormSheet(
        title = stringResource(R.string.feeding_log_sheet),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = { onSave(target, food.trim().ifEmpty { null }, amount.trim().ifEmpty { null }, note.trim().ifEmpty { null }) },
        onDismiss = onDismiss,
    ) {
        FormSection(stringResource(R.string.feeding_for)) {
            ChoiceChips(listOf<Long?>(null) + fish.map { it.id }, target, { id -> id?.let { i -> fish.first { it.id == i }.displayName } ?: wholeTank }, { target = it })
        }
        AppTextField(food, { food = it }, stringResource(R.string.feeding_food), placeholder = stringResource(R.string.feeding_food_hint))
        AppTextField(amount, { amount = it }, stringResource(R.string.feeding_amount))
        AppTextField(note, { note = it }, stringResource(R.string.add_plant_notes), singleLine = false)
    }
}

/** Target of a maintenance schedule: a maintenance type, or water testing. */
sealed interface MaintTarget {
    data class Type(val type: MaintenanceType) : MaintTarget
    data object WaterTest : MaintTarget
}

@Composable
fun MaintTarget.label(): String = when (this) {
    is MaintTarget.Type -> type.label()
    MaintTarget.WaterTest -> stringResource(R.string.maint_water_testing)
}

@Composable
fun MaintenanceScheduleSheet(
    existing: List<CareTask>,
    initial: MaintTarget?,
    onSave: (MaintTarget, ScheduleInput?) -> Unit,
    onDismiss: () -> Unit,
) {
    val targets = listOf<MaintTarget>(
        MaintTarget.Type(MaintenanceType.WATER_CHANGE),
        MaintTarget.WaterTest,
        MaintTarget.Type(MaintenanceType.FILTER_INSPECTION),
        MaintTarget.Type(MaintenanceType.FILTER_MAINTENANCE),
        MaintTarget.Type(MaintenanceType.GLASS_CLEANING),
        MaintTarget.Type(MaintenanceType.GRAVEL_VACUUM),
        MaintTarget.Type(MaintenanceType.EQUIPMENT_CHECK),
        MaintTarget.Type(MaintenanceType.PLANT_TRIMMING),
    )
    var target by remember { mutableStateOf(initial ?: targets.first()) }
    fun taskFor(t: MaintTarget) = existing.firstOrNull {
        when (t) {
            MaintTarget.WaterTest -> it.type == TaskType.WATER_TEST
            is MaintTarget.Type -> it.type == TaskType.MAINTENANCE && it.maintenanceType == t.type
        }
    }
    var schedule by remember(target) {
        mutableStateOf(taskFor(target)?.let(ScheduleInput::fromTask) ?: ScheduleInput(kind = ScheduleKind.EVERY_N_WEEKS, startDate = LocalDate.now()))
    }
    var tried by remember { mutableStateOf(false) }
    FormSheet(
        title = stringResource(R.string.maint_schedule_sheet),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            tried = true
            if (schedule.validate() == null) onSave(target, schedule)
        },
        onDismiss = onDismiss,
    ) {
        FormSection(stringResource(R.string.maint_what)) {
            ChoiceChips(targets, target, { it.label() }, { target = it })
        }
        ScheduleEditor(
            schedule,
            { schedule = it },
            kinds = listOf(ScheduleKind.EVERY_N_DAYS, ScheduleKind.WEEKLY, ScheduleKind.EVERY_N_WEEKS, ScheduleKind.MONTHLY),
            showError = tried,
        )
        if (taskFor(target) != null) {
            TextButton(onClick = { onSave(target, null) }) { Text(stringResource(R.string.feeding_remove_schedule), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun LogMaintenanceSheet(initial: MaintenanceType?, onSave: (MaintenanceType, Int?, String?) -> Unit, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(initial ?: MaintenanceType.WATER_CHANGE) }
    var percent by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val pct = percent.toIntOrNull()
    val pctValid = percent.isBlank() || (pct != null && pct in 1..100)
    FormSheet(
        title = stringResource(R.string.maint_log_sheet),
        confirmLabel = stringResource(R.string.action_save),
        confirmEnabled = pctValid,
        onConfirm = { onSave(type, pct.takeIf { type == MaintenanceType.WATER_CHANGE }, note.trim().ifEmpty { null }) },
        onDismiss = onDismiss,
    ) {
        FormSection(stringResource(R.string.maint_what)) {
            ChoiceChips(MaintenanceType.entries, type, { it.label() }, { type = it })
        }
        if (type == MaintenanceType.WATER_CHANGE) {
            NumberField(percent, { percent = it }, stringResource(R.string.maint_percent), decimal = false, error = if (!pctValid) stringResource(R.string.error_number) else null)
        }
        AppTextField(note, { note = it }, stringResource(R.string.add_plant_notes), singleLine = false)
    }
}

@Composable
fun EquipmentSheet(onSave: (String, InventoryCategory, LocalDate?, String?) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf(InventoryCategory.EQUIPMENT) }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    FormSheet(
        title = stringResource(R.string.equipment_sheet_title),
        confirmLabel = stringResource(R.string.action_save),
        confirmEnabled = name.isNotBlank(),
        onConfirm = { onSave(name.trim(), category, date, notes.trim().ifEmpty { null }) },
        onDismiss = onDismiss,
    ) {
        AppTextField(name, { name = it }, stringResource(R.string.equipment_name), required = true, placeholder = stringResource(R.string.equipment_name_hint))
        FormSection(stringResource(R.string.equipment_category)) {
            ChoiceChips(
                listOf(InventoryCategory.EQUIPMENT, InventoryCategory.FILTER_MEDIA, InventoryCategory.TEST_KIT, InventoryCategory.WATER_CONDITIONER, InventoryCategory.FISH_FOOD),
                category,
                { it.label() },
                { category = it },
            )
        }
        DateField(stringResource(R.string.equipment_purchase), date, { date = it }, clearable = true, maxDate = LocalDate.now())
        AppTextField(notes, { notes = it }, stringResource(R.string.equipment_notes), singleLine = false)
    }
}
