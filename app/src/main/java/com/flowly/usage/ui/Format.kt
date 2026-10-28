package com.flowly.usage.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

fun formatQuota(gb: Float): String =
    if (gb % 1f == 0f) "${gb.toInt()} GB" else String.format(Locale.US, "%.1f GB", gb)

fun percentDelta(current: Long, previous: Long): Int? {
    if (previous <= 0L) return null
    return (((current - previous).toDouble() / previous) * 100).roundToInt()
}

fun todaySubtitle(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE))

fun chartDateLabel(date: LocalDate, compact: Boolean): String =
    if (compact) {
        date.format(DateTimeFormatter.ofPattern("M/d", Locale.US))
    } else {
        date.format(DateTimeFormatter.ofPattern("E", Locale.SIMPLIFIED_CHINESE))
    }
