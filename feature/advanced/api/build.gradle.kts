plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chargepilot.feature.advanced.api"
}

dependencies {
    api(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
