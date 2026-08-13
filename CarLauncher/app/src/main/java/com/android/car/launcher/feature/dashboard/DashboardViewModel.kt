package com.android.car.launcher.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.car.launcher.feature.media.MediaController
import com.android.car.launcher.feature.media.MediaState
import com.android.car.launcher.feature.dashboard.repository.HvacRepository
import com.android.car.launcher.feature.dashboard.repository.VehicleRepository
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.VehicleStatusState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val media: MediaState = MediaState(),
    val hvac: HvacState = HvacState(),
    val vehicleStatus: VehicleStatusState = VehicleStatusState(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val mediaRepository: MediaController,
    private val hvacRepository: HvacRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {
    val state = combine(
        mediaRepository.state,
        hvacRepository.state,
        vehicleRepository.state,
        ::DashboardUiState,
    )
        .stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    init {
        hvacRepository.start()
        vehicleRepository.start()
        viewModelScope.launch { mediaRepository.loadSongs() }
    }

    fun onPlayPause() = mediaRepository.togglePlayback()
    fun onNext() = mediaRepository.playNext()
    fun onPrevious() = mediaRepository.playPrevious()

    fun refresh() {
        hvacRepository.refresh()
        vehicleRepository.refresh()
    }
}
