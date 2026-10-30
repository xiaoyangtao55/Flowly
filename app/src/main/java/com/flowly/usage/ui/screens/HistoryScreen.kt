@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.flowly.usage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.flowly.usage.data.MonthUsage
import com.flowly.usage.data.Transport
import com.flowly.usage.ui.UiState
import com.flowly.usage.ui.components.FlowlyCard
import com.flowly.usage.ui.components.MonthlyBarChart
import com.flowly.usage.ui.components.SectionHeader
import com.flowly.usage.ui.components.TransportToggle
import com.flowly.usage.ui.formatBytes

@Composable
fun HistoryScreen(
    state: UiState,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var transport by rememberSaveable { mutableStateOf(Transport.MOBILE) }
    val months = state.months
    val maxMonth = months.maxOfOrNull { it.total }?.coerceAtLeast(1L) ?: 1L
    val hasData = months.any { it.total > 0L }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "历史用量",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "本地保存 · 保留最近 400 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            FlowlyCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "近 6 个月",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.historyLoading) {
                        LoadingIndicator(Modifier.size(20.dp))
                    } else {
                        TextButton(onClick = onReload, contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Text("刷新")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                TransportToggle(selected = transport, onSelect = { transport = it })
                Spacer(Modifier.height(16.dp))
                MonthlyBarChart(months = months, transport = transport)
                Spacer(Modifier.height(14.dp))
                val total = months.sumOf { it.bytes(transport) }
                val average = if (months.isEmpty()) 0L else total / months.size
                Row {
                    HistoryMetric("月均", formatBytes(average), Modifier.weight(1f))
                    HistoryMetric("区间总计", formatBytes(total), Modifier.weight(1f))
                }
            }
        }

        item { SectionHeader("按月明细") }

        item {
            FlowlyCard(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                if (!hasData) {
                    Text(
                        text = "还没有历史记录。打开应用后每天会自动记录一次，首次会尝试回填最近 90 天（受系统保留期限制）。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    months.reversed().forEachIndexed { index, month ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                        }
                        MonthRow(month = month, maxTotal = maxMonth)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MonthRow(month: MonthUsage, maxTotal: Long) {
    val fraction = (month.total.toFloat() / maxTotal).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = month.label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(text = formatBytes(month.total), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = buildString {
                append("移动 ${formatBytes(month.mobileBytes)} · Wi-Fi ${formatBytes(month.wifiBytes)}")
                month.hotspotBytes?.let { append(" · 热点 ${formatBytes(it)}") }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
