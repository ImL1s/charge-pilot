plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.device"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
