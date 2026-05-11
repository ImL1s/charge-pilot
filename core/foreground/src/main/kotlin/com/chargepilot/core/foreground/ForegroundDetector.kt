package com.chargepilot.core.foreground

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub. Real impl uses `UsageStatsManager`. Always returns null until the
 * `PACKAGE_USAGE_STATS` flow is wired.
 */
@Singleton
class ForegroundDetector @Inject constructor() {
    fun currentForegroundPackage(): String? = null
    fun isPackageInForeground(packageName: String): Boolean = false
}
