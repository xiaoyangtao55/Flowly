package com.flowly.usage.data

import android.content.Context

class PlanStore(context: Context) {

    private val prefs = context.getSharedPreferences("flowly_settings", Context.MODE_PRIVATE)

    fun load(): PlanSettings = PlanSettings(
        quotaGb = prefs.getFloat(KEY_QUOTA_GB, 20f),
        cycleStartDay = prefs.getInt(KEY_CYCLE_DAY, 1),
    )

    fun save(plan: PlanSettings) {
        prefs.edit()
            .putFloat(KEY_QUOTA_GB, plan.quotaGb)
            .putInt(KEY_CYCLE_DAY, plan.cycleStartDay)
            .apply()
    }

    private companion object {
        const val KEY_QUOTA_GB = "quota_gb"
        const val KEY_CYCLE_DAY = "cycle_start_day"
    }
}
