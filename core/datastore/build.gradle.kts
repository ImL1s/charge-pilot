plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.chargepilot.core.datastore"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
