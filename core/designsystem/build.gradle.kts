plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.compose)
}

android {
    namespace = "com.chargepilot.core.designsystem"
}

dependencies {
    api(libs.compose.material3)
    api(libs.compose.foundation)
    api(libs.compose.material.icons.extended)
}
