import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("EnumEntryName")
enum class FlavorDimension { distribution }

@Suppress("EnumEntryName")
enum class ChargePilotFlavor(val dimension: FlavorDimension, val applicationIdSuffix: String? = null) {
    play(FlavorDimension.distribution),
    full(FlavorDimension.distribution, applicationIdSuffix = ".full"),
}

class AndroidFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.findByType(ApplicationExtension::class.java)?.let { ext ->
                configureFlavors(ext)
            }
            extensions.findByType(LibraryExtension::class.java)?.let { ext ->
                configureFlavors(ext)
            }
        }
    }
}

internal fun configureFlavors(extension: CommonExtension<*, *, *, *, *, *>) {
    val isApp = extension is ApplicationExtension
    extension.flavorDimensions += FlavorDimension.distribution.name
    extension.productFlavors {
        ChargePilotFlavor.values().forEach { chargeFlavor ->
            create(chargeFlavor.name) {
                dimension = chargeFlavor.dimension.name
                if (isApp && chargeFlavor.applicationIdSuffix != null) {
                    (this as? ApplicationProductFlavor)?.applicationIdSuffix =
                        chargeFlavor.applicationIdSuffix
                }
            }
        }
    }
}
