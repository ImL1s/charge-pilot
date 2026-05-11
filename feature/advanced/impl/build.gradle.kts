plugins {
    alias(libs.plugins.chargepilot.android.feature)
    alias(libs.plugins.chargepilot.android.flavors)
}

android {
    namespace = "com.chargepilot.feature.advanced.impl"
    sourceSets {
        getByName("play") {
            java.srcDirs("src/play/kotlin")
        }
        getByName("full") {
            java.srcDirs("src/full/kotlin")
        }
    }
}

dependencies {
    implementation(project(":feature:advanced:api"))
    implementation(project(":core:control"))
    "fullImplementation"(project(":core:control-shizuku"))
    "fullImplementation"(project(":core:control-root"))
}
