plugins {
    alias(libs.plugins.chargepilot.android.feature)
}

android {
    namespace = "com.chargepilot.feature.disclosure.impl"
}

dependencies {
    implementation(project(":feature:disclosure:api"))
    implementation(project(":core:control"))
}
