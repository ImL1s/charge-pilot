plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.control.root"
}

dependencies {
    api(project(":core:control"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    // libsu intentionally omitted while RootStrategy is a stub (plan 019).
}
