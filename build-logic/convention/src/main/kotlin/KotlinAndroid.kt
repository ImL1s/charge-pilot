import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Apply shared Android-Kotlin configuration (compile/min SDK, Java 17, Kotlin compiler args). */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    val libs = extensions.getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java)
        .named("libs")
    commonExtension.apply {
        compileSdk = libs.findVersion("android-compile-sdk").get().requiredVersion.toInt()
        defaultConfig {
            minSdk = libs.findVersion("android-min-sdk").get().requiredVersion.toInt()
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = false
        }
    }
    configureKotlin<KotlinAndroidProjectExtension>()
}

/** Apply shared JVM-Kotlin configuration. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    configureKotlin<KotlinJvmProjectExtension>()
}

private inline fun <reified T : Any> Project.configureKotlin() {
    configure<T> {
        when (this) {
            is KotlinAndroidProjectExtension -> compilerOptions
            is KotlinJvmProjectExtension -> compilerOptions
            else -> error("Unsupported Kotlin extension")
        }.apply {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(false)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}
