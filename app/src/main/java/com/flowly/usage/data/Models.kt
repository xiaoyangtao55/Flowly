package com.flowly.usage.data

import java.time.LocalDate
import java.time.ZoneId

const val BYTES_PER_GB = 1024L * 1024L * 1024L

enum class Transport { MOBILE, WIFI }

enum class Period { TODAY, CYCLE }

data class PlanSettings(
    val quotaGb: Float = 20f,
    val cycleStartDay: Int = 1,
) {
    val quotaBytes: Long get() = (quotaGb.toDouble() * BYTES_PER_GB).toLong()

    fun cycleStart(today: LocalDate = LocalDate.now()): LocalDate {
        val day = cycleStartDay.coerceIn(1, 28)
        return if (today.dayOfMonth >= day) {
            today.withDayOfMonth(day)
        } else {
            today.minusMonths(1).withDayOfMonth(day)
        }
    }
}

data class DayTotal(
    val date: LocalDate,
    val mobileBytes: Long,
    val wifiBytes: Long,
) {
    fun bytes(transport: Transport): Long =
        if (transport == Transport.MOBILE) mobileBytes else wifiBytes
}

data class AppUsage(
    val uid: Int,
    val packageName: String,
    val label: String,
    val todayMobile: Long,
    val todayWifi: Long,
    val cycleMobile: Long,
    val cycleWifi: Long,
) {
    fun bytes(transport: Transport, period: Period): Long {
        val today = if (transport == Transport.MOBILE) todayMobile else todayWifi
        val cycle = if (transport == Transport.MOBILE) cycleMobile else cycleWifi
        return if (period == Period.TODAY) today else cycle
    }

    val cycleTotal: Long get() = cycleMobile + cycleWifi
}

data class UsageSnapshot(
    val todayMobile: Long = 0,
    val todayWifi: Long = 0,
    val yesterdayMobile: Long = 0,
    val yesterdayWifi: Long = 0,
    val cycleMobile: Long = 0,
    val cycleWifi: Long = 0,
    /** 热点共享用量；null 表示系统未返回 tethering 记录（无使用或机型不上报） */
    val todayHotspot: Long? = null,
    /** 热点共享用量；null 表示系统未返回 tethering 记录（无使用或机型不上报） */
    val cycleHotspot: Long? = null,
    val daily: List<DayTotal> = emptyList(),
    val apps: List<AppUsage> = emptyList(),
    val cycleStart: LocalDate = LocalDate.now(),
    val generatedAt: Long = 0L,
) {
    fun today(transport: Transport): Long =
        if (transport == Transport.MOBILE) todayMobile else todayWifi

    fun yesterday(transport: Transport): Long =
        if (transport == Transport.MOBILE) yesterdayMobile else yesterdayWifi

    fun cycle(transport: Transport): Long =
        if (transport == Transport.MOBILE) cycleMobile else cycleWifi
}
