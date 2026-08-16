package com.android.car.launcher.feature.dashboard.domain.usecase

import com.android.car.launcher.feature.dashboard.domain.model.AppLaunchTarget
import com.android.car.launcher.feature.dashboard.domain.model.ExternalLaunchTarget
import com.android.car.launcher.feature.dashboard.domain.model.FeatureStatus
import com.android.car.launcher.feature.dashboard.domain.model.HvacState
import com.android.car.launcher.feature.dashboard.domain.model.HomeAppCatalog
import com.android.car.launcher.feature.dashboard.domain.model.HomeAppTarget
import com.android.car.launcher.feature.dashboard.domain.model.VehicleStatusState
import com.android.car.launcher.feature.dashboard.domain.repository.HvacRepository
import com.android.car.launcher.feature.dashboard.domain.repository.LauncherNavigationDestination
import com.android.car.launcher.feature.dashboard.domain.repository.NavigationStateReporter
import com.android.car.launcher.feature.dashboard.domain.repository.VehicleRepository
import com.android.car.launcher.feature.maps.domain.usecase.ResolveMapLaunchTargetUseCase
import com.android.car.launcher.feature.media.domain.model.MediaState
import com.android.car.launcher.feature.media.domain.usecase.LoadMediaLibraryUseCase
import com.android.car.launcher.feature.media.domain.usecase.ObserveMediaStateUseCase
import com.android.car.launcher.feature.media.domain.usecase.PlayNextMediaTrackUseCase
import com.android.car.launcher.feature.media.domain.usecase.PlayPreviousMediaTrackUseCase
import com.android.car.launcher.feature.media.domain.usecase.ToggleMediaPlaybackUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ObserveVehicleStatusUseCase @Inject constructor(
    private val repository: VehicleRepository,
) {
    operator fun invoke(): Flow<VehicleStatusState> = repository.state
}

class StartVehicleMonitoringUseCase @Inject constructor(
    private val repository: VehicleRepository,
) {
    operator fun invoke() = repository.start()
}

class RefreshVehicleStatusUseCase @Inject constructor(
    private val repository: VehicleRepository,
) {
    operator fun invoke() = repository.refresh()
}

class ObserveHvacStateUseCase @Inject constructor(
    private val repository: HvacRepository,
) {
    operator fun invoke(): Flow<HvacState> = repository.state
}

class StartHvacMonitoringUseCase @Inject constructor(
    private val repository: HvacRepository,
) {
    operator fun invoke() = repository.start()
}

class RefreshHvacStateUseCase @Inject constructor(
    private val repository: HvacRepository,
) {
    operator fun invoke() = repository.refresh()
}

class ObserveDashboardMediaUseCase @Inject constructor(
    private val observeMediaState: ObserveMediaStateUseCase,
) {
    operator fun invoke(): StateFlow<MediaState> = observeMediaState()
}

class LoadDashboardMediaUseCase @Inject constructor(
    private val loadMediaLibrary: LoadMediaLibraryUseCase,
) {
    suspend operator fun invoke() = loadMediaLibrary()
}

class ToggleDashboardPlaybackUseCase @Inject constructor(
    private val togglePlayback: ToggleMediaPlaybackUseCase,
) {
    operator fun invoke() = togglePlayback()
}

class PlayNextDashboardTrackUseCase @Inject constructor(
    private val playNext: PlayNextMediaTrackUseCase,
) {
    operator fun invoke() = playNext()
}

class PlayPreviousDashboardTrackUseCase @Inject constructor(
    private val playPrevious: PlayPreviousMediaTrackUseCase,
) {
    operator fun invoke() = playPrevious()
}

class OpenDashboardAppUseCase @Inject constructor(
    private val resolveMapLaunchTarget: ResolveMapLaunchTargetUseCase,
) {
    operator fun invoke(appId: String): AppLaunchTarget {
        val definition = HomeAppCatalog.definitions.firstOrNull { it.id == appId }
            ?: return AppLaunchTarget.Unavailable
        return when (val target = definition.target) {
            HomeAppTarget.Media -> AppLaunchTarget.Media
            HomeAppTarget.Bluetooth -> AppLaunchTarget.Bluetooth
            HomeAppTarget.Maps -> {
                val target = resolveMapLaunchTarget()
                AppLaunchTarget.External(
                    ExternalLaunchTarget.Component(target.packageName, target.className),
                )
            }
            is HomeAppTarget.Packages ->
                AppLaunchTarget.External(ExternalLaunchTarget.Packages(target.launchers))
            HomeAppTarget.Dialer -> AppLaunchTarget.External(ExternalLaunchTarget.Dialer)
            HomeAppTarget.Browser -> AppLaunchTarget.External(ExternalLaunchTarget.Browser)
            HomeAppTarget.Settings -> AppLaunchTarget.External(ExternalLaunchTarget.Settings)
            HomeAppTarget.Mock -> AppLaunchTarget.Unavailable
        }
    }
}

class ReportNavigationStateUseCase @Inject constructor(
    private val reporter: NavigationStateReporter,
) {
    operator fun invoke(destination: LauncherNavigationDestination) = reporter.report(destination)
}
