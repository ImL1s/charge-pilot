package com.chargepilot.core.foreground

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Uses UsageStatsManager to estimate the current foreground app when Usage access is granted. */
@Singleton
class ForegroundDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Best-effort: AppOps `GET_USAGE_STATS` mode allowed for this package.
     * Missing permission must never be treated as "game not foreground is fine".
     */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun currentForegroundPackage(): String? {
        if (!hasUsageAccess()) return null
        val usageStats = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        return latestForegroundPackage(usageStats, now - RECENT_LOOKBACK_MS, now)
            ?: latestForegroundPackage(usageStats, now - FALLBACK_LOOKBACK_MS, now)
    }

    fun isPackageInForeground(packageName: String): Boolean =
        currentForegroundPackage() == packageName

    fun isGameInForeground(knownGames: Set<String>?): Boolean {
        val packageName = currentForegroundPackage() ?: return false
        if (knownGames != null) return packageName in knownGames
        return isGamePackage(packageName)
    }

    private fun isGamePackage(packageName: String): Boolean = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        info.category == ApplicationInfo.CATEGORY_GAME
    }.getOrDefault(false)

    private fun latestForegroundPackage(
        usageStats: UsageStatsManager,
        startTimeMs: Long,
        endTimeMs: Long,
    ): String? = runCatching {
        val events = usageStats.queryEvents(startTimeMs, endTimeMs)
        val event = UsageEvents.Event()
        val foregroundPackages = linkedMapOf<String, Long>()
        var latestPackage: String? = null
        var latestTimestamp = Long.MIN_VALUE

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    foregroundPackages[event.packageName] = event.timeStamp
                    if (event.timeStamp >= latestTimestamp) {
                        latestTimestamp = event.timeStamp
                        latestPackage = event.packageName
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    foregroundPackages.remove(event.packageName)
                    if (latestPackage == event.packageName) {
                        latestPackage = foregroundPackages.maxByOrNull { it.value }?.key
                        latestTimestamp = foregroundPackages[latestPackage] ?: Long.MIN_VALUE
                    }
                }
            }
        }
        latestPackage ?: foregroundPackages.maxByOrNull { it.value }?.key
    }.getOrNull()

    private companion object {
        const val RECENT_LOOKBACK_MS = 15_000L
        const val FALLBACK_LOOKBACK_MS = 60_000L
    }
}
