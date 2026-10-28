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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flowly.usage.data.AppUsage
import com.flowly.usage.data.Period
import com.flowly.usage.data.Transport
import com.flowly.usage.ui.UiState
import com.flowly.usage.ui.components.AppUsageRow
import com.flowly.usage.ui.components.PeriodToggle
import com.flowly.usage.ui.components.TransportToggle
import com.flowly.usage.ui.formatBytes

@Composable
fun AppsScreen(
    state: UiState,
    onAppClick: (AppUsage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var transport by rememberSaveable { mutableStateOf(Transport.MOBILE) }
    var period by rememberSaveable { mutableStateOf(Period.CYCLE) }

    val filtered = remember(state.snapshot.apps, query, transport, period) {
        val keyword = query.trim()
        state.snapshot.apps
            .filter {
                keyword.isEmpty() ||
                    it.label.contains(keyword, ignoreCase = true) ||
                    it.packageName.contains(keyword, ignoreCase = true)
            }
            .sortedByDescending { it.bytes(transport, period) }
    }
    val maxBytes = (filtered.maxOfOrNull { it.bytes(transport, period) } ?: 1L).coerceAtLeast(1L)

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("应用流量", style = MaterialTheme.typography.displaySmall)
            Text(
                text = "${state.snapshot.apps.size} 个应用 · 本周期共 " +
                    formatBytes(state.snapshot.cycleMobile + state.snapshot.cycleWifi),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索应用") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "清空")
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
            )

            TransportToggle(selected = transport, onSelect = { transport = it })

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PeriodToggle(selected = period, onSelect = { period = it })
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (period == Period.TODAY) "今日用量" else "本周期用量",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = if (query.isEmpty()) "暂无应用流量数据" else "没有匹配「$query」的应用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            } else {
                items(filtered, key = { it.uid }) { app ->
                    AppUsageRow(
                        app = app,
                        transport = transport,
                        period = period,
                        maxBytes = maxBytes,
                        onClick = { onAppClick(app) },
                    )
                }
            }
        }
    }
}
