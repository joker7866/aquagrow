package com.aquagrow.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.theme.AquaGrowTheme

/**
 * Last 7 days (mockup). Bar height = tasks that day (scaled to the busiest day).
 * Dark green = everything done, sage = some done, light = none done yet / nothing scheduled.
 */
@Composable
fun WeeklyCareCard(state: HomeUiState.Dashboard, modifier: Modifier = Modifier) {
    val f = rememberFormatters()
    val ext = AquaGrowTheme.colors
    val max = (state.week.maxOfOrNull { it.scheduled } ?: 0).coerceAtLeast(1)
    val parts = mutableListOf<String>()
    for (day in state.week) {
        parts += stringResource(R.string.home_week_day_description, f.weekdayShort.format(day.date), day.done, day.scheduled)
    }
    val chartDescription = stringResource(R.string.home_week_chart_description, parts.joinToString(" "))

    AppCard(modifier = modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_week_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                if (state.weekScheduled == 0) stringResource(R.string.home_week_none) else stringResource(R.string.home_week_done, state.weekDone, state.weekScheduled),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.mutedText,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(112.dp)
                .clearAndSetSemantics { contentDescription = chartDescription },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            state.week.forEach { day ->
                val color = when {
                    day.scheduled > 0 && day.done == day.scheduled -> ext.garden.primary
                    day.done > 0 -> ext.garden.accent.copy(alpha = 0.8f)
                    else -> ext.cardBorder
                }
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        val fraction = if (day.scheduled == 0) 0.06f else (day.scheduled.toFloat() / max).coerceAtLeast(0.12f)
                        Surface(shape = RoundedCornerShape(10.dp), color = color, modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction)) {}
                    }
                    Text(
                        f.weekdayNarrow.format(day.date),
                        style = MaterialTheme.typography.labelLarge,
                        color = ext.mutedText,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
