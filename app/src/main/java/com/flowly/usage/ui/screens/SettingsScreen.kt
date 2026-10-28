@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.flowly.usage.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flowly.usage.ui.UiState
import com.flowly.usage.ui.components.FlowlyCard
import com.flowly.usage.ui.formatQuota

@Composable
fun SettingsScreen(
    state: UiState,
    onSavePlan: (Float, Int) -> Unit,
    onOpenPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var quotaText by rememberSaveable(state.plan.quotaGb) {
        mutableStateOf(formatQuotaInput(state.plan.quotaGb))
    }
    var cycleDay by rememberSaveable(state.plan.cycleStartDay) {
        mutableStateOf(state.plan.cycleStartDay)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text("设置", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "套餐、周期与访问权限",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            FlowlyCard {
                Text("数据套餐", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "仅用于计算本周期剩余额度与百分比，不参与运营商计费。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = quotaText,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?$"))) quotaText = input
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("每月流量上限") },
                    suffix = { Text("GB") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "计费周期起始日",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                        FilterChip(
                            selected = cycleDay == day,
                            onClick = { cycleDay = day },
                            label = { Text("${day} 日") },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        quotaText.toFloatOrNull()?.let { onSavePlan(it, cycleDay) }
                    },
                    enabled = (quotaText.toFloatOrNull() ?: 0f) > 0f,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text("保存套餐")
                }
            }
        }

        item {
            FlowlyCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state.hasUsageAccess) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("使用情况访问权限", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (state.hasUsageAccess) "已授权，流量数据可正常读取" else "未授权，无法读取应用流量统计",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onOpenPermission,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("管理权限")
                }
            }
        }

        item {
            FlowlyCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text("关于", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("流量视界 · 1.0.0", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "数据来自 Android NetworkStats，按 UID 归入应用，包含后台流量。所有统计仅在设备本地完成，不会上传。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

private fun formatQuotaInput(gb: Float): String =
    if (gb % 1f == 0f) gb.toInt().toString() else String.format(java.util.Locale.US, "%.1f", gb)
