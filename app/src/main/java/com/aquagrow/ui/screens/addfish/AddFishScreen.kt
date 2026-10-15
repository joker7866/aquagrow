package com.aquagrow.ui.screens.addfish

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.aquagrow.domain.aquarium.CompatibilityWarning
import com.aquagrow.domain.catalog.CatalogFish
import com.aquagrow.domain.catalog.FishCatalog
import com.aquagrow.domain.model.CareModule
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppFilterChip
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.ConfirmDialog
import com.aquagrow.ui.components.DateField
import com.aquagrow.ui.components.DiscardChangesGuard
import com.aquagrow.ui.components.ExpandableSection
import com.aquagrow.ui.components.FormSection
import com.aquagrow.ui.components.NumberField
import com.aquagrow.ui.components.PhotoField
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.Stepper
import com.aquagrow.ui.components.formatReading
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.text
import com.aquagrow.ui.screens.plantdetail.InfoGrid
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import java.time.LocalDate

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddFishScreen(onDone: () -> Unit, viewModel: AddFishViewModel = hiltViewModel()) = ModuleTheme(CareModule.AQUARIUM) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    DiscardChangesGuard(state.dirty) { onDone() }
    var confirmDelete by remember { mutableStateOf(false) }
    val f = state.form
    val e = if (state.showErrors) f.errors() else emptySet()
    val update = viewModel::update

    Scaffold(
        containerColor = AquaGrowTheme.module.pageBackground,
        topBar = {
            AppTopBar(stringResource(if (state.isEdit) R.string.edit_fish_title else R.string.add_fish_title), onDone) {
                if (state.isEdit) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.action_delete)) }
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
                    PrimaryButton(
                        stringResource(R.string.add_fish_save),
                        viewModel::save,
                        enabled = !state.saving && state.tanks.isNotEmpty(),
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
            if (state.tanks.isEmpty()) {
                Text(stringResource(R.string.add_fish_no_tank), color = AquaGrowTheme.colors.onWarningContainer)
                return@Column
            }
            PhotoField(f.photoPath, { p -> update { it.copy(photoPath = p) } }, R.drawable.ic_fish, Modifier.fillMaxWidth())
            FormSection(stringResource(R.string.add_fish_species)) {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FishCatalog.all.forEach { c ->
                        AppFilterChip(f.speciesKey == c.key, c.name, { viewModel.pickSpecies(c.key) })
                    }
                    AppFilterChip(f.speciesKey == null, stringResource(R.string.add_fish_custom), { viewModel.pickSpecies(null) })
                }
                if (FishField.SPECIES in e && f.speciesKey != null) Text(stringResource(R.string.error_species), color = MaterialTheme.colorScheme.error)
            }
            if (f.speciesKey == null) {
                AppTextField(
                    f.customSpecies,
                    { v -> update { it.copy(customSpecies = v) } },
                    stringResource(R.string.add_fish_custom_species),
                    required = true,
                    maxLength = 60,
                    error = if (FishField.SPECIES in e) stringResource(R.string.error_species) else null,
                )
            }
            f.catalog?.let { ReferenceCard(it) }
            AppTextField(f.nickname, { v -> update { it.copy(nickname = v) } }, stringResource(R.string.add_fish_nickname), placeholder = stringResource(R.string.add_fish_nickname_hint), maxLength = 40)
            FormSection(stringResource(R.string.add_fish_tank) + " *") {
                ChoiceChips(state.tanks.map { it.id }, f.aquariumId, { id -> state.tanks.first { it.id == id }.name }, { id -> update { it.copy(aquariumId = id) } })
                if (FishField.TANK in e) Text(stringResource(R.string.error_tank_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Stepper(
                stringResource(R.string.add_fish_quantity),
                f.count,
                { n -> update { it.copy(count = n) } },
                1..999,
                error = if (FishField.COUNT in e) stringResource(R.string.error_count) else null,
            )
            if (state.warnings.isNotEmpty()) CompatibilityCard(state.warnings)
            DateField(stringResource(R.string.add_fish_date), f.addedDate, { d -> update { it.copy(addedDate = d) } }, clearable = true, maxDate = LocalDate.now())

            ExpandableSection(state.moreExpanded, viewModel::toggleMore) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(f.age, { v -> update { it.copy(age = v) } }, stringResource(R.string.add_fish_age), Modifier.weight(1f), placeholder = stringResource(R.string.add_fish_age_hint))
                    NumberField(f.sizeText, { v -> update { it.copy(sizeText = v) } }, stringResource(R.string.add_fish_size), Modifier.weight(1f), error = if (FishField.SIZE in e) stringResource(R.string.error_positive) else null)
                }
                AppTextField(f.feeding, { v -> update { it.copy(feeding = v) } }, stringResource(R.string.add_fish_feeding), singleLine = false)
                AppTextField(f.compatNotes, { v -> update { it.copy(compatNotes = v) } }, stringResource(R.string.add_fish_compat_notes), singleLine = false)
                AppTextField(
                    f.health,
                    { v -> update { it.copy(health = v) } },
                    stringResource(R.string.add_fish_health),
                    placeholder = stringResource(R.string.add_fish_health_hint),
                    singleLine = false,
                    minLines = 2,
                )
                AppTextField(f.careNotes, { v -> update { it.copy(careNotes = v) } }, stringResource(R.string.add_fish_care_notes), singleLine = false, minLines = 2)
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.action_delete),
            f.speciesName,
            stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

@Composable
fun ReferenceCard(c: CatalogFish) {
    AppCard(Modifier.fillMaxWidth(), color = AquaGrowTheme.module.surfaceAlt) {
        Text(stringResource(R.string.fish_reference) + " · " + c.scientificName, style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.fish_reference_note), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
        InfoGrid(
            listOf(
                stringResource(R.string.fish_temperament) to c.temperament.label(),
                stringResource(R.string.fish_adult_size) to stringResource(R.string.fish_size_cm, formatReading(c.adultSizeCm)),
                stringResource(R.string.fish_temp_range) to "${formatReading(c.tempMinC)}–${formatReading(c.tempMaxC)} °C",
                stringResource(R.string.fish_ph_range) to "${formatReading(c.phMin)}–${formatReading(c.phMax)}",
                stringResource(R.string.fish_min_tank) to stringResource(R.string.fish_min_liters, c.minTankLiters),
                stringResource(R.string.fish_group) to if (c.minGroup > 1) stringResource(R.string.fish_group_n, c.minGroup) else stringResource(R.string.fish_group_alone),
            ),
        )
        Text(stringResource(R.string.fish_diet) + ": " + c.diet, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        c.careNotes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun CompatibilityCard(warnings: List<CompatibilityWarning>) {
    val ext = AquaGrowTheme.colors
    Surface(shape = MaterialTheme.shapes.large, color = ext.warningContainer, contentColor = ext.onWarningContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_info), null)
                Text(" " + stringResource(R.string.compat_title), style = MaterialTheme.typography.titleSmall)
            }
            warnings.forEach { Text("• " + it.text(), style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(R.string.compat_footer), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
