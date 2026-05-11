plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.common"
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.timber)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
