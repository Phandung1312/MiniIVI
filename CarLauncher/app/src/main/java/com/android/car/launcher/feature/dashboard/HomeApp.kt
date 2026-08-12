package com.android.car.launcher.feature.dashboard

import androidx.annotation.StringRes
import com.android.car.launcher.R

internal data class HomeApp(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: HomeAppIcon,
    val target: HomeAppTarget,
)

internal enum class HomeAppIcon {
    Media,
    Video,
    Weather,
    Browser,
    Bluetooth,
    Maps,
    Phone,
    Settings,
}

internal sealed interface HomeAppTarget {
    data object Media : HomeAppTarget
    data object Bluetooth : HomeAppTarget
    data class Packages(val launchers: List<PackageLaunch>) : HomeAppTarget
    data object Dialer : HomeAppTarget
    data object Browser : HomeAppTarget
    data object Settings : HomeAppTarget
    data object Mock : HomeAppTarget
}

internal data class PackageLaunch(
    val packageName: String,
    val category: String? = null,
)

internal object HomeAppCatalog {
    val apps = listOf(
        HomeApp(
            id = "media",
            titleRes = R.string.media,
            icon = HomeAppIcon.Media,
            target = HomeAppTarget.Media,
        ),
        HomeApp(
            id = "video",
            titleRes = R.string.video,
            icon = HomeAppIcon.Video,
            target = HomeAppTarget.Packages(
                listOf(PackageLaunch(packageName = "com.google.android.youtube")),
            ),
        ),
        HomeApp(
            id = "weather",
            titleRes = R.string.weather,
            icon = HomeAppIcon.Weather,
            target = HomeAppTarget.Mock,
        ),
        HomeApp(
            id = "browser",
            titleRes = R.string.browser,
            icon = HomeAppIcon.Browser,
            target = HomeAppTarget.Browser,
        ),
        HomeApp(
            id = "bluetooth",
            titleRes = R.string.bluetooth,
            icon = HomeAppIcon.Bluetooth,
            target = HomeAppTarget.Bluetooth,
        ),
        HomeApp(
            id = "maps",
            titleRes = R.string.maps,
            icon = HomeAppIcon.Maps,
            target = HomeAppTarget.Packages(
                listOf(
                    PackageLaunch(packageName = "com.google.android.apps.maps"),
                    PackageLaunch(
                        packageName = "com.android.car.mapsplaceholder",
                        category = "android.intent.category.APP_MAPS",
                    ),
                ),
            ),
        ),
        HomeApp(
            id = "phone",
            titleRes = R.string.phone,
            icon = HomeAppIcon.Phone,
            target = HomeAppTarget.Dialer,
        ),
        HomeApp(
            id = "settings",
            titleRes = R.string.settings,
            icon = HomeAppIcon.Settings,
            target = HomeAppTarget.Settings,
        ),
    )
}
