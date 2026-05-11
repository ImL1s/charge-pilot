plugins {
    alias(libs.plugins.chargepilot.android.library)
}

android {
    namespace = "com.chargepilot.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(libs.junit.jupiter)
    api(libs.turbine)
    api(libs.truth)
    api(libs.coroutines.test)
}
