package com.aquagrow.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.analytics.Completion
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.ChartPoint
import com.aquagrow.ui.components.LineChart
import com.aquagrow.ui.components.PillSegmented
import com.aquagrow.ui.components.currentLocale
import com.aquagrow.ui.components.formatMoney
import com.aquagrow.ui.components.formatReading
import com.aquagrow.ui.components.label
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import java.time.ZoneId

/** Simple vertical bars scaled to the largest value; one accessibility node for the whole chart. */
@Composable
fun BarChart(values: List<Float>, labels: List<String>, description: String, modifier: Modifier = Modifier, color: Color = AquaGrowTheme.module.primary) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            values.forEach { v ->
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (v > 0) color else AquaGrowTheme.colors.cardBorder,
                        modifier = Modifier.fillMaxWidth().fillMaxHeight((v / max).coerceAtLeast(0.04f)),
                    ) {}
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { l -> Text(l, style = MaterialTheme.typography.labelSmall, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.weight(1f), maxLines = 1) }
        }
    }
}

@Composable
private fun StatCardBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun NoData() = Text(stringResource(R.string.analytics_no_data), style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText)

@Composable
private fun CompletionBlock(title: String, c: Completion) = StatCardBlock(title) {
    val pct = c.percent
    if (pct == null) {
        NoData()
    } else {
        Text(stringResource(R.string.analytics_percent, pct), style = MaterialTheme.typography.headlineMedium)
        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = AquaGrowTheme.module.primary, trackColor = AquaGrowTheme.module.container)
        Text(stringResource(R.string.analytics_done_of, c.done, c.total), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
    }
}

@Composable
fun AnalyticsScreen(onBack: () -> Unit, vm: AnalyticsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()
    val tabs = AnalyticsTab.entries.filter { (it != AnalyticsTab.GARDEN || s.gardenEnabled) && (it != AnalyticsTab.AQUARIUM || s.aquariumEnabled) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { AppTopBar(stringResource(R.string.analytics_title), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tabs.size > 1) {
                item {
                    PillSegmented(tabs, s.tab, {
                        stringResource(
                            when (it) {
                                AnalyticsTab.OVERALL -> R.string.analytics_overall
                                AnalyticsTab.GARDEN -> R.string.analytics_garden
                                AnalyticsTab.AQUARIUM -> R.string.analytics_aquarium
                            },
                        )
                    }, vm::setTab)
                }
            }
            when (s.tab) {
                AnalyticsTab.OVERALL -> {
                    item {
                        StatCardBlock(stringResource(R.string.analytics_weekly)) {
                            if (s.weekly.all { it.total == 0 }) {
                                NoData()
                            } else {
                                BarChart(
                                    values = s.weekly.map { if (it.total == 0) 0f else it.done * 100f / it.total },
                                    labels = s.weekly.map { f.dayMonth.format(it.weekStart).substringBefore(' ') },
                                    description = s.weekly.joinToString("; ") { "${f.dayMonth.format(it.weekStart)}: ${it.done}/${it.total}" },
                                    modifier = Modifier.height(140.dp),
                                )
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                StatCardBlock(stringResource(R.string.analytics_streak)) {
                                    Text(pluralStringResource(R.plurals.analytics_streak_days, s.streak, s.streak), style = MaterialTheme.typography.headlineSmall)
                                    Text(stringResource(R.string.analytics_streak_help), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                                }
                            }
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                StatCardBlock(stringResource(R.string.analytics_upcoming)) {
                                    Text(pluralStringResource(R.plurals.analytics_upcoming_count, s.upcoming, s.upcoming), style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }
                    item {
                        StatCardBlock(stringResource(R.string.analytics_monthly_expenses)) {
                            if (s.monthlyExpenses.all { it.second == 0L }) {
                                NoData()
                            } else {
                                BarChart(
                                    values = s.monthlyExpenses.map { it.second.toFloat() },
                                    labels = s.monthlyExpenses.map { it.first.month.getDisplayName(java.time.format.TextStyle.SHORT, currentLocale()) },
                                    description = s.monthlyExpenses.joinToString("; ") { "${it.first}: ${formatMoney(it.second, s.currency)}" },
                                    modifier = Modifier.height(120.dp),
                                )
                                Text(formatMoney(s.monthlyExpenses.last().second, s.currency), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }

                AnalyticsTab.GARDEN -> {
                    item { StatCardBlock(stringResource(R.string.analytics_active_plants)) { Text(s.activePlants.toString(), style = MaterialTheme.typography.headlineMedium) } }
                    item { CompletionBlock(stringResource(R.string.analytics_watering), s.watering) }
                    item {
                        StatCardBlock(stringResource(R.string.analytics_growth)) {
                            val g = s.growthProgress
                            if (g == null) {
                                NoData()
                            } else {
                                Text(stringResource(R.string.analytics_percent, g), style = MaterialTheme.typography.headlineMedium)
                                LinearProgressIndicator(progress = { g / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = AquaGrowTheme.colors.garden.primary, trackColor = AquaGrowTheme.colors.garden.container)
                                Text(stringResource(R.string.analytics_growth_help), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                            }
                        }
                    }
                    item {
                        StatCardBlock(stringResource(R.string.analytics_fertilizer)) {
                            if (s.fertilizerByType.isEmpty()) NoData() else s.fertilizerByType.forEach { (type, n) -> Text(stringResource(R.string.analytics_by_type, type.label(), n), style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                    item {
                        StatCardBlock(stringResource(R.string.analytics_harvest)) {
                            if (s.harvestTotals.isEmpty()) NoData() else Text(s.harvestTotals.joinToString("  ·  ") { "${formatReading(it.second)} ${it.first}" }, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    item {
                        StatCardBlock(stringResource(R.string.analytics_conditions)) {
                            if (s.conditions.isEmpty()) NoData() else s.conditions.forEach { (c, n) -> Text(stringResource(R.string.analytics_by_type, c.label(), n), style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                }

                AnalyticsTab.AQUARIUM -> {
                    item {
                        StatCardBlock(stringResource(R.string.analytics_population)) {
                            if (s.population.isEmpty()) {
                                NoData()
                            } else {
                                Text(s.fishTotal.toString(), style = MaterialTheme.typography.headlineMedium)
                                s.population.forEach { (name, n) -> Text(stringResource(R.string.analytics_by_type, name, n), style = MaterialTheme.typography.bodyLarge) }
                            }
                        }
                    }
                    item { CompletionBlock(stringResource(R.string.analytics_feeding), s.feeding) }
                    item { CompletionBlock(stringResource(R.string.analytics_maintenance), s.maintenance) }
                    item {
                        StatCardBlock(stringResource(R.string.analytics_water_trends)) {
                            val withData = s.tanks.filter { it.nitrate.size >= 2 }
                            if (withData.isEmpty()) NoData()
                            withData.forEach { h ->
                                Text("${h.tank.name} · ${stringResource(R.string.param_nitrate)}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                LineChart(
                                    points = h.nitrate.mapIndexed { i, t -> ChartPoint(i.toFloat(), t.nitratePpm!!.toFloat()) },
                                    alertLines = listOfNotNull(h.tank.ranges.nitrateMaxPpm?.toFloat()),
                                    height = 110.dp,
                                    description = h.nitrate.joinToString("; ") { "${f.dayMonth.format(it.testedAt.atZone(zone))} ${formatReading(it.nitratePpm!!)}" },
                                )
                            }
                        }
                    }
                    item {
                        StatCardBlock(stringResource(R.string.analytics_tank_history)) {
                            if (s.tanks.isEmpty()) NoData()
                            s.tanks.forEach { h ->
                                Text(h.tank.name, style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.analytics_tank_history_line, h.tests, h.maintenance, h.feedings), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(bottom = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
