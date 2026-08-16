package com.android.car.launcher.feature.dashboard.domain.model

data class HomeAppDefinition(
    val id: String,
    val target: HomeAppTarget,
)

sealed interface HomeAppTarget {
    data object Media : HomeAppTarget
    data object Bluetooth : HomeAppTarget
    data object Maps : HomeAppTarget
    data class Packages(val launchers: List<PackageLaunch>) : HomeAppTarget
    data object Dialer : HomeAppTarget
    data object Browser : HomeAppTarget
    data object Settings : HomeAppTarget
    data object Mock : HomeAppTarget
}

data class PackageLaunch(
    val packageName: String,
    val category: String? = null,
)

object HomeAppCatalog {
    val definitions = listOf(
        HomeAppDefinition("media", HomeAppTarget.Media),
        HomeAppDefinition(
            "video",
            HomeAppTarget.Packages(
                listOf(PackageLaunch(packageName = "com.google.android.youtube")),
            ),
        ),
        HomeAppDefinition("weather", HomeAppTarget.Mock),
        HomeAppDefinition("browser", HomeAppTarget.Browser),
        HomeAppDefinition("bluetooth", HomeAppTarget.Bluetooth),
        HomeAppDefinition("maps", HomeAppTarget.Maps),
        HomeAppDefinition("phone", HomeAppTarget.Dialer),
        HomeAppDefinition("settings", HomeAppTarget.Settings),
    )
}

enum class HomeDestination {
    Home,
    Apps,
}

object HomeStartDestination {
    const val EXTRA = "com.android.car.launcher.extra.START_DESTINATION"
    const val APPS = "apps"

    fun fromValue(value: String?): HomeDestination =
        if (value == APPS) HomeDestination.Apps else HomeDestination.Home
}

sealed interface AppLaunchTarget {
    data object Media : AppLaunchTarget
    data object Bluetooth : AppLaunchTarget
    data class External(val target: ExternalLaunchTarget) : AppLaunchTarget
    data object Unavailable : AppLaunchTarget
}

sealed interface ExternalLaunchTarget {
    data class Packages(val launchers: List<PackageLaunch>) : ExternalLaunchTarget
    data object Dialer : ExternalLaunchTarget
    data object Browser : ExternalLaunchTarget
    data object Settings : ExternalLaunchTarget
    data class Component(val packageName: String, val className: String) : ExternalLaunchTarget
}
