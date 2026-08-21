pluginManagement {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CarSystemUI"
include(
    ":app",
    ":boot-animation",
    ":boot-brand",
    ":boot-progress-overlay",
    ":car-service-api",
    ":car-service-client",
    ":navigation-contract",
)
project(":car-service-api").projectDir = file("../CarService/api")
project(":car-service-client").projectDir = file("../CarService/client")
