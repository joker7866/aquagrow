package com.aquagrow.ui.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.ConfigurationCompat
import com.aquagrow.R
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.TaskOccurrence
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatters that follow the device locale and its 12/24-hour setting. */
class AppFormatters(locale: Locale, is24Hour: Boolean) {
    val time: DateTimeFormatter = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", locale)
    val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", locale)
    val weekdayDayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", locale)
    val fullDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
    val dayMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    val weekdayShort: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", locale)
    val weekdayNarrow: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEEE", locale)

    fun time(t: LocalTime): String = time.format(t)
    fun date(d: LocalDate): String = dayMonthYear.format(d)
}

/** The UI locale, read so composables update when the user changes language. */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun currentLocale(): Locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.ROOT

@Composable
fun rememberFormatters(): AppFormatters {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.ROOT
    val is24 = DateFormat.is24HourFormat(context)
    return remember(locale, is24) { AppFormatters(locale, is24) }
}

/** "Water · Tomato" — the task title plus whatever it is linked to. */
fun CareTask.displayTitle(): String = subjectName?.let { "$title · $it" } ?: title

/** Meta line for task rows: reminder time, or overdue note. */
@Composable
fun TaskOccurrence.metaText(today: LocalDate, f: AppFormatters): String = when {
    isOverdue(today) -> stringResource(R.string.task_overdue_since, f.weekdayDayMonth.format(date))
    task.reminderTime != null -> f.time(task.reminderTime)
    else -> stringResource(R.string.task_any_time)
}

/**
 * Home / calendar meta line (mockup): "Garden · 7:00 AM · check soil first",
 * "Aquarium · 8:00 AM · flakes", "Garden · overdue by 1 day".
 */
@Composable
fun TaskOccurrence.homeMeta(today: LocalDate, f: AppFormatters): String {
    val module = stringResource(
        if (task.module == com.aquagrow.domain.model.CareModule.GARDEN) R.string.home_module_garden else R.string.home_module_aquarium,
    )
    if (isOverdue(today)) {
        val days = java.time.temporal.ChronoUnit.DAYS.between(date, today).toInt()
        return "$module · " + androidx.compose.ui.res.pluralStringResource(R.plurals.home_overdue_by, days, days)
    }
    val extra = when (task.type) {
        com.aquagrow.domain.model.TaskType.WATERING -> stringResource(R.string.home_check_soil)
        else -> task.itemName
    }
    return listOfNotNull(module, task.reminderTime?.let(f::time), extra).joinToString(" · ")
}
