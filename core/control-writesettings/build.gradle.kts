plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.control.writesettings"
}

dependencies {
    api(project(":core:control"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.timber)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
