package com.android.car.launcher.feature.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.car.launcher.feature.dashboard.domain.model.AppLaunchTarget
import com.android.car.launcher.feature.dashboard.domain.model.HomeDestination
import com.android.car.launcher.feature.dashboard.domain.model.HomeStartDestination
import com.android.car.launcher.feature.dashboard.domain.repository.LauncherNavigationDestination
import com.android.car.launcher.feature.dashboard.domain.usecase.LoadDashboardMediaUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.ObserveDashboardMediaUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.ObserveHvacStateUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.ObserveVehicleStatusUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.OpenDashboardAppUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.PlayNextDashboardTrackUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.PlayPreviousDashboardTrackUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.RefreshHvacStateUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.RefreshVehicleStatusUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.ReportNavigationStateUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.StartHvacMonitoringUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.StartVehicleMonitoringUseCase
import com.android.car.launcher.feature.dashboard.domain.usecase.ToggleDashboardPlaybackUseCase
import com.android.car.launcher.feature.dashboard.presentation.model.DashboardEffect
import com.android.car.launcher.feature.dashboard.presentation.model.DashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeMedia: ObserveDashboardMediaUseCase,
    observeHvac: ObserveHvacStateUseCase,
    observeVehicle: ObserveVehicleStatusUseCase,
    private val startHvac: StartHvacMonitoringUseCase,
    private val startVehicle: StartVehicleMonitoringUseCase,
    private val refreshHvac: RefreshHvacStateUseCase,
    private val refreshVehicle: RefreshVehicleStatusUseCase,
    private val loadMedia: LoadDashboardMediaUseCase,
    private val togglePlayback: ToggleDashboardPlaybackUseCase,
    private val playNext: PlayNextDashboardTrackUseCase,
    private val playPrevious: PlayPreviousDashboardTrackUseCase,
    private val openApp: OpenDashboardAppUseCase,
    private val reportNavigationState: ReportNavigationStateUseCase,
) : ViewModel() {
    private val destination = MutableStateFlow(HomeDestination.Home)
    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    internal val state = combine(
        observeMedia(),
        observeHvac(),
        observeVehicle(),
        destination,
    ) { media, hvac, vehicle, currentDestination ->
        DashboardUiState(
            media = media,
            hvac = hvac,
            vehicleStatus = vehicle,
            destination = currentDestination,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    init {
        startHvac()
        startVehicle()
        viewModelScope.launch { loadMedia() }
    }

    fun onStartDestination(value: String?) {
        setDestination(HomeStartDestination.fromValue(value))
    }

    fun showHome() {
        setDestination(HomeDestination.Home)
    }

    fun onResumed() {
        refreshHvac()
        refreshVehicle()
        reportCurrentDestination()
    }

    fun onAppSelected(appId: String) {
        viewModelScope.launch {
            _effects.send(DashboardEffect.LaunchApp(openApp(appId)))
        }
    }

    fun onExternalAppOpened() {
        reportNavigationState(LauncherNavigationDestination.None)
    }

    fun onPlayPause() = togglePlayback()
    fun onNext() = playNext()
    fun onPrevious() = playPrevious()

    private fun setDestination(value: HomeDestination) {
        destination.value = value
        reportCurrentDestination()
    }

    private fun reportCurrentDestination() {
        val navigationDestination = when (destination.value) {
            HomeDestination.Home -> LauncherNavigationDestination.Home
            HomeDestination.Apps -> LauncherNavigationDestination.AppList
        }
        reportNavigationState(navigationDestination)
    }
}
