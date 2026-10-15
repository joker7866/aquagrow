package com.aquagrow.ui.screens.addplant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.catalog.CatalogPlant
import com.aquagrow.domain.catalog.PlantCatalog
import com.aquagrow.domain.garden.PlantFieldError
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.PlantCondition
import com.aquagrow.domain.model.PlantingMethod
import com.aquagrow.domain.model.SunlightLevel
import com.aquagrow.domain.schedule.ScheduleError
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.schedule.ScheduleKind
import com.aquagrow.ui.components.AppFilterChip
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.DashedPhotoBox
import com.aquagrow.ui.components.DateField
import com.aquagrow.ui.components.DiscardChangesGuard
import com.aquagrow.ui.components.ExpandableSection
import com.aquagrow.ui.components.FieldLabel
import com.aquagrow.ui.components.FormSection
import com.aquagrow.ui.components.FormTopBar
import com.aquagrow.ui.components.IntervalBox
import com.aquagrow.ui.components.LabeledDateField
import com.aquagrow.ui.components.LabeledTextField
import com.aquagrow.ui.components.NumberField
import com.aquagrow.ui.components.PhotoSourceSheet
import com.aquagrow.ui.components.PillSegmented
import com.aquagrow.ui.components.ScheduleEditor
import com.aquagrow.ui.components.SwitchRow
import com.aquagrow.ui.components.TimeField
import com.aquagrow.ui.components.label
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun AddPlantScreen(
    onDone: (savedId: Long?) -> Unit,
    viewModel: AddPlantViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let { onDone(it) } }
    DiscardChangesGuard(enabled = state.dirty) { onDone(null) }
    AddPlantContent(
        state = state,
        onUpdate = viewModel::update,
        onPickCatalog = viewModel::pickCatalog,
        onToggleMore = viewModel::toggleMore,
        onSave = viewModel::save,
        onBack = { onDone(null) },
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPlantContent(
    state: AddPlantUiState,
    onUpdate: ((PlantForm) -> PlantForm) -> Unit,
    onPickCatalog: (CatalogPlant?) -> Unit,
    onToggleMore: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) = ModuleTheme(CareModule.GARDEN) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showCatalog by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var otherSchedules by rememberSaveable { mutableStateOf(false) }
    val f = state.form
    val e = if (state.showErrors) state.errors else emptySet()
    val fixMessage = stringResource(R.string.form_fix_errors)
    LaunchedEffect(state.showErrors, state.errors) {
        if (state.showErrors && state.errors.isNotEmpty()) snackbar.showSnackbar(fixMessage)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            FormTopBar(
                title = stringResource(if (state.isEdit) R.string.edit_plant_title else R.string.add_plant_title),
                onClose = onBack,
                onSave = onSave,
                saveEnabled = !state.saving && !state.loading,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column {
                DashedPhotoBox(f.photoPath, stringResource(R.string.add_plant_photo_prompt), onClick = { showPhotoSheet = true })
                if (f.photoPath != null) {
                    TextButton(onClick = { onUpdate { it.copy(photoPath = null) } }) { Text(stringResource(R.string.photo_remove)) }
                }
            }

            if (!state.isEdit || f.catalogKey != null) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel(stringResource(R.string.add_plant_catalog))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val picks = PlantCatalog.quickPicks.mapNotNull(PlantCatalog::find)
                        val selectedOutside = PlantCatalog.find(f.catalogKey)?.takeIf { it !in picks }
                        (picks + listOfNotNull(selectedOutside)).forEach { entry ->
                            AppFilterChip(f.catalogKey == entry.key, entry.name, { onPickCatalog(if (f.catalogKey == entry.key) null else entry) })
                        }
                        AppFilterChip(false, stringResource(R.string.add_plant_more_plants), { showCatalog = true })
                        AppFilterChip(f.catalogKey == null, stringResource(R.string.add_plant_custom), { onPickCatalog(null) })
                    }
                }
            }

            LabeledTextField(
                label = stringResource(R.string.add_plant_name),
                value = f.name,
                onValueChange = { v -> onUpdate { it.copy(name = v) } },
                maxLength = 80,
                error = when {
                    PlantFieldError.NAME_REQUIRED in e -> stringResource(R.string.error_name_required)
                    PlantFieldError.NAME_TOO_LONG in e -> stringResource(R.string.error_name_too_long)
                    else -> null
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledTextField(
                    stringResource(R.string.add_plant_variety),
                    f.variety,
                    { v -> onUpdate { it.copy(variety = v) } },
                    Modifier.weight(1f),
                    placeholder = stringResource(R.string.add_plant_variety_hint),
                    maxLength = 40,
                )
                LabeledDateField(
                    stringResource(R.string.add_plant_planting_date),
                    f.plantingDate,
                    { d -> if (d != null) onUpdate { it.copy(plantingDate = d) } },
                    Modifier.weight(1f),
                    maxDate = LocalDate.now(),
                    error = if (PlantFieldError.PLANTING_DATE_IN_FUTURE in e) stringResource(R.string.error_planting_future) else null,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledTextField(
                    stringResource(R.string.add_plant_pot_size),
                    f.potSize,
                    { v -> onUpdate { it.copy(potSize = v) } },
                    Modifier.weight(1f),
                    placeholder = stringResource(R.string.add_plant_pot_size_hint),
                    maxLength = 40,
                )
                LabeledTextField(
                    stringResource(R.string.add_plant_location),
                    f.location,
                    { v -> onUpdate { it.copy(location = v) } },
                    Modifier.weight(1f),
                    placeholder = stringResource(R.string.add_plant_location_hint),
                    maxLength = 40,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel(stringResource(R.string.add_plant_sunlight))
                PillSegmented(SunlightLevel.entries, f.sunlight ?: SunlightLevel.FULL_SUN, { it.label() }, { s -> onUpdate { it.copy(sunlight = s) } })
            }

            WateringSection(f, state.showErrors, otherSchedules, { otherSchedules = !otherSchedules }, onUpdate)

            LabeledTextField(
                label = stringResource(R.string.add_plant_notes_optional),
                value = f.notes,
                onValueChange = { v -> onUpdate { it.copy(notes = v) } },
                placeholder = stringResource(R.string.add_plant_notes_hint),
                singleLine = false,
                minLines = 3,
                maxLength = 2000,
            )

            ExpandableSection(expanded = state.moreExpanded, onToggle = onToggleMore) {
                Text(stringResource(R.string.add_plant_more_details_hint), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                FormSection(stringResource(R.string.add_plant_category)) {
                    ChoiceChips(PlantCategory.entries, f.category, { it.label() }, { c -> onUpdate { it.copy(category = c) } })
                }
                if (f.stages.isNotEmpty()) {
                    FormSection(stringResource(R.string.add_plant_stage)) {
                        ChoiceChips(f.stages.indices.toList(), f.currentStage, { f.stages[it].ifBlank { "—" } }, { i -> onUpdate { it.copy(currentStage = i) } })
                    }
                }
                FormSection(stringResource(R.string.add_plant_method)) {
                    ChoiceChips(
                        PlantingMethod.entries,
                        f.method,
                        { it.label() },
                        { m -> onUpdate { it.copy(method = m) } },
                        allowDeselect = true,
                        onDeselect = { onUpdate { it.copy(method = null) } },
                    )
                }
                DateField(
                    label = stringResource(R.string.add_plant_sowing_date),
                    date = f.sowingDate,
                    onDateChange = { d -> onUpdate { it.copy(sowingDate = d) } },
                    clearable = true,
                    maxDate = f.plantingDate,
                    error = if (PlantFieldError.SOWING_AFTER_PLANTING in e) stringResource(R.string.error_sowing_after) else null,
                )
                AppTextField(f.soilMix, { v -> onUpdate { it.copy(soilMix = v) } }, stringResource(R.string.add_plant_soil_mix), placeholder = stringResource(R.string.add_plant_soil_mix_hint))
                FormSection(stringResource(R.string.add_plant_condition)) {
                    ChoiceChips(
                        PlantCondition.entries,
                        f.condition,
                        { it.label() },
                        { c -> onUpdate { it.copy(condition = c) } },
                        allowDeselect = true,
                        onDeselect = { onUpdate { it.copy(condition = null) } },
                    )
                }
                SwitchRow(
                    title = stringResource(R.string.add_plant_fertilizer),
                    checked = f.fertilizerOn,
                    onCheckedChange = { on -> onUpdate { it.copy(fertilizerOn = on) } },
                    supporting = stringResource(R.string.add_plant_fertilizer_help),
                )
                if (f.fertilizerOn) {
                    FormSection(stringResource(R.string.add_plant_fertilizer_type)) {
                        ChoiceChips(FertilizerType.entries, f.fertType, { it.label() }, { t -> onUpdate { it.copy(fertType = t) } })
                    }
                    AppTextField(f.fertProduct, { v -> onUpdate { it.copy(fertProduct = v) } }, stringResource(R.string.add_plant_fertilizer_product))
                    AppTextField(f.fertAmount, { v -> onUpdate { it.copy(fertAmount = v) } }, stringResource(R.string.add_plant_fertilizer_amount))
                    ScheduleEditor(
                        input = f.fertilizer,
                        onChange = { s -> onUpdate { it.copy(fertilizer = s) } },
                        kinds = listOf(ScheduleKind.EVERY_N_DAYS, ScheduleKind.EVERY_N_WEEKS, ScheduleKind.MONTHLY),
                        showError = state.showErrors,
                    )
                }
                StagesEditor(
                    stages = f.stages,
                    onChange = { list -> onUpdate { it.copy(stages = list, currentStage = it.currentStage.coerceIn(0, (list.size - 1).coerceAtLeast(0))) } },
                    error = if (PlantFieldError.STAGES_EMPTY in e) stringResource(R.string.error_stages_empty) else null,
                )
                FormSection(stringResource(R.string.add_plant_harvest)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField(f.harvestMin, { v -> onUpdate { it.copy(harvestMin = v) } }, stringResource(R.string.add_plant_harvest_min), Modifier.weight(1f), decimal = false)
                        NumberField(f.harvestMax, { v -> onUpdate { it.copy(harvestMax = v) } }, stringResource(R.string.add_plant_harvest_max), Modifier.weight(1f), decimal = false)
                    }
                    Text(
                        if (PlantFieldError.HARVEST_RANGE_INVALID in e) stringResource(R.string.error_harvest_range) else stringResource(R.string.add_plant_harvest_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (PlantFieldError.HARVEST_RANGE_INVALID in e) MaterialTheme.colorScheme.error else AquaGrowTheme.colors.mutedText,
                    )
                }
            }
        }
    }

    if (showPhotoSheet) {
        PhotoSourceSheet(
            onPicked = { p ->
                onUpdate { it.copy(photoPath = p) }
                showPhotoSheet = false
            },
            onDismiss = { showPhotoSheet = false },
            onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
        )
    }
    if (showCatalog) {
        ModalBottomSheet(onDismissRequest = { showCatalog = false }) {
            Text(
                stringResource(R.string.add_plant_catalog_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            )
            LazyColumn {
                items(PlantCatalog.all, key = { it.key }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.name) },
                        supportingContent = { Text(entry.category.label()) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            onPickCatalog(entry)
                            showCatalog = false
                        },
                    )
                }
            }
        }
    }
}

/** Mockup: "Watering reminder" label with an on/off switch, then "Every [N] days  − +". */
@Composable
private fun WateringSection(
    f: PlantForm,
    showErrors: Boolean,
    otherSchedules: Boolean,
    onToggleOther: () -> Unit,
    onUpdate: ((PlantForm) -> PlantForm) -> Unit,
) {
    val w = f.watering
    val err = if (showErrors && f.wateringOn) w.validate() else null
    // Errors here are usually below the fold: scroll them into view after a failed Save.
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(err) { if (err != null) requester.bringIntoView() }
    Column(Modifier.bringIntoViewRequester(requester), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { FieldLabel(stringResource(R.string.add_plant_watering)) }
            Switch(
                checked = f.wateringOn,
                onCheckedChange = { on -> onUpdate { it.copy(wateringOn = on) } },
                colors = SwitchDefaults.colors(checkedTrackColor = AquaGrowTheme.module.primary),
            )
        }
        if (f.wateringOn) {
            if (otherSchedules || w.kind != ScheduleKind.EVERY_N_DAYS) {
                ScheduleEditor(input = w, onChange = { s -> onUpdate { it.copy(watering = s) } }, showError = showErrors)
            } else {
                IntervalBox(
                    prefix = stringResource(R.string.add_plant_every),
                    unit = stringResource(R.string.add_plant_days),
                    displayUnit = pluralStringResource(R.plurals.add_plant_days_unit, w.interval ?: 2),
                    value = w.interval,
                    onValueChange = { n -> onUpdate { it.copy(watering = w.copy(interval = n)) } },
                    range = 1..ScheduleInput.MAX_INTERVAL,
                    error = when (err) {
                        ScheduleError.MISSING_INTERVAL -> stringResource(R.string.schedule_error_interval)
                        ScheduleError.INTERVAL_RANGE -> stringResource(R.string.schedule_error_range)
                        else -> null
                    },
                )
                TimeField(
                    label = stringResource(R.string.schedule_time),
                    time = w.time,
                    onTimeChange = { t -> onUpdate { it.copy(watering = w.copy(time = t)) } },
                )
            }
            Text(stringResource(R.string.add_plant_watering_help), style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText)
            if (w.kind == ScheduleKind.EVERY_N_DAYS) {
                TextButton(onClick = onToggleOther) {
                    Text(stringResource(if (otherSchedules) R.string.form_fewer_details else R.string.add_plant_other_schedules))
                }
            }
        }
    }
}

@Composable
private fun StagesEditor(stages: List<String>, onChange: (List<String>) -> Unit, error: String?) {
    FormSection(stringResource(R.string.add_plant_stages)) {
        Text(stringResource(R.string.add_plant_stages_help), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        stages.forEachIndexed { index, stage ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    value = stage,
                    onValueChange = { v -> onChange(stages.toMutableList().also { it[index] = v }) },
                    label = stringResource(R.string.add_plant_stage_name, index + 1),
                    modifier = Modifier.weight(1f),
                    maxLength = 30,
                )
                IconButton(onClick = { onChange(stages.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.add_plant_stage_remove, stage))
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (stages.size < 12) {
            TextButton(onClick = { onChange(stages + "") }) {
                Icon(painterResource(R.drawable.ic_add), null)
                Text(stringResource(R.string.add_plant_stage_add))
            }
        }
    }
}
