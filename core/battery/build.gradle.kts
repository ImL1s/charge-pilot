plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.battery"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.coroutines.core)
}
