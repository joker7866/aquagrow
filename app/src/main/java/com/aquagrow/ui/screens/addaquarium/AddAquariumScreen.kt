package com.aquagrow.ui.screens.addaquarium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.TankType
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.DateField
import com.aquagrow.ui.components.DiscardChangesGuard
import com.aquagrow.ui.components.ExpandableSection
import com.aquagrow.ui.components.FormSection
import com.aquagrow.ui.components.NumberField
import com.aquagrow.ui.components.PhotoField
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.SwitchRow
import com.aquagrow.ui.components.label
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun AddAquariumScreen(onDone: (Long?) -> Unit, viewModel: AddAquariumViewModel = hiltViewModel()) = ModuleTheme(CareModule.AQUARIUM) {
    val res = LocalResources.current
    LaunchedEffect(Unit) { viewModel.suggestName { n -> res.getString(R.string.tank_default_name, n) } }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedId) { state.savedId?.let { onDone(it) } }
    DiscardChangesGuard(state.dirty) { onDone(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val f = state.form
    val e = if (state.showErrors) f.errors() else emptySet()
    val update = viewModel::update

    Scaffold(
        containerColor = AquaGrowTheme.module.pageBackground,
        topBar = { AppTopBar(stringResource(if (state.isEdit) R.string.edit_tank_title else R.string.add_tank_title), { onDone(null) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
                    PrimaryButton(
                        stringResource(R.string.add_tank_save),
                        viewModel::save,
                        enabled = !state.saving && !state.loading,
                        modifier = Modifier.navigationBarsPadding().imePadding().padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
                    )
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PhotoField(
                f.photoPath,
                { p -> update { it.copy(photoPath = p) } },
                R.drawable.ic_fish,
                Modifier.fillMaxWidth(),
                onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
            )
            AppTextField(
                f.name,
                { v -> update { it.copy(name = v) } },
                stringResource(R.string.add_tank_name),
                required = true,
                placeholder = stringResource(R.string.add_tank_name_hint),
                maxLength = 60,
                error = if (TankField.NAME in e) stringResource(R.string.error_tank_name) else null,
            )
            FormSection(stringResource(R.string.add_tank_dimensions)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dimErr = stringResource(R.string.error_dimension)
                    NumberField(f.length, { v -> update { it.copy(length = v) } }, stringResource(R.string.add_tank_length), Modifier.weight(1f), error = if (TankField.LENGTH in e) dimErr else null)
                    NumberField(f.width, { v -> update { it.copy(width = v) } }, stringResource(R.string.add_tank_width), Modifier.weight(1f), error = if (TankField.WIDTH in e) dimErr else null)
                    NumberField(f.height, { v -> update { it.copy(height = v) } }, stringResource(R.string.add_tank_height), Modifier.weight(1f), error = if (TankField.HEIGHT in e) dimErr else null)
                }
            }
            NumberField(
                f.volume,
                viewModel::onVolumeTyped,
                stringResource(R.string.add_tank_volume),
                error = if (TankField.VOLUME in e) stringResource(R.string.error_volume) else null,
                supporting = stringResource(if (f.volumeManual) R.string.add_tank_volume_manual else R.string.add_tank_volume_auto),
            )
            if (f.volumeManual && f.calculatedVolume != null) {
                TextButton(onClick = viewModel::resetVolume) { Text(stringResource(R.string.add_tank_volume_reset)) }
            }
            FormSection(stringResource(R.string.add_tank_type)) {
                ChoiceChips(TankType.entries, f.tankType, { it.label() }, { t -> update { it.copy(tankType = t) } })
            }
            DateField(stringResource(R.string.add_tank_setup_date), f.setupDate, { d -> update { it.copy(setupDate = d) } }, clearable = true, maxDate = LocalDate.now())

            ExpandableSection(state.moreExpanded, viewModel::toggleMore) {
                AppTextField(f.filter, { v -> update { it.copy(filter = v) } }, stringResource(R.string.add_tank_filter), placeholder = stringResource(R.string.add_tank_filter_hint))
                AppTextField(f.heater, { v -> update { it.copy(heater = v) } }, stringResource(R.string.add_tank_heater), placeholder = stringResource(R.string.add_tank_heater_hint))
                AppTextField(f.lighting, { v -> update { it.copy(lighting = v) } }, stringResource(R.string.add_tank_lighting))
                AppTextField(f.substrate, { v -> update { it.copy(substrate = v) } }, stringResource(R.string.add_tank_substrate))
                SwitchRow(stringResource(R.string.add_tank_live_plants), f.livePlants, { on -> update { it.copy(livePlants = on) } })
                if (f.livePlants) AppTextField(f.livePlantsNote, { v -> update { it.copy(livePlantsNote = v) } }, stringResource(R.string.add_tank_live_plants_note))
                AppTextField(f.equipment, { v -> update { it.copy(equipment = v) } }, stringResource(R.string.add_tank_equipment), singleLine = false)
                AppTextField(f.location, { v -> update { it.copy(location = v) } }, stringResource(R.string.add_tank_location))
                AppTextField(f.notes, { v -> update { it.copy(notes = v) } }, stringResource(R.string.add_plant_notes), singleLine = false, minLines = 3, maxLength = 2000)
            }
        }
    }
}
