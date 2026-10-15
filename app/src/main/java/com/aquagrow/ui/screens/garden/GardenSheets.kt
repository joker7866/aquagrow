package com.aquagrow.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.domain.model.FertilizerMethod
import com.aquagrow.domain.model.FertilizerType
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.PlantCondition
import com.aquagrow.domain.model.SoilMoisture
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.DateField
import com.aquagrow.ui.components.FormSection
import com.aquagrow.ui.components.NumberField
import com.aquagrow.ui.components.PhotoField
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.SegmentedControl
import com.aquagrow.ui.components.SwitchRow
import com.aquagrow.ui.components.label
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import java.time.LocalDate

/** Bottom sheet scaffold: title, scrollable body, primary action pinned at the bottom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
            PrimaryButton(confirmLabel, onConfirm, enabled = confirmEnabled, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

data class WateringEntry(val watered: Boolean, val moisture: SoilMoisture?, val amountMl: Int?, val note: String?)

@Composable
fun RecordWateringSheet(
    plantName: String?,
    onSave: (WateringEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var moisture by rememberSaveable { mutableStateOf<SoilMoisture?>(null) }
    var watered by rememberSaveable { mutableStateOf(true) }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    FormSheet(
        title = plantName?.let { "${stringResource(R.string.watering_sheet_title)} · $it" } ?: stringResource(R.string.watering_sheet_title),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = { onSave(WateringEntry(watered, moisture, amount.toIntOrNull(), note)) },
        onDismiss = onDismiss,
    ) {
        Text(stringResource(R.string.watering_check_soil), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        FormSection(stringResource(R.string.watering_sheet_soil)) {
            ChoiceChips(
                SoilMoisture.entries,
                moisture,
                { it.label() },
                { m ->
                    moisture = m
                    // Wet soil: default to "didn't water", the user can still change it.
                    if (m == SoilMoisture.WET) {
                        watered = false
                    } else if (m == SoilMoisture.DRY) {
                        watered = true
                    }
                },
                allowDeselect = true,
                onDeselect = { moisture = null },
            )
            if (moisture == SoilMoisture.WET) {
                Text(stringResource(R.string.watering_sheet_wet_hint), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.onWarningContainer)
            }
        }
        SegmentedControl(
            options = listOf(true, false),
            selected = watered,
            label = { stringResource(if (it) R.string.watering_sheet_watered else R.string.watering_sheet_skipped) },
            onSelect = { watered = it },
        )
        if (watered) NumberField(amount, { amount = it }, stringResource(R.string.watering_sheet_amount), decimal = false)
        AppTextField(note, { note = it }, stringResource(R.string.add_plant_notes), singleLine = false, maxLength = 500)
    }
}

data class GrowthEntry(
    val stageIndex: Int?,
    val heightCm: Double?,
    val condition: PlantCondition?,
    val note: String?,
    val photoPath: String?,
)

@Composable
fun GrowthLogSheet(plant: Plant, onSave: (GrowthEntry) -> Unit, onDismiss: () -> Unit) {
    var stage by rememberSaveable { mutableIntStateOf(plant.currentStageIndex) }
    var height by rememberSaveable { mutableStateOf("") }
    var condition by rememberSaveable { mutableStateOf(plant.condition) }
    var note by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    val heightValid = height.isBlank() || (height.toDoubleOrNull()?.let { it > 0 } == true)
    FormSheet(
        title = stringResource(R.string.growth_sheet_title),
        confirmLabel = stringResource(R.string.action_save),
        confirmEnabled = heightValid,
        onConfirm = {
            onSave(GrowthEntry(stage.takeIf { it != plant.currentStageIndex }, height.toDoubleOrNull(), condition, note, photo))
        },
        onDismiss = onDismiss,
    ) {
        PhotoField(photo, { photo = it }, R.drawable.ic_sprout, Modifier.fillMaxWidth())
        if (plant.growthStages.isNotEmpty()) {
            FormSection(stringResource(R.string.plant_growth_stage)) {
                ChoiceChips(plant.growthStages.indices.toList(), stage, { plant.growthStages[it] }, { stage = it })
            }
        }
        NumberField(
            height,
            { height = it },
            stringResource(R.string.growth_sheet_height),
            error = if (!heightValid) stringResource(R.string.error_positive) else null,
        )
        FormSection(stringResource(R.string.add_plant_condition)) {
            ChoiceChips(PlantCondition.entries, condition, { it.label() }, { condition = it }, allowDeselect = true, onDeselect = { condition = null })
        }
        AppTextField(note, { note = it }, stringResource(R.string.growth_sheet_note), singleLine = false, minLines = 2, maxLength = 1000)
    }
}

data class FertilizerEntry(
    val plantId: Long,
    val type: FertilizerType,
    val product: String?,
    val quantity: Double?,
    val unit: String?,
    val method: FertilizerMethod?,
    val note: String?,
    val nextDate: LocalDate?,
)

/** [plants] non-null = let the user choose the plant (Fertilizer tool). */
@Composable
fun FertilizerLogSheet(
    plantId: Long?,
    plants: List<Plant>?,
    onSave: (FertilizerEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPlant by rememberSaveable { mutableStateOf(plantId) }
    var type by rememberSaveable { mutableStateOf(FertilizerType.COMPOST) }
    var product by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf<FertilizerMethod?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var remind by rememberSaveable { mutableStateOf(false) }
    var nextDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(14)) }
    val qtyValid = quantity.isBlank() || (quantity.toDoubleOrNull()?.let { it > 0 } == true)
    FormSheet(
        title = stringResource(R.string.fert_sheet_title),
        confirmLabel = stringResource(R.string.action_save),
        confirmEnabled = selectedPlant != null && qtyValid,
        onConfirm = {
            onSave(
                FertilizerEntry(
                    selectedPlant!!,
                    type,
                    product.trim().ifEmpty { null },
                    quantity.toDoubleOrNull(),
                    unit.trim().ifEmpty { null },
                    method,
                    note.trim().ifEmpty { null },
                    if (remind) nextDate else null,
                ),
            )
        },
        onDismiss = onDismiss,
    ) {
        Text(stringResource(R.string.fertilizer_label_note), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        if (plants != null) {
            FormSection(stringResource(R.string.fert_sheet_plant)) {
                ChoiceChips(plants.map { it.id }, selectedPlant, { id -> plants.first { it.id == id }.name }, { selectedPlant = it })
            }
        }
        FormSection(stringResource(R.string.add_plant_fertilizer_type)) {
            ChoiceChips(FertilizerType.entries, type, { it.label() }, { type = it })
        }
        AppTextField(product, { product = it }, stringResource(R.string.add_plant_fertilizer_product))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                quantity,
                { quantity = it },
                stringResource(R.string.fert_sheet_quantity),
                Modifier.weight(1f),
                error = if (!qtyValid) stringResource(R.string.error_positive) else null,
            )
            AppTextField(unit, { unit = it }, stringResource(R.string.fert_sheet_unit), Modifier.weight(1f), placeholder = stringResource(R.string.fert_sheet_unit_hint), maxLength = 12)
        }
        FormSection(stringResource(R.string.fert_sheet_method)) {
            ChoiceChips(FertilizerMethod.entries, method, { it.label() }, { method = it }, allowDeselect = true, onDeselect = { method = null })
        }
        AppTextField(note, { note = it }, stringResource(R.string.add_plant_notes), singleLine = false, maxLength = 500)
        SwitchRow(stringResource(R.string.fert_sheet_next), remind, { remind = it })
        if (remind) {
            DateField(
                stringResource(R.string.schedule_start),
                nextDate,
                { d -> d?.let { nextDate = it } },
                minDate = LocalDate.now().plusDays(1),
            )
        }
    }
}

data class HarvestEntry(val quantity: Double, val unit: String, val date: LocalDate, val note: String?)

@Composable
fun HarvestLogSheet(onSave: (HarvestEntry) -> Unit, onDismiss: () -> Unit) {
    var quantity by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("g") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var note by rememberSaveable { mutableStateOf("") }
    val q = quantity.toDoubleOrNull()
    val valid = q != null && q > 0
    FormSheet(
        title = stringResource(R.string.harvest_sheet_title),
        confirmLabel = stringResource(R.string.action_save),
        confirmEnabled = valid && unit.isNotBlank(),
        onConfirm = { onSave(HarvestEntry(q!!, unit.trim(), date, note.trim().ifEmpty { null })) },
        onDismiss = onDismiss,
    ) {
        NumberField(
            quantity,
            { quantity = it },
            stringResource(R.string.harvest_sheet_quantity),
            error = if (quantity.isNotBlank() && !valid) stringResource(R.string.error_positive) else null,
        )
        FormSection(stringResource(R.string.harvest_sheet_unit)) {
            ChoiceChips(listOf("g", "kg", "pcs", "bunch"), unit, { it }, { unit = it })
        }
        DateField(stringResource(R.string.harvest_sheet_date), date, { d -> d?.let { date = it } }, maxDate = LocalDate.now())
        AppTextField(note, { note = it }, stringResource(R.string.add_plant_notes), singleLine = false, maxLength = 500)
    }
}
