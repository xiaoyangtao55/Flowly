@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.flowly.usage.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flowly.usage.data.AppUsage
import com.flowly.usage.data.Period
import com.flowly.usage.data.Transport
import com.flowly.usage.ui.UiState
import com.flowly.usage.ui.components.AppUsageRow
import com.flowly.usage.ui.components.DailyBarChart
import com.flowly.usage.ui.components.FlowlyCard
import com.flowly.usage.ui.components.SectionHeader
import com.flowly.usage.ui.components.StatTile
import com.flowly.usage.ui.components.TransportToggle
import com.flowly.usage.ui.formatBytes
import com.flowly.usage.ui.formatQuota
import com.flowly.usage.ui.percentDelta
import com.flowly.usage.ui.todaySubtitle
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun OverviewScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onDaysChange: (Int) -> Unit,
    onOpenApps: () -> Unit,
    onOpenPlan: () -> Unit,
    onAppClick: (AppUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var chartTransport by rememberSaveable { mutableStateOf(Transport.MOBILE) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "流量视界",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${todaySubtitle()} · 数据仅存于本机",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRefresh) {
                    if (state.refreshing) {
                        LoadingIndicator(Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (state.error != null && state.snapshot.generatedAt == 0L) {
            item {
                FlowlyCard(color = MaterialTheme.colorScheme.errorContainer) {
                    Text("读取失败", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(state.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onRefresh) { Text("重试") }
                }
            }
        }

        item { PlanHeroCard(state = state, onOpenPlan = onOpenPlan) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "今日移动",
                    value = formatBytes(state.snapshot.todayMobile),
                    icon = Icons.Rounded.PhoneAndroid,
                    delta = percentDelta(state.snapshot.todayMobile, state.snapshot.yesterdayMobile),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "今日 Wi-Fi",
                    value = formatBytes(state.snapshot.todayWifi),
                    icon = Icons.Rounded.Wifi,
                    delta = percentDelta(state.snapshot.todayWifi, state.snapshot.yesterdayWifi),
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            FlowlyCard(contentPadding = PaddingValues(20.dp)) {
                SectionHeader("用量趋势") {
                    TextButton(
                        onClick = { onDaysChange(if (state.chartDays == 7) 30 else 7) },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text(if (state.chartDays == 7) "近 7 天" else "近 30 天")
                    }
                }
                Spacer(Modifier.height(10.dp))
                TransportToggle(
                    selected = chartTransport,
                    onSelect = { chartTransport = it },
                )
                Spacer(Modifier.height(16.dp))
                DailyBarChart(
                    daily = state.snapshot.daily,
                    transport = chartTransport,
                )
                Spacer(Modifier.height(14.dp))
                val total = state.snapshot.daily.sumOf { it.bytes(chartTransport) }
                val average = if (state.snapshot.daily.isEmpty()) 0L else total / state.snapshot.daily.size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrendMetric("日均", formatBytes(average), Modifier.weight(1f))
                    TrendMetric("区间总计", formatBytes(total), Modifier.weight(1f))
                }
            }
        }

        item {
            SectionHeader("应用排行") {
                TextButton(onClick = onOpenApps, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("查看全部")
                }
            }
        }

        item {
            FlowlyCard(contentPadding = PaddingValues(10.dp)) {
                val topApps = state.snapshot.apps.take(5)
                if (topApps.isEmpty()) {
                    Text(
                        text = "暂无应用流量数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    val maxBytes = topApps.maxOf { it.bytes(chartTransport, Period.CYCLE) }.coerceAtLeast(1L)
                    topApps.forEach { app ->
                        AppUsageRow(
                            app = app,
                            transport = chartTransport,
                            period = Period.CYCLE,
                            maxBytes = maxBytes,
                            onClick = { onAppClick(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanHeroCard(state: UiState, onOpenPlan: () -> Unit) {
    val quotaBytes = state.plan.quotaBytes.coerceAtLeast(1L)
    val used = state.snapshot.cycleMobile
    val remaining = (quotaBytes - used).coerceAtLeast(0L)
    val fraction = (used.toFloat() / quotaBytes).coerceIn(0f, 1f)
    val remainingPercent = ((1f - fraction) * 100f).roundToInt()
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "planProgress",
    )
    val flowerShape = MaterialShapes.Flower.toShape()
    val strokeWidth = with(LocalDensity.current) { 13.dp.toPx() }
    val cycleLabel = remember(state.snapshot.cycleStart) {
        state.snapshot.cycleStart.format(DateTimeFormatter.ofPattern("M/d"))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 64.dp, y = (-78).dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.06f),
                        shape = flowerShape,
                    ),
            )

            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.DataUsage,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "本周期移动数据",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = onOpenPlan,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Text("套餐设置", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularWavyProgressIndicator(
                            progress = { animatedFraction },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            stroke = Stroke(strokeWidth, cap = StrokeCap.Round),
                            trackStroke = Stroke(strokeWidth, cap = StrokeCap.Round),
                            gapSize = 4.dp,
                            amplitude = { 0.05f },
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$remainingPercent%",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Text(
                                text = "剩余",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "已用",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                        Text(
                            text = formatBytes(used),
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "套餐 ${formatQuota(state.plan.quotaGb)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Text(
                            text = "剩余 ${formatBytes(remaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "周期自 $cycleLabel 起",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
