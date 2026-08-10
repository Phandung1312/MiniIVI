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
include(":app", ":car-service-api", ":car-service-client")
project(":car-service-api").projectDir = file("../CarService/api")
project(":car-service-client").projectDir = file("../CarService/client")
