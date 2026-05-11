plugins {
    alias(libs.plugins.chargepilot.android.feature)
}

android {
    namespace = "com.chargepilot.feature.history.impl"
}

dependencies {
    implementation(project(":feature:history:api"))
    implementation(project(":core:datastore"))
    implementation(project(":core:control"))
}
