package com.flowly.usage.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

/**
 * 按天保存当日流量快照，避免系统清理 NetworkStats 历史后数据丢失。
 * 存储在 SharedPreferences 的 JSON 里：key 为 epochDay，值为 [移动, Wi-Fi, 热点(-1 表示未知)]。
 */
class HistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("flowly_history", Context.MODE_PRIVATE)
    private val lock = Any()

    data class Entry(
        val date: LocalDate,
        val mobileBytes: Long,
        val wifiBytes: Long,
        val hotspotBytes: Long? = null,
    )

    fun upsert(entries: List<Entry>) {
        if (entries.isEmpty()) return
        synchronized(lock) {
            val root = load()
            entries.forEach { entry ->
                val key = entry.date.toEpochDay().toString()
                val old = root.optJSONArray(key)
                val previousHotspot = old?.optLong(2, -1L)?.takeIf { it >= 0L }
                val hotspot = entry.hotspotBytes ?: previousHotspot
                root.put(
                    key,
                    JSONArray()
                        .put(entry.mobileBytes)
                        .put(entry.wifiBytes)
                        .put(hotspot ?: -1L),
                )
            }
            prune(root)
            save(root)
        }
    }

    /** 返回 [from, to] 区间内还没有记录的自然日（不含今天部分数据）。 */
    fun missingDays(from: LocalDate, to: LocalDate): List<LocalDate> {
        if (to.isBefore(from)) return emptyList()
        synchronized(lock) {
            val root = load()
            return generateSequence(from) { it.plusDays(1) }
                .takeWhile { !it.isAfter(to) }
                .filter { !root.has(it.toEpochDay().toString()) }
                .toList()
        }
    }

    /** 最近 count 个月（含本月）的汇总，按时间升序。 */
    fun months(count: Int, today: LocalDate = LocalDate.now()): List<MonthUsage> {
        val safeCount = count.coerceIn(1, 24)
        val current = YearMonth.from(today)
        val first = current.minusMonths((safeCount - 1).toLong())
        val rangeStart = first.atDay(1)

        val accumulators = HashMap<YearMonth, MonthAccumulator>()
        synchronized(lock) {
            val root = load()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val epochDay = key.toLongOrNull() ?: continue
                val date = LocalDate.ofEpochDay(epochDay)
                if (date.isBefore(rangeStart) || date.isAfter(today)) continue
                val array = root.optJSONArray(key) ?: continue
                val accumulator = accumulators.getOrPut(YearMonth.from(date)) { MonthAccumulator() }
                accumulator.mobile += array.optLong(0, 0L)
                accumulator.wifi += array.optLong(1, 0L)
                val hotspot = array.optLong(2, -1L)
                if (hotspot >= 0L) accumulator.hotspot = (accumulator.hotspot ?: 0L) + hotspot
            }
        }

        return (0 until safeCount).map { offset ->
            val ym = current.minusMonths((safeCount - 1 - offset).toLong())
            val accumulator = accumulators[ym]
            MonthUsage(
                year = ym.year,
                month = ym.monthValue,
                mobileBytes = accumulator?.mobile ?: 0L,
                wifiBytes = accumulator?.wifi ?: 0L,
                hotspotBytes = accumulator?.hotspot,
            )
        }
    }

    private fun prune(root: JSONObject) {
        val cutoff = LocalDate.now().toEpochDay() - MAX_KEEP_DAYS
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val epochDay = key.toLongOrNull() ?: continue
            if (epochDay < cutoff) root.remove(key)
        }
    }

    private fun load(): JSONObject {
        val raw = prefs.getString(KEY, null) ?: return JSONObject()
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    private fun save(root: JSONObject) {
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    private class MonthAccumulator {
        var mobile: Long = 0L
        var wifi: Long = 0L
        var hotspot: Long? = null
    }

    companion object {
        private const val KEY = "days"
        private const val MAX_KEEP_DAYS = 400L
    }
}
