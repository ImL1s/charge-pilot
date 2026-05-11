import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

// Convention for feature impl modules under feature/<name>/impl.
// Applies Android library + Compose + Hilt and wires the standard core dependencies
// that every feature impl needs.
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("chargepilot.android.library")
                apply("chargepilot.android.compose")
                apply("chargepilot.android.hilt")
            }
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:ui"))
                "implementation"(project(":core:common"))
                "implementation"(project(":core:model"))
                "implementation"(project(":core:domain"))
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-navigation-compose").get())
                "implementation"(libs.findLibrary("hilt-navigation-compose").get())
            }
        }
    }
}
