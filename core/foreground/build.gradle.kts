plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.foreground"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
}
