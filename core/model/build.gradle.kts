plugins {
    alias(libs.plugins.chargepilot.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
