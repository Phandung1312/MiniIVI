package com.android.car.launcher.feature.dashboard

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.android.car.launcher.R

internal data class HomeApp(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: HomeAppIcon,
    val accentColor: Color,
    val target: HomeAppTarget,
)

internal enum class HomeAppIcon {
    Media,
    Bluetooth,
    YouTube,
    Maps,
    Weather,
    Phone,
    Browser,
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
            accentColor = Color(0xFFFF6B7A),
            target = HomeAppTarget.Media,
        ),
        HomeApp(
            id = "bluetooth",
            titleRes = R.string.bluetooth,
            icon = HomeAppIcon.Bluetooth,
            accentColor = Color(0xFF63A8FF),
            target = HomeAppTarget.Bluetooth,
        ),
        HomeApp(
            id = "youtube",
            titleRes = R.string.youtube,
            icon = HomeAppIcon.YouTube,
            accentColor = Color(0xFFFF5A5F),
            target = HomeAppTarget.Packages(
                listOf(PackageLaunch(packageName = "com.google.android.youtube")),
            ),
        ),
        HomeApp(
            id = "maps",
            titleRes = R.string.maps,
            icon = HomeAppIcon.Maps,
            accentColor = Color(0xFF57C785),
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
            id = "weather",
            titleRes = R.string.weather,
            icon = HomeAppIcon.Weather,
            accentColor = Color(0xFFFFB454),
            target = HomeAppTarget.Mock,
        ),
        HomeApp(
            id = "phone",
            titleRes = R.string.phone,
            icon = HomeAppIcon.Phone,
            accentColor = Color(0xFF6DCE8A),
            target = HomeAppTarget.Dialer,
        ),
        HomeApp(
            id = "browser",
            titleRes = R.string.browser,
            icon = HomeAppIcon.Browser,
            accentColor = Color(0xFF6D9CFF),
            target = HomeAppTarget.Browser,
        ),
        HomeApp(
            id = "settings",
            titleRes = R.string.settings,
            icon = HomeAppIcon.Settings,
            accentColor = Color(0xFFB19CFF),
            target = HomeAppTarget.Settings,
        ),
    )
}
