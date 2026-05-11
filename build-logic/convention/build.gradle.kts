plugins {
    `kotlin-dsl`
}

group = "com.chargepilot.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "chargepilot.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "chargepilot.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "chargepilot.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidCompose") {
            id = "chargepilot.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "chargepilot.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFlavors") {
            id = "chargepilot.android.flavors"
            implementationClass = "AndroidFlavorsConventionPlugin"
        }
        register("jvmLibrary") {
            id = "chargepilot.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
