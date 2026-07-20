@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "charge-pilot"

include(":app")

// Core modules
include(":core:designsystem")
include(":core:ui")
include(":core:common")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:datastore")
include(":core:device")
include(":core:capability")
include(":core:control")
include(":core:control-writesettings")
include(":core:control-shizuku")
include(":core:control-root")
include(":core:battery")
include(":core:foreground")
include(":core:testing")

// Feature modules (api/impl split)
listOf("home", "brands", "advanced", "history", "about", "disclosure").forEach { feature ->
    include(":feature:$feature:api")
    include(":feature:$feature:impl")
}
