package com.aquagrow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supporting: String? = null,
    error: String? = null,
    required: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    maxLength: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (maxLength == null || it.length <= maxLength) onValueChange(it) },
        label = { Text(if (required) "$label *" else label) },
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = AquaGrowTheme.colors.cardBorder,
            focusedBorderColor = AquaGrowTheme.module.primary,
            focusedLabelColor = AquaGrowTheme.module.primary,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Numeric text field; the caller parses. */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supporting: String? = null,
    decimal: Boolean = true,
) {
    AppTextField(
        value = value,
        onValueChange = { new -> if (new.all { it.isDigit() || (decimal && (it == '.' || it == ',')) }) onValueChange(new.replace(',', '.')) },
        label = label,
        error = error,
        supporting = supporting,
        keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        modifier = modifier,
        maxLength = 9,
    )
}

/** A read-only field that opens a picker when tapped. */
@Composable
private fun PickerField(
    label: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { if (it is PressInteraction.Release) onClick() }
    }
    OutlinedTextField(
        value = text,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        supportingText = error?.let { { Text(it) } },
        isError = error != null,
        trailingIcon = trailing,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = AquaGrowTheme.colors.cardBorder,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    clearable: Boolean = false,
    maxDate: LocalDate? = null,
    minDate: LocalDate? = null,
) {
    val f = rememberFormatters()
    var open by remember { mutableStateOf(false) }
    PickerField(
        label = label,
        text = date?.let(f::date) ?: stringResource(R.string.form_not_set),
        onClick = { open = true },
        error = error,
        modifier = modifier,
        trailing = {
            if (clearable && date != null) {
                IconButton(onClick = { onDateChange(null) }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.form_clear))
                }
            } else {
                Icon(painterResource(R.drawable.ic_event), null)
            }
        },
    )
    if (open) {
        DatePickerModal(
            initial = date ?: LocalDate.now(),
            onDismiss = { open = false },
            onConfirm = {
                onDateChange(it)
                open = false
            },
            maxDate = maxDate,
            minDate = minDate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    maxDate: LocalDate? = null,
    minDate: LocalDate? = null,
) {
    // DatePicker works in UTC millis at midnight.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return (maxDate == null || !d.isAfter(maxDate)) && (minDate == null || !d.isBefore(minDate))
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    time: LocalTime?,
    onTimeChange: (LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
    nullText: String = stringResource(R.string.schedule_time_default),
) {
    val f = rememberFormatters()
    var open by remember { mutableStateOf(false) }
    PickerField(
        label = label,
        text = time?.let(f::time) ?: nullText,
        onClick = { open = true },
        modifier = modifier,
        trailing = {
            if (time != null) {
                IconButton(onClick = { onTimeChange(null) }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.form_clear))
                }
            } else {
                Icon(painterResource(R.drawable.ic_schedule), null)
            }
        },
    )
    if (open) {
        val initial = time ?: LocalTime.of(8, 0)
        val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current))
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onTimeChange(LocalTime.of(state.hour, state.minute))
                    open = false
                }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state = state) },
        )
    }
}

/**
 * − value + stepper. [value] may be null ("Choose"), which is how the app avoids
 * pre-filling a recommended interval.
 */
@Composable
fun Stepper(
    label: String,
    value: Int?,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    valueText: (Int) -> String = { it.toString() },
    error: String? = null,
) {
    val shown = value?.let(valueText) ?: stringResource(R.string.schedule_choose)
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            FilledTonalIconButton(
                onClick = { onValueChange(((value ?: range.first + 1) - 1).coerceIn(range)) },
                enabled = value == null || value > range.first,
            ) { Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.form_decrease, label)) }
            Text(
                shown,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 72.dp)
                    .padding(horizontal = 4.dp)
                    .semantics { stateDescription = shown },
            )
            FilledTonalIconButton(
                onClick = { onValueChange(((value ?: range.first - 1) + 1).coerceIn(range)) },
                enabled = value == null || value < range.last,
            ) { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.form_increase, label)) }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    allowDeselect: Boolean = false,
    onDeselect: () -> Unit = {},
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            AppFilterChip(
                selected = isSelected,
                text = label(option),
                onClick = { if (isSelected && allowDeselect) onDeselect() else onSelect(option) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> MultiChoiceChips(
    options: List<T>,
    selected: Set<T>,
    label: @Composable (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            AppFilterChip(selected = option in selected, text = label(option), onClick = { onToggle(option) })
        }
    }
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Override for the selected fill, e.g. ink for list filters (mockup). */
    selectedColor: androidx.compose.ui.graphics.Color? = null,
    leadingDot: androidx.compose.ui.graphics.Color? = null,
) {
    val module = AquaGrowTheme.module
    val fill = selectedColor ?: module.primary
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = leadingDot?.let { c -> { androidx.compose.foundation.layout.Box(Modifier.size(8.dp).background(c, CircleShape)) } },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = fill,
            selectedLabelColor = if (selectedColor != null) MaterialTheme.colorScheme.surface else module.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AquaGrowTheme.colors.cardBorder,
        ),
        modifier = modifier.heightIn(min = Dimens.MinTouchTarget),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val module = AquaGrowTheme.module
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = module.container,
                    activeContentColor = module.onContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                ),
                icon = {},
                label = { Text(label(option), style = MaterialTheme.typography.labelLarge, maxLines = 1) },
            )
        }
    }
}

@Composable
fun DaysOfWeekPicker(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit, modifier: Modifier = Modifier) {
    val f = rememberFormatters()
    val base = LocalDate.of(2024, 1, 1) // a Monday
    MultiChoiceChips(
        options = DayOfWeek.entries,
        selected = selected,
        label = { f.weekdayShort.format(base.plusDays((it.value - 1).toLong())) },
        onToggle = onToggle,
        modifier = modifier,
    )
}

@Composable
fun FormSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
fun ExpandableSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val title = stringResource(if (expanded) R.string.form_fewer_details else R.string.form_more_details)
    Column(modifier.fillMaxWidth()) {
        TextButton(onClick = onToggle, modifier = Modifier.heightIn(min = Dimens.MinTouchTarget)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = AquaGrowTheme.module.primary)
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                null,
                tint = AquaGrowTheme.module.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
                    .rotate(if (expanded) 90f else 0f),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else AquaGrowTheme.module.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Small round "selected" badge used in pickers and photo selection. */
@Composable
fun SelectionBadge(selected: Boolean, modifier: Modifier = Modifier, description: String? = null) {
    androidx.compose.material3.Surface(
        shape = CircleShape,
        color = if (selected) AquaGrowTheme.module.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        modifier = modifier
            .size(24.dp)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        if (selected) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_check), null, tint = AquaGrowTheme.module.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Title + optional supporting text + switch; the whole row toggles (one TalkBack stop). */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
            }
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = AquaGrowTheme.module.primary,
                checkedThumbColor = AquaGrowTheme.module.onPrimary,
            ),
        )
    }
}

/** Intercepts system back while [enabled] and asks before throwing away edits. */
@Composable
fun DiscardChangesGuard(enabled: Boolean, onDiscard: () -> Unit) {
    var ask by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = enabled) { ask = true }
    if (ask) {
        ConfirmDialog(
            title = stringResource(R.string.form_discard_title),
            body = stringResource(R.string.form_discard_body),
            confirmLabel = stringResource(R.string.form_discard),
            onConfirm = {
                ask = false
                onDiscard()
            },
            onDismiss = { ask = false },
            destructive = true,
        )
    }
}
