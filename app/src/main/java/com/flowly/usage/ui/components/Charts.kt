package com.flowly.usage.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flowly.usage.data.DayTotal
import com.flowly.usage.data.MonthUsage
import com.flowly.usage.data.Transport
import com.flowly.usage.ui.chartDateLabel

@Composable
fun DailyBarChart(
    daily: List<DayTotal>,
    transport: Transport,
    modifier: Modifier = Modifier,
) {
    val compact = daily.size > 10
    val labels = daily.mapIndexed { index, day ->
        val showLabel = !compact || (daily.lastIndex - index) % 5 == 0
        if (showLabel) chartDateLabel(day.date, compact) else ""
    }
    BarChart(
        values = daily.map { it.bytes(transport) },
        labels = labels,
        modifier = modifier,
    )
}

@Composable
fun MonthlyBarChart(
    months: List<MonthUsage>,
    transport: Transport,
    modifier: Modifier = Modifier,
) {
    BarChart(
        values = months.map { it.bytes(transport) },
        labels = months.map { "${it.month}月" },
        modifier = modifier,
    )
}

@Composable
private fun BarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp,
) {
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val animation = remember { Animatable(0f) }

    LaunchedEffect(values) {
        animation.snapTo(0f)
        animation.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow),
        )
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val barWidth = (slot * 0.46f).coerceAtLeast(6.dp.toPx())
            val baseY = size.height - 2.dp.toPx()

            drawLine(
                color = track,
                start = Offset(0f, baseY),
                end = Offset(size.width, baseY),
                strokeWidth = 1.dp.toPx(),
            )

            values.forEachIndexed { index, value ->
                val ratio = value.toFloat() / maxValue
                val height = (size.height * 0.86f) * ratio * animation.value
                val left = slot * index + (slot - barWidth) / 2f
                val isCurrent = index == values.lastIndex

                if (value <= 0L) {
                    drawRoundRect(
                        color = track.copy(alpha = 0.5f),
                        topLeft = Offset(left, baseY - 4.dp.toPx()),
                        size = Size(barWidth, 4.dp.toPx()),
                        cornerRadius = CornerRadius(barWidth / 2f),
                    )
                } else {
                    val barHeight = height.coerceAtLeast(6.dp.toPx())
                    val top = baseY - barHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (isCurrent) {
                                listOf(primary, primary.copy(alpha = 0.62f))
                            } else {
                                listOf(primary.copy(alpha = 0.55f), tertiary.copy(alpha = 0.35f))
                            },
                            startY = top,
                            endY = baseY,
                        ),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == labels.lastIndex) primary else labelColor,
                )
            }
        }
    }
}
