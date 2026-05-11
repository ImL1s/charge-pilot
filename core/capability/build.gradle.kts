plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chargepilot.core.capability"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
