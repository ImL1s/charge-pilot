plugins {
    alias(libs.plugins.chargepilot.android.library)
    alias(libs.plugins.chargepilot.android.hilt)
}

android {
    namespace = "com.chargepilot.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:capability"))
    implementation(project(":core:control"))
    implementation(project(":core:datastore"))
    implementation(project(":core:device"))
    implementation(libs.coroutines.core)
}
