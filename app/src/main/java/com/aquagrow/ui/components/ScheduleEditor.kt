package com.aquagrow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.RecurrenceType
import com.aquagrow.domain.schedule.ScheduleError
import com.aquagrow.domain.schedule.ScheduleInput
import com.aquagrow.domain.schedule.ScheduleKind
import java.time.LocalDate

/** Picker for how often something repeats. Used by every scheduling screen. */
@Composable
fun ScheduleEditor(
    input: ScheduleInput,
    onChange: (ScheduleInput) -> Unit,
    modifier: Modifier = Modifier,
    kinds: List<ScheduleKind> = listOf(ScheduleKind.DAILY, ScheduleKind.EVERY_N_DAYS, ScheduleKind.WEEKLY, ScheduleKind.EVERY_N_WEEKS),
    showError: Boolean = false,
    minStartDate: LocalDate? = null,
) {
    val error = if (showError) input.validate() else null
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceChips(
            options = kinds,
            selected = input.kind,
            label = { it.label() },
            onSelect = { kind ->
                // Keep the user's interval when switching between interval kinds; never invent one.
                onChange(input.copy(kind = kind, interval = if (kind == ScheduleKind.MONTHLY && input.interval == null) 1 else input.interval))
            },
        )
        val intervalLabel = when (input.kind) {
            ScheduleKind.EVERY_N_DAYS -> R.string.schedule_interval_days
            ScheduleKind.EVERY_N_WEEKS -> R.string.schedule_interval_weeks
            ScheduleKind.MONTHLY -> R.string.schedule_interval_months
            else -> null
        }
        if (intervalLabel != null) {
            Stepper(
                label = stringResource(intervalLabel),
                value = input.interval,
                onValueChange = { onChange(input.copy(interval = it)) },
                range = 1..ScheduleInput.MAX_INTERVAL,
                error = when (error) {
                    ScheduleError.MISSING_INTERVAL -> stringResource(R.string.schedule_error_interval)
                    ScheduleError.INTERVAL_RANGE -> stringResource(R.string.schedule_error_range)
                    else -> null
                },
            )
        }
        if (input.kind == ScheduleKind.WEEKLY || input.kind == ScheduleKind.EVERY_N_WEEKS) {
            FormSection(stringResource(R.string.schedule_days)) {
                DaysOfWeekPicker(
                    selected = input.daysOfWeek,
                    onToggle = { d -> onChange(input.copy(daysOfWeek = if (d in input.daysOfWeek) input.daysOfWeek - d else input.daysOfWeek + d)) },
                )
                if (error == ScheduleError.NO_DAYS) {
                    androidx.compose.material3.Text(
                        stringResource(R.string.schedule_error_days),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateField(
                label = stringResource(R.string.schedule_start),
                date = input.startDate,
                onDateChange = { it?.let { d -> onChange(input.copy(startDate = d)) } },
                minDate = minStartDate,
                modifier = Modifier.weight(1f),
            )
            TimeField(
                label = stringResource(R.string.schedule_time),
                time = input.time,
                onTimeChange = { onChange(input.copy(time = it)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ScheduleKind.label(): String = stringResource(
    when (this) {
        ScheduleKind.ONCE -> R.string.schedule_once
        ScheduleKind.DAILY -> R.string.schedule_daily
        ScheduleKind.EVERY_N_DAYS -> R.string.schedule_every_n_days
        ScheduleKind.WEEKLY -> R.string.schedule_weekly
        ScheduleKind.EVERY_N_WEEKS -> R.string.schedule_every_n_weeks
        ScheduleKind.MONTHLY -> R.string.schedule_monthly
    },
)

/** "Every 3 days at 8:00 AM", "Weekly on Mon, Thu", … */
@Composable
fun CareTask.scheduleSummary(): String {
    val f = rememberFormatters()
    val r = recurrence
    val base = LocalDate.of(2024, 1, 1)
    val days = r.daysOfWeek.sorted().joinToString(", ") { f.weekdayShort.format(base.plusDays((it.value - 1).toLong())) }
    val text = when (r.type) {
        RecurrenceType.ONCE -> stringResource(R.string.schedule_once_on, f.weekdayDayMonth.format(startDate))

        RecurrenceType.DAILY -> pluralStringResource(R.plurals.schedule_every_days, r.interval, r.interval)

        RecurrenceType.WEEKLY -> if (r.interval == 1) {
            stringResource(R.string.schedule_weekly_on, days.ifEmpty { f.weekdayShort.format(startDate) })
        } else {
            stringResource(R.string.schedule_every_weeks_on, pluralStringResource(R.plurals.schedule_every_weeks, r.interval, r.interval), days)
        }

        RecurrenceType.MONTHLY -> pluralStringResource(R.plurals.schedule_every_months, r.interval, r.interval)
    }
    return reminderTime?.let { stringResource(R.string.schedule_at_time, text, f.time(it)) } ?: text
}
