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

rootProject.name = "CarService"
include(":app", ":car-service-api", ":car-service-client")
project(":car-service-api").projectDir = file("api")
project(":car-service-client").projectDir = file("client")
