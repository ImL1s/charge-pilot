plugins {
    alias(libs.plugins.chargepilot.android.feature)
}

android {
    namespace = "com.chargepilot.feature.home.impl"
}

dependencies {
    implementation(project(":feature:home:api"))
    implementation(project(":feature:disclosure:api"))
    implementation(project(":core:capability"))
    implementation(project(":core:control"))
    implementation(project(":core:datastore"))
    implementation(project(":core:device"))
    implementation(project(":core:foreground"))
    implementation(libs.timber)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
}
