package com.android.car.launcher.feature.dashboard.presentation.model

import androidx.annotation.StringRes
import com.android.car.launcher.R
import com.android.car.launcher.feature.dashboard.domain.model.HomeDestination
import com.android.car.launcher.feature.dashboard.domain.model.HvacState
import com.android.car.launcher.feature.dashboard.domain.model.VehicleStatusState
import com.android.car.launcher.feature.media.domain.model.MediaState

internal data class DashboardUiState(
    val media: MediaState = MediaState(),
    val hvac: HvacState = HvacState(),
    val vehicleStatus: VehicleStatusState = VehicleStatusState(),
    val destination: HomeDestination = HomeDestination.Home,
    val apps: List<HomeApp> = HomeAppCatalog.apps,
)

sealed interface DashboardEffect {
    data class LaunchApp(
        val target: com.android.car.launcher.feature.dashboard.domain.model.AppLaunchTarget,
    ) : DashboardEffect
}

internal data class HomeApp(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: HomeAppIcon,
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

internal object HomeAppCatalog {
    val apps = listOf(
        HomeApp("media", R.string.media, HomeAppIcon.Media),
        HomeApp("video", R.string.video, HomeAppIcon.Video),
        HomeApp("weather", R.string.weather, HomeAppIcon.Weather),
        HomeApp("browser", R.string.browser, HomeAppIcon.Browser),
        HomeApp("bluetooth", R.string.bluetooth, HomeAppIcon.Bluetooth),
        HomeApp("maps", R.string.maps, HomeAppIcon.Maps),
        HomeApp("phone", R.string.phone, HomeAppIcon.Phone),
        HomeApp("settings", R.string.settings, HomeAppIcon.Settings),
    )
}
