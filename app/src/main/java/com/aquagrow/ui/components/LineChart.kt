package com.aquagrow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aquagrow.ui.theme.AquaGrowTheme

data class ChartPoint(val x: Float, val y: Float)

/**
 * Minimal line chart: line + points, optional dashed alert line, first/last x labels.
 * x values are typically epoch days. The whole chart is one accessibility node described
 * by [description] (screen readers should not walk individual points).
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    description: String,
    modifier: Modifier = Modifier,
    lineColor: Color = AquaGrowTheme.module.primary,
    alertLines: List<Float> = emptyList(),
    alertColor: Color = AquaGrowTheme.colors.onWarningContainer,
    height: Dp = 160.dp,
    startLabel: String? = null,
    endLabel: String? = null,
    yLabelFormatter: (Float) -> String = { formatReading(it.toDouble()) },
) {
    val grid = AquaGrowTheme.colors.cardBorder
    val muted = AquaGrowTheme.colors.mutedText
    val sorted = points.sortedBy { it.x }
    val values = sorted.map { it.y } + alertLines
    val minY = (values.minOrNull() ?: 0f).let { if (it > 0f && (values.maxOrNull() ?: 0f) - it > it * 0.2f) it * 0.9f else minOf(0f, it) }
    val maxY = (values.maxOrNull() ?: 1f).let { if (it == minY) it + 1f else it * 1.1f }
    val minX = sorted.firstOrNull()?.x ?: 0f
    val maxX = sorted.lastOrNull()?.x?.takeIf { it > minX } ?: (minX + 1f)

    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Row {
            Column(Modifier.padding(end = 6.dp)) {
                Text(yLabelFormatter(maxY), style = MaterialTheme.typography.labelSmall, color = muted)
                androidx.compose.foundation.layout.Spacer(Modifier.height(height - 32.dp))
                Text(yLabelFormatter(minY), style = MaterialTheme.typography.labelSmall, color = muted)
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(height),
            ) {
                fun px(p: ChartPoint) = Offset(
                    x = (p.x - minX) / (maxX - minX) * size.width,
                    y = size.height - (p.y - minY) / (maxY - minY) * size.height,
                )
                // Horizontal grid lines.
                for (i in 0..3) {
                    val y = size.height * i / 3f
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                alertLines.forEach { a ->
                    val y = size.height - (a - minY) / (maxY - minY) * size.height
                    drawLine(
                        alertColor,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                }
                if (sorted.size >= 2) {
                    val path = Path().apply {
                        sorted.forEachIndexed { i, p -> px(p).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
                    }
                    drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                sorted.forEach { p ->
                    val o = px(p)
                    val over = alertLines.isNotEmpty() && (p.y > alertLines.max() || (alertLines.size > 1 && p.y < alertLines.min()))
                    drawCircle(if (over) alertColor else lineColor, radius = 4.dp.toPx(), center = o)
                }
            }
        }
        if (startLabel != null || endLabel != null) {
            Row(Modifier.fillMaxWidth().padding(start = 28.dp, top = 4.dp)) {
                Text(startLabel.orEmpty(), style = MaterialTheme.typography.labelSmall, color = muted, modifier = Modifier.weight(1f))
                Text(endLabel.orEmpty(), style = MaterialTheme.typography.labelSmall, color = muted)
            }
        }
    }
}
