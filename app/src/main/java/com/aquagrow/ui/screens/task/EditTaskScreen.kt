package com.aquagrow.ui.screens.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.schedule.ScheduleKind
import com.aquagrow.ui.components.ChoiceChips
import com.aquagrow.ui.components.ConfirmDialog
import com.aquagrow.ui.components.DateField
import com.aquagrow.ui.components.DiscardChangesGuard
import com.aquagrow.ui.components.FieldLabel
import com.aquagrow.ui.components.FormTopBar
import com.aquagrow.ui.components.LabeledTextField
import com.aquagrow.ui.components.PillSegmented
import com.aquagrow.ui.components.ScheduleEditor
import com.aquagrow.ui.components.taskTypeLabel
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme

@Composable
fun EditTaskScreen(onDone: () -> Unit, viewModel: EditTaskViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.done) { if (s.done) onDone() }
    DiscardChangesGuard(s.dirty) { onDone() }
    var confirmDelete by remember { mutableStateOf(false) }
    val f = s.form
    val e = if (s.showErrors) f.errors() else emptySet()
    val nothing = stringResource(R.string.task_field_none)

    ModuleTheme(f.module) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = { FormTopBar(stringResource(if (s.isEdit) R.string.task_edit_title else R.string.task_new_title), onDone, viewModel::save) },
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LabeledTextField(
                    stringResource(R.string.task_field_title),
                    f.title,
                    { v -> viewModel.update { it.copy(title = v) } },
                    placeholder = stringResource(R.string.task_field_title_hint),
                    maxLength = 80,
                    error = if (TaskField.TITLE in e) stringResource(R.string.error_task_title) else null,
                )
                if (s.modules.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FieldLabel(stringResource(R.string.task_field_module))
                        PillSegmented(
                            s.modules,
                            f.module,
                            { stringResource(if (it == CareModule.GARDEN) R.string.calendar_garden else R.string.calendar_aquarium) },
                            { m -> viewModel.update { it.copy(module = m, type = TaskType.CUSTOM) } },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel(stringResource(R.string.task_field_type))
                    val types = listOf(TaskType.CUSTOM) + TaskType.entries.filter { it.module == f.module }
                    ChoiceChips(types, f.type, { stringResource(taskTypeLabel(it)) }, { t -> viewModel.update { it.copy(type = t) } })
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel(stringResource(R.string.task_field_link))
                    if (f.module == CareModule.GARDEN) {
                        ChoiceChips(listOf<Long?>(null) + s.plants.map { it.id }, f.plantId, { id -> id?.let { i -> s.plants.first { it.id == i }.name } ?: nothing }, { id -> viewModel.update { it.copy(plantId = id) } })
                    } else {
                        ChoiceChips(listOf<Long?>(null) + s.tanks.map { it.id }, f.aquariumId, { id -> id?.let { i -> s.tanks.first { it.id == i }.name } ?: nothing }, { id -> viewModel.update { it.copy(aquariumId = id) } })
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel(stringResource(R.string.task_field_repeat))
                    ScheduleEditor(
                        f.schedule,
                        { sch -> viewModel.update { it.copy(schedule = sch) } },
                        kinds = listOf(ScheduleKind.ONCE, ScheduleKind.DAILY, ScheduleKind.EVERY_N_DAYS, ScheduleKind.WEEKLY, ScheduleKind.EVERY_N_WEEKS, ScheduleKind.MONTHLY),
                        showError = s.showErrors,
                    )
                }
                if (f.schedule.kind != ScheduleKind.ONCE) {
                    DateField(
                        stringResource(R.string.task_field_end),
                        f.endDate,
                        { d -> viewModel.update { it.copy(endDate = d) } },
                        clearable = true,
                        minDate = f.schedule.startDate,
                        error = if (TaskField.END in e) stringResource(R.string.error_task_end) else null,
                    )
                }
                LabeledTextField(stringResource(R.string.task_field_notes), f.notes, { v -> viewModel.update { it.copy(notes = v) } }, singleLine = false, minLines = 3, maxLength = 1000)
                if (s.isEdit) {
                    TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.task_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.task_delete),
            stringResource(R.string.task_delete_body),
            stringResource(R.string.action_delete),
            {
                confirmDelete = false
                viewModel.delete()
            },
            { confirmDelete = false },
            destructive = true,
        )
    }
}
