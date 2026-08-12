package com.android.car.launcher.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.car.launcher.feature.media.MediaRepository
import com.android.car.launcher.feature.media.MediaState
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.VehicleStatusState
import com.miniivi.car.client.MiniIviCarClient
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
    private val mediaRepository: MediaRepository,
    private val carClient: MiniIviCarClient,
) : ViewModel() {
    val state = combine(
        mediaRepository.state,
        carClient.hvacState,
        carClient.vehicleStatusState,
        ::DashboardUiState,
    )
        .stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    init {
        carClient.start()
        viewModelScope.launch { mediaRepository.loadSongs() }
    }

    fun onPlayPause() = mediaRepository.togglePlayback()
    fun onNext() = mediaRepository.playNext()
    fun onPrevious() = mediaRepository.playPrevious()

    override fun onCleared() {
        carClient.close()
        super.onCleared()
    }
}
