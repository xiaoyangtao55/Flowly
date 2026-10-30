package com.flowly.usage.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/** NetworkStats 中代表热点共享流量的特殊 UID（android.net.TrafficStats.UID_TETHERING） */
private const val UID_TETHERING = -5

/** 历史首次回填的最大天数 */
private const val BACKFILL_DAYS = 90L

class UsageRepository(private val context: Context) {

    private val statsManager: NetworkStatsManager? =
        context.getSystemService(NetworkStatsManager::class.java)
    private val packageManager = context.packageManager
    private val history = HistoryStore(context)

    private val dayCache = ConcurrentHashMap<Long, LongArray>()
    private val appCache = HashMap<Int, AppIdentity>()
    private val appCacheLock = Any()

    suspend fun loadSnapshot(plan: PlanSettings, chartDays: Int): UsageSnapshot =
        withContext(Dispatchers.IO) {
            val manager = statsManager ?: error("当前设备不支持系统流量统计")

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val now = System.currentTimeMillis()
            val cycleStart = plan.cycleStart(today)

            val todayStart = startOfDay(today, zone)
            val cycleStartMillis = startOfDay(cycleStart, zone)

            val todayMobile = queryPerUid(manager, ConnectivityManager.TYPE_MOBILE, todayStart, now)
            val todayWifi = queryPerUid(manager, ConnectivityManager.TYPE_WIFI, todayStart, now)
            val cycleMobile = if (cycleStart == today) {
                todayMobile
            } else {
                queryPerUid(manager, ConnectivityManager.TYPE_MOBILE, cycleStartMillis, now)
            }
            val cycleWifi = if (cycleStart == today) {
                todayWifi
            } else {
                queryPerUid(manager, ConnectivityManager.TYPE_WIFI, cycleStartMillis, now)
            }

            val todayHotspot = hotspotUsage(todayMobile, todayWifi)
            val cycleHotspot = hotspotUsage(cycleMobile, cycleWifi)

            val days = (chartDays - 1 downTo 0).map { today.minusDays(it.toLong()) }
            val daily = days.map { date ->
                if (date == today) {
                    DayTotal(date, todayMobile.total(), todayWifi.total())
                } else {
                    val (mobile, wifi) = dayTotals(manager, date, today, zone)
                    DayTotal(date, mobile, wifi)
                }
            }

            val yesterday = if (daily.size >= 2) daily[daily.size - 2] else null
            val apps = buildApps(todayMobile, todayWifi, cycleMobile, cycleWifi)

            history.upsert(
                daily.map { day ->
                    HistoryStore.Entry(
                        date = day.date,
                        mobileBytes = day.mobileBytes,
                        wifiBytes = day.wifiBytes,
                        hotspotBytes = if (day.date == today) todayHotspot else null,
                    )
                },
            )

            UsageSnapshot(
                todayMobile = todayMobile.total(),
                todayWifi = todayWifi.total(),
                yesterdayMobile = yesterday?.mobileBytes
                    ?: dayTotals(manager, today.minusDays(1), today, zone).first,
                yesterdayWifi = yesterday?.wifiBytes
                    ?: dayTotals(manager, today.minusDays(1), today, zone).second,
                cycleMobile = cycleMobile.total(),
                cycleWifi = cycleWifi.total(),
                todayHotspot = todayHotspot,
                cycleHotspot = cycleHotspot,
                daily = daily,
                apps = apps,
                cycleStart = cycleStart,
                generatedAt = now,
            )
        }

    /**
     * 读取本地保存的按月历史；首次使用时回填最近 90 天（受系统保留期限制，能取多少算多少）。
     */
    suspend fun loadHistory(months: Int = 6): List<MonthUsage> =
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val manager = statsManager ?: return@withContext history.months(months)

            val missing = history.missingDays(
                from = today.minusDays(BACKFILL_DAYS - 1),
                to = today.minusDays(1),
            )
            if (missing.isNotEmpty()) {
                val entries = missing.mapNotNull { date ->
                    runCatching {
                        val (mobile, wifi) = dayTotals(manager, date, today, zone)
                        HistoryStore.Entry(date, mobile, wifi)
                    }.getOrNull()
                }
                history.upsert(entries)
            }
            history.months(months)
        }

    private fun dayTotals(
        manager: NetworkStatsManager,
        date: LocalDate,
        today: LocalDate,
        zone: ZoneId,
    ): Pair<Long, Long> {
        val key = date.toEpochDay()
        val cached = dayCache[key]
        if (cached != null) return cached[0] to cached[1]

        val start = startOfDay(date, zone)
        val end = minOf(startOfDay(date.plusDays(1), zone), System.currentTimeMillis())
        val mobile = queryPerUid(manager, ConnectivityManager.TYPE_MOBILE, start, end).total()
        val wifi = queryPerUid(manager, ConnectivityManager.TYPE_WIFI, start, end).total()

        if (date.isBefore(today)) {
            if (dayCache.size > 90) dayCache.clear()
            dayCache[key] = longArrayOf(mobile, wifi)
        }
        return mobile to wifi
    }

    private fun queryPerUid(
        manager: NetworkStatsManager,
        networkType: Int,
        start: Long,
        end: Long,
    ): Map<Int, Long> {
        val stats = try {
            manager.querySummary(networkType, null, start, end)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            return emptyMap()
        }
        return try {
            val result = HashMap<Int, Long>()
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val bytes = bucket.rxBytes + bucket.txBytes
                if (bytes > 0L) {
                    result[bucket.uid] = (result[bucket.uid] ?: 0L) + bytes
                }
            }
            result
        } finally {
            stats.close()
        }
    }

    private fun buildApps(
        todayMobile: Map<Int, Long>,
        todayWifi: Map<Int, Long>,
        cycleMobile: Map<Int, Long>,
        cycleWifi: Map<Int, Long>,
    ): List<AppUsage> {
        val uids = HashSet<Int>(cycleMobile.keys + cycleWifi.keys)
        return uids.mapNotNull { uid ->
            val mobileCycle = cycleMobile[uid] ?: 0L
            val wifiCycle = cycleWifi[uid] ?: 0L
            if (mobileCycle + wifiCycle <= 0L) return@mapNotNull null
            val identity = resolveApp(uid) ?: return@mapNotNull null
            AppUsage(
                uid = uid,
                packageName = identity.packageName,
                label = identity.label,
                todayMobile = todayMobile[uid] ?: 0L,
                todayWifi = todayWifi[uid] ?: 0L,
                cycleMobile = mobileCycle,
                cycleWifi = wifiCycle,
            )
        }.sortedByDescending { it.cycleMobile + it.cycleWifi }
    }

    private fun resolveApp(uid: Int): AppIdentity? {
        synchronized(appCacheLock) {
            appCache[uid]?.let { return it }
        }
        val packages = packageManager.getPackagesForUid(uid) ?: return null
        val packageName = packages.firstOrNull() ?: return null
        val identity = try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            AppIdentity(packageName, packageManager.getApplicationLabel(info).toString())
        } catch (e: Exception) {
            AppIdentity(packageName, packageName)
        }
        synchronized(appCacheLock) { appCache[uid] = identity }
        return identity
    }

    /**
     * 系统把热点共享流量记在特殊 UID（-5）下，不归属任何应用。
     * 部分机型完全不返回该 bucket，此时无法区分“没用过热点”与“系统不上报”，返回 null。
     */
    private fun hotspotUsage(mobile: Map<Int, Long>, wifi: Map<Int, Long>): Long? {
        val mobileBytes = mobile[UID_TETHERING]
        val wifiBytes = wifi[UID_TETHERING]
        if (mobileBytes == null && wifiBytes == null) return null
        return (mobileBytes ?: 0L) + (wifiBytes ?: 0L)
    }

    private fun Map<Int, Long>.total(): Long = values.sum()

    private fun startOfDay(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private data class AppIdentity(val packageName: String, val label: String)

    companion object {
        fun hasUsageAccess(context: Context): Boolean {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
