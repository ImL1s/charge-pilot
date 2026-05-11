plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.control"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:capability"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
