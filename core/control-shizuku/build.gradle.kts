plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.control.shizuku"
}

dependencies {
    api(project(":core:control"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}
