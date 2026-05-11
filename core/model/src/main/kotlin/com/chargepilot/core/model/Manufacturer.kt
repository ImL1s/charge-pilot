package com.chargepilot.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Manufacturer {
    SAMSUNG,
    GOOGLE,
    ONEPLUS,
    XIAOMI,
    HONOR,
    HUAWEI,
    OTHER;

    companion object {
        /**
         * Map a `Build.MANUFACTURER` string (case-insensitive, possibly null) to a known
         * [Manufacturer]. Unknown vendors map to [OTHER] so the UI can still render an
         * "unverified" placeholder.
         */
        fun fromBuild(manufacturerString: String?): Manufacturer = when (manufacturerString?.lowercase()) {
            "samsung" -> SAMSUNG
            "google" -> GOOGLE
            "oneplus" -> ONEPLUS
            "xiaomi", "redmi", "poco" -> XIAOMI
            "honor", "hihonor" -> HONOR
            "huawei" -> HUAWEI
            else -> OTHER
        }
    }
}
