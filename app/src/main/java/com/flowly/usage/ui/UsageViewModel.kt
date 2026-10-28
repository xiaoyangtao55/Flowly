package com.flowly.usage.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowly.usage.data.PlanSettings
import com.flowly.usage.data.PlanStore
import com.flowly.usage.data.UsageRepository
import com.flowly.usage.data.UsageSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.app.Application

data class UiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val hasUsageAccess: Boolean = true,
    val plan: PlanSettings = PlanSettings(),
    val snapshot: UsageSnapshot = UsageSnapshot(),
    val chartDays: Int = 7,
    val error: String? = null,
)

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageRepository(application)
    private val planStore = PlanStore(application)

    private val _state = MutableStateFlow(UiState(plan = planStore.load()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh(days: Int = _state.value.chartDays) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val hasAccess = UsageRepository.hasUsageAccess(getApplication())
            _state.update {
                it.copy(
                    chartDays = days,
                    hasUsageAccess = hasAccess,
                    refreshing = true,
                    loading = it.snapshot.generatedAt == 0L,
                    error = null,
                )
            }
            if (!hasAccess) {
                _state.update { it.copy(loading = false, refreshing = false) }
                return@launch
            }
            runCatching {
                repository.loadSnapshot(_state.value.plan, days)
            }.onSuccess { snapshot ->
                _state.update {
                    it.copy(
                        snapshot = snapshot,
                        loading = false,
                        refreshing = false,
                        hasUsageAccess = true,
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = throwable.message ?: "流量数据读取失败",
                    )
                }
            }
        }
    }

    fun savePlan(quotaGb: Float, cycleStartDay: Int) {
        val plan = PlanSettings(
            quotaGb = quotaGb.coerceIn(0.1f, 999f),
            cycleStartDay = cycleStartDay.coerceIn(1, 28),
        )
        planStore.save(plan)
        _state.update { it.copy(plan = plan) }
        refresh()
    }
}
