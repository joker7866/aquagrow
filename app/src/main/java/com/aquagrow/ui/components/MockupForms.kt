package com.aquagrow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import java.time.LocalDate

/** Mockup form style: bold label above a rounded field, placeholder inside. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    supporting: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VisualFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = { if (maxLength == null || it.length <= maxLength) onValueChange(it) },
            placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
            supportingText = (error ?: supporting)?.let { { Text(it) } },
            isError = error != null,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = KeyboardCapitalization.Sentences),
            shape = MaterialTheme.shapes.medium,
            colors = mockupFieldColors(),
            // The visible label sits outside the field, so name the field itself for TalkBack.
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

/** Label drawn above a field; screen readers get it from the field instead, so it is not read twice. */
@Composable
private fun VisualFieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.clearAndSetSemantics {})
}

@Composable
fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
}

@Composable
private fun mockupFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = AquaGrowTheme.colors.cardBorder.copy(alpha = 1f),
    focusedBorderColor = AquaGrowTheme.module.primary,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedPlaceholderColor = AquaGrowTheme.colors.mutedText.copy(alpha = 0.8f),
)

/** Label above a read-only field that opens the date picker. */
@Composable
fun LabeledDateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    maxDate: LocalDate? = null,
    minDate: LocalDate? = null,
    placeholder: String = stringResource(R.string.form_not_set),
) {
    val f = rememberFormatters()
    var open by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) { interaction.interactions.collect { if (it is PressInteraction.Release) open = true } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VisualFieldLabel(label)
        OutlinedTextField(
            value = date?.let(f::date).orEmpty(),
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            supportingText = error?.let { { Text(it) } },
            isError = error != null,
            textStyle = MaterialTheme.typography.bodyLarge,
            interactionSource = interaction,
            shape = MaterialTheme.shapes.medium,
            colors = mockupFieldColors(),
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = label
                onClick {
                    open = true
                    true
                }
            },
        )
    }
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

/** Mockup segmented control: grey track, white raised pill for the selection. */
@Composable
fun <T> PillSegmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp).selectableGroup()) {
            options.forEach { option ->
                val isSelected = option == selected
                val bg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh, label = "seg")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bg,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget)
                        .selectable(selected = isSelected, onClick = { onSelect(option) }, role = Role.RadioButton),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label(option),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else AquaGrowTheme.colors.mutedText,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Dashed "Take or choose a photo" box; shows the photo once one is set. */
@Composable
fun DashedPhotoBox(
    path: String?,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val module = AquaGrowTheme.module
    val dash = module.accent
    Box(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                )
            }
            .padding(2.dp)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AppPhoto(path, R.drawable.ic_image, module, Modifier.fillMaxWidth().height(176.dp), shape = RoundedCornerShape(18.dp))
        } else {
            Surface(color = module.container.copy(alpha = 0.35f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(176.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(R.drawable.ic_photo_camera), null, tint = module.primary, modifier = Modifier.size(36.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge, color = module.primary, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

/** Mockup top bar for forms: close (X) on the left, title, filled "Save" pill on the right. */
@Composable
fun FormTopBar(
    title: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
) {
    val module = AquaGrowTheme.module
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close)) }
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Button(
                    onClick = onSave,
                    enabled = saveEnabled,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = module.primary, contentColor = module.onPrimary),
                    modifier = Modifier.heightIn(min = Dimens.MinTouchTarget).padding(end = 8.dp),
                ) { Text(stringResource(R.string.action_save), style = MaterialTheme.typography.labelLarge) }
            }
            androidx.compose.material3.HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
        }
    }
}

/**
 * Mockup watering box: "Every [N] days" with − / + buttons in one bordered row.
 * [value] null shows a dash so the user must pick (no recommended default).
 */
@Composable
fun IntervalBox(
    prefix: String,
    unit: String,
    value: Int?,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    error: String? = null,
    /** Unit shown after the number, e.g. "day" for 1 (defaults to [unit]). */
    displayUnit: String = unit,
) {
    Column(modifier) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (error != null) MaterialTheme.colorScheme.error else AquaGrowTheme.colors.cardBorder),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotated(prefix, value?.toString() ?: "—", displayUnit),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                StepButton(R.drawable.ic_remove, stringResource(R.string.form_decrease, unit), enabled = value == null || value > range.first) {
                    onValueChange(((value ?: range.first + 1) - 1).coerceIn(range))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                StepButton(R.drawable.ic_add, stringResource(R.string.form_increase, unit), enabled = value == null || value < range.last) {
                    onValueChange(((value ?: range.first - 1) + 1).coerceIn(range))
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}

private fun buildAnnotated(prefix: String, value: String, unit: String) = androidx.compose.ui.text.buildAnnotatedString {
    append("$prefix ")
    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    append(value)
    pop()
    append(" $unit")
}

@Composable
private fun StepButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val module = AquaGrowTheme.module
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = module.container,
        contentColor = module.primary,
        modifier = Modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), description) }
    }
}
