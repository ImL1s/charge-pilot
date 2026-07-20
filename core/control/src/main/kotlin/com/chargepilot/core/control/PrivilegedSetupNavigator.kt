package com.chargepilot.core.control

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.chargepilot.core.model.ControlMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens trusted setup surfaces for optional privileged backends. This is deliberately
 * navigation-only: it never installs APKs silently and never starts privileged services
 * without the user's explicit action in the external app.
 */
@Singleton
class PrivilegedSetupNavigator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun openSetup(method: ControlMethod): SetupNavigationResult = when (method) {
        ControlMethod.WRITE_SETTINGS_KEY -> openWriteSettingsAccess()
        ControlMethod.SHIZUKU_RPC -> openShizukuSetup()
        ControlMethod.ROOT_SHELL -> SetupNavigationResult.Unsupported
        ControlMethod.OFFICIAL_GUIDANCE,
        -> SetupNavigationResult.Unsupported
    }

    fun openWriteSettingsAccess(): SetupNavigationResult {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
        return if (start(intent)) {
            SetupNavigationResult.OpenedPermissionPage
        } else {
            SetupNavigationResult.Failed
        }
    }

    /** Opens the system Usage access settings so GameInForeground can be evaluated. */
    fun openUsageAccessSettings(): SetupNavigationResult {
        val candidates = listOf(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        )
        candidates.forEach { intent ->
            if (start(intent)) return SetupNavigationResult.OpenedPermissionPage
        }
        return SetupNavigationResult.Failed
    }

    fun openShizukuSetup(): SetupNavigationResult {
        context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let { launch ->
            if (start(launch)) return SetupNavigationResult.OpenedInstalledApp
        }

        val candidates = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD_URL)),
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PACKAGE"))
                .setPackage(PLAY_STORE_PACKAGE),
            Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL)),
        )
        candidates.forEach { intent ->
            if (start(intent)) return SetupNavigationResult.OpenedInstaller
        }
        return SetupNavigationResult.Failed
    }

    fun samsungGameToolsStatus(): SamsungGameToolsStatus = SamsungGameToolsStatus(
        gamingHubInstalled = isPackageInstalled(SAMSUNG_GAMING_HUB_PACKAGE),
        gameBoosterInstalled = isPackageInstalled(SAMSUNG_GAME_BOOSTER_PACKAGE),
        gameOptimizingServiceInstalled = isPackageInstalled(SAMSUNG_GOS_PACKAGE),
        goodLockInstalled = isPackageInstalled(SAMSUNG_GOOD_LOCK_PACKAGE),
        gamePluginsInstalled = isPackageInstalled(SAMSUNG_GAME_PLUGINS_PACKAGE),
        gameBoosterPlusInstalled = isPackageInstalled(SAMSUNG_GAME_BOOSTER_PLUS_PACKAGE),
    )

    fun openSamsungGameClassificationSetup(): SetupNavigationResult {
        val status = samsungGameToolsStatus()
        if (!status.gamePluginsInstalled) {
            val candidates = listOf(
                galaxyStoreIntent(SAMSUNG_GAME_PLUGINS_PACKAGE),
                Intent(Intent.ACTION_VIEW, Uri.parse(GALAXY_STORE_GAME_PLUGINS_URL)),
            )
            candidates.forEach { intent ->
                if (start(intent)) return SetupNavigationResult.OpenedInstaller
            }
        }
        if (!status.gameBoosterPlusInstalled) {
            val candidates = listOf(
                galaxyStoreIntent(SAMSUNG_GAME_BOOSTER_PLUS_PACKAGE),
                Intent(Intent.ACTION_VIEW, Uri.parse(GALAXY_STORE_GAME_BOOSTER_PLUS_URL)),
            )
            candidates.forEach { intent ->
                if (start(intent)) return SetupNavigationResult.OpenedInstaller
            }
        }
        if (status.gameBoosterPlusInstalled) {
            val explicitGameBoosterPlus = Intent(Intent.ACTION_MAIN).apply {
                component = android.content.ComponentName(
                    SAMSUNG_GAME_BOOSTER_PLUS_PACKAGE,
                    SAMSUNG_GAME_BOOSTER_PLUS_MAIN_ACTIVITY,
                )
            }
            if (start(explicitGameBoosterPlus)) return SetupNavigationResult.OpenedInstalledApp
        }
        val launchPackages = buildList {
            add(SAMSUNG_GAME_PLUGINS_PACKAGE)
            add(SAMSUNG_GAME_BOOSTER_PLUS_PACKAGE)
            if (status.classificationToolInstalled) add(SAMSUNG_GOOD_LOCK_PACKAGE)
        }
        launchPackages.forEach { packageName ->
            context.packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
                if (start(launch)) return SetupNavigationResult.OpenedInstalledApp
            }
        }

        context.packageManager.getLaunchIntentForPackage(SAMSUNG_GOOD_LOCK_PACKAGE)?.let { launch ->
            if (start(launch)) return SetupNavigationResult.OpenedInstalledApp
        }
        return SetupNavigationResult.Failed
    }

    fun openSamsungGamingHub(): SetupNavigationResult {
        context.packageManager.getLaunchIntentForPackage(SAMSUNG_GAMING_HUB_PACKAGE)?.let { launch ->
            if (start(launch)) return SetupNavigationResult.OpenedInstalledApp
        }
        val fallback = Intent(Intent.ACTION_MAIN).apply {
            component = android.content.ComponentName(
                SAMSUNG_GAMING_HUB_PACKAGE,
                "com.samsung.android.game.gamehome.app.MainActivity",
            )
        }
        return if (start(fallback)) SetupNavigationResult.OpenedInstalledApp else SetupNavigationResult.Failed
    }

    private fun galaxyStoreIntent(packageName: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("samsungapps://ProductDetail/$packageName"))
            .setPackage(GALAXY_STORE_PACKAGE)

    @Suppress("DEPRECATION")
    private fun isPackageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: RuntimeException) {
        false
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val SAMSUNG_GAMING_HUB_PACKAGE = "com.samsung.android.game.gamehome"
        const val SAMSUNG_GAME_BOOSTER_PACKAGE = "com.samsung.android.game.gametools"
        const val SAMSUNG_GOS_PACKAGE = "com.samsung.android.game.gos"
        const val SAMSUNG_GOOD_LOCK_PACKAGE = "com.samsung.android.goodlock"
        const val SAMSUNG_GAME_PLUGINS_PACKAGE = "com.samsung.android.game.gamelab"
        const val SAMSUNG_GAME_BOOSTER_PLUS_PACKAGE = "com.samsung.android.game.gameboosterplus"
        const val SAMSUNG_GAME_BOOSTER_PLUS_MAIN_ACTIVITY =
            "com.samsung.android.game.gameboosterplus.app.view.main.BoosterPlusActivity"
        const val GALAXY_STORE_PACKAGE = "com.sec.android.app.samsungapps"
        const val PLAY_STORE_PACKAGE = "com.android.vending"
        const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE"
        const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
        const val SHIZUKU_SETUP_GUIDE_URL = "https://shizuku.rikka.app/guide/setup/"
        const val GALAXY_STORE_GAME_PLUGINS_URL =
            "https://galaxystore.samsung.com/detail/$SAMSUNG_GAME_PLUGINS_PACKAGE"
        const val GALAXY_STORE_GAME_BOOSTER_PLUS_URL =
            "https://galaxystore.samsung.com/detail/$SAMSUNG_GAME_BOOSTER_PLUS_PACKAGE"
    }
}

data class SamsungGameToolsStatus(
    val gamingHubInstalled: Boolean,
    val gameBoosterInstalled: Boolean,
    val gameOptimizingServiceInstalled: Boolean,
    val goodLockInstalled: Boolean,
    val gamePluginsInstalled: Boolean,
    val gameBoosterPlusInstalled: Boolean,
) {
    val systemGameStackReady: Boolean =
        gamingHubInstalled && gameBoosterInstalled && gameOptimizingServiceInstalled
    val classificationToolInstalled: Boolean = gamePluginsInstalled && gameBoosterPlusInstalled
}

enum class SetupNavigationResult {
    OpenedPermissionPage,
    OpenedInstalledApp,
    OpenedInstaller,
    RequestedPermission,
    AlreadyReady,
    Unsupported,
    Failed,
}
