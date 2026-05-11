import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            // Apply to whichever Android extension this module already has (app or library).
            extensions.findByType(CommonExtension::class.java)?.apply {
                buildFeatures.compose = true
            }
            dependencies {
                val bom = libs.findLibrary("compose-bom").get()
                "implementation"(platform(bom))
                "androidTestImplementation"(platform(bom))
                "implementation"(libs.findLibrary("compose-foundation").get())
                "implementation"(libs.findLibrary("compose-ui").get())
                "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
                "implementation"(libs.findLibrary("compose-material3").get())
                "implementation"(libs.findLibrary("compose-material-icons-extended").get())
                "debugImplementation"(libs.findLibrary("compose-ui-tooling").get())
            }
        }
    }
}
