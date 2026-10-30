package com.flowly.usage.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowly.usage.data.AppUsage
import com.flowly.usage.ui.components.LoadingPane
import com.flowly.usage.ui.components.PermissionGate
import com.flowly.usage.ui.screens.AppDetailSheet
import com.flowly.usage.ui.screens.AppsScreen
import com.flowly.usage.ui.screens.HistoryScreen
import com.flowly.usage.ui.screens.OverviewScreen
import com.flowly.usage.ui.screens.SettingsScreen

@Composable
fun FlowlyApp() {
    val viewModel: UsageViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tab by rememberSaveable { mutableStateOf(0) }
    var selectedApp by remember { mutableStateOf<AppUsage?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
        viewModel.refreshHistory()
    }

    LaunchedEffect(tab) {
        if (tab == 2) viewModel.refreshHistory()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text("概览") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Rounded.Apps, contentDescription = null) },
                    label = { Text("应用") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                    label = { Text("历史") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text("设置") },
                )
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                tab == 3 -> SettingsScreen(
                    state = state,
                    onSavePlan = { quotaGb, day -> viewModel.savePlan(quotaGb, day) },
                    onOpenPermission = { openUsageAccess(context) },
                )

                !state.hasUsageAccess && !state.loading -> PermissionGate(
                    onGrant = { openUsageAccess(context) },
                    onRefresh = { viewModel.refresh() },
                )

                state.loading && state.snapshot.generatedAt == 0L -> LoadingPane()

                tab == 0 -> OverviewScreen(
                    state = state,
                    onRefresh = { viewModel.refresh() },
                    onDaysChange = { days -> viewModel.refresh(days) },
                    onOpenApps = { tab = 1 },
                    onOpenPlan = { tab = 3 },
                    onAppClick = { selectedApp = it },
                )

                tab == 2 -> HistoryScreen(
                    state = state,
                    onReload = { viewModel.refreshHistory() },
                )

                else -> AppsScreen(
                    state = state,
                    onAppClick = { selectedApp = it },
                )
            }
        }
    }

    AppDetailSheet(app = selectedApp, onDismiss = { selectedApp = null })
}

private fun openUsageAccess(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
