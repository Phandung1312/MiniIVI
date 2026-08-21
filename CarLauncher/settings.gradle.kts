pluginManagement {
    repositories {
        google()
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

rootProject.name = "CarLauncher"
include(
    ":app",
    ":map-preview-contract",
    ":car-service-api",
    ":car-service-client",
    ":navigation-contract",
)
project(":map-preview-contract").projectDir = file("../MiniIviMaps/contract")
project(":car-service-api").projectDir = file("../CarService/api")
project(":car-service-client").projectDir = file("../CarService/client")
project(":navigation-contract").projectDir = file("../CarSystemUI/navigation-contract")
