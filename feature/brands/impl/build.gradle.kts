plugins {
    alias(libs.plugins.chargepilot.android.feature)
}

android {
    namespace = "com.chargepilot.feature.brands.impl"
}

dependencies {
    implementation(project(":feature:brands:api"))
    implementation(project(":core:capability"))
}
