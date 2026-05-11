plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.compose)
}

android {
    namespace = "com.chargepilot.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(libs.androidx.lifecycle.runtime.compose)
}
