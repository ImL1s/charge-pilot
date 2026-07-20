plugins {
    alias(libs.plugins.chargepilot.android.feature)
}

android {
    namespace = "com.chargepilot.feature.about.impl"
}

dependencies {
    implementation(project(":feature:about:api"))
    implementation(project(":core:device"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
}
