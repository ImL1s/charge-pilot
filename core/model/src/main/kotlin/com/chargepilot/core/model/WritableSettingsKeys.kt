package com.chargepilot.core.model

/**
 * Hard allowlist of system settings keys Charge Pilot may write via WRITE_SETTINGS or
 * Shizuku. Independent of the bundled registry JSON so a bad registry entry (or a future
 * remote policy file) cannot enable arbitrary system-int writes.
 *
 * Add keys here **and** in the registry in the same change, with device evidence.
 */
object WritableSettingsKeys {
    /** `Settings.System` integer keys that may be written as 0/1. */
    val SYSTEM_INT: Set<String> = setOf(
        "pass_through",
    )

    fun isAllowedSystemInt(key: String): Boolean = key in SYSTEM_INT
}
