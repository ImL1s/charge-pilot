plugins {
    alias(libs.plugins.chargepilot.android.application)
    alias(libs.plugins.chargepilot.android.compose)
    alias(libs.plugins.chargepilot.android.hilt)
    alias(libs.plugins.chargepilot.android.flavors)
}

android {
    namespace = "com.chargepilot.app"

    defaultConfig {
        applicationId = "com.chargepilot.app"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:device"))
    implementation(project(":core:capability"))
    implementation(project(":core:control"))
    implementation(project(":core:control-writesettings"))
    implementation(project(":core:battery"))
    implementation(project(":core:foreground"))

    // Feature modules (api for navigation graph wiring; impl for code)
    implementation(project(":feature:home:api"))
    implementation(project(":feature:home:impl"))
    implementation(project(":feature:brands:api"))
    implementation(project(":feature:brands:impl"))
    implementation(project(":feature:advanced:api"))
    implementation(project(":feature:advanced:impl"))
    implementation(project(":feature:history:api"))
    implementation(project(":feature:history:impl"))
    implementation(project(":feature:about:api"))
    implementation(project(":feature:about:impl"))
    implementation(project(":feature:disclosure:api"))
    implementation(project(":feature:disclosure:impl"))

    // Privileged-mode strategies — full flavor only
    "fullImplementation"(project(":core:control-shizuku"))
    "fullImplementation"(project(":core:control-root"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.timber)
}
