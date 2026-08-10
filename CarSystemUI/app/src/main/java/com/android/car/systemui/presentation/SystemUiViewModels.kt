package com.android.car.systemui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.repository.AudioRepository
import com.android.car.systemui.data.repository.AudioLevelMapper
import com.android.car.systemui.data.repository.BrightnessRepository
import com.android.car.systemui.data.repository.HvacRepository
import com.android.car.systemui.data.repository.NavigationRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SystemUiState(val quickControlVisible: Boolean = false)

data class QuickControlUiState(
    val brightness: BrightnessState = BrightnessState(),
    val audio: AudioState = AudioState(),
    val hvac: HvacState = HvacState(),
    val brightnessPreview: Float? = null,
) {
    val displayedBrightness: Float
        get() = brightnessPreview ?: brightness.progress
}

class SystemUiViewModel(
    private val navigationRepository: NavigationRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SystemUiState())
    val state = mutableState.asStateFlow()

    fun toggleQuickControl() {
        mutableState.value = mutableState.value.copy(
            quickControlVisible = !mutableState.value.quickControlVisible,
        )
    }

    fun dismissQuickControl() {
        mutableState.value = mutableState.value.copy(quickControlVisible = false)
    }

    fun goHome() = navigationRepository.goHome()
    fun openSettings() = navigationRepository.openSettings()
    fun openAppList() = navigationRepository.openAppList()
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
class QuickControlViewModel(
    private val brightnessRepository: BrightnessRepository,
    private val audioRepository: AudioRepository,
    private val hvacRepository: HvacRepository,
) : ViewModel() {
    private val brightnessPreview = MutableStateFlow<Float?>(null)
    private val brightnessChanges = MutableSharedFlow<Float>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val state: StateFlow<QuickControlUiState> = combine(
        brightnessRepository.state,
        audioRepository.state,
        hvacRepository.state,
        brightnessPreview,
    ) { brightness, audio, hvac, preview ->
        QuickControlUiState(brightness, audio, hvac, preview)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        QuickControlUiState(),
    )

    init {
        brightnessRepository.start()
        audioRepository.start()
        hvacRepository.start()
        viewModelScope.launch {
            brightnessChanges.sample(BRIGHTNESS_WRITE_INTERVAL_MS).collect { progress ->
                brightnessRepository.setBrightness(progress)
            }
        }
    }

    fun refresh() {
        brightnessRepository.refresh()
        audioRepository.refresh()
        hvacRepository.refresh()
    }

    fun onBrightnessChanged(progress: Float) {
        val normalized = progress.coerceIn(0f, 1f)
        brightnessPreview.value = normalized
        brightnessChanges.tryEmit(normalized)
    }

    fun onBrightnessChangeFinished() {
        val finalValue = brightnessPreview.value ?: return
        viewModelScope.launch {
            brightnessRepository.setBrightness(finalValue)
            brightnessPreview.value = null
        }
    }

    fun onVolumeChanged(progress: Float) {
        val audio = audioRepository.state.value
        if (!audio.available) return
        val volume = AudioLevelMapper.toVolume(progress, audio.minimum, audio.maximum)
        if (volume != audio.volume) audioRepository.setVolume(volume)
    }

    fun decreaseTemperature(zone: ClimateZone) =
        hvacRepository.adjustTemperature(zone, -TEMPERATURE_STEP)

    fun increaseTemperature(zone: ClimateZone) =
        hvacRepository.adjustTemperature(zone, TEMPERATURE_STEP)

    fun setAc(enabled: Boolean) = hvacRepository.setAc(enabled)

    override fun onCleared() {
        brightnessRepository.stop()
        audioRepository.stop()
        hvacRepository.stop()
        super.onCleared()
    }

    private companion object {
        const val BRIGHTNESS_WRITE_INTERVAL_MS = 50L
        const val TEMPERATURE_STEP = 0.5f
    }
}

class SystemUiViewModelFactory(
    private val navigationRepository: NavigationRepository,
    private val brightnessRepository: BrightnessRepository,
    private val audioRepository: AudioRepository,
    private val hvacRepository: HvacRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(SystemUiViewModel::class.java) ->
            SystemUiViewModel(navigationRepository) as T
        modelClass.isAssignableFrom(QuickControlViewModel::class.java) ->
            QuickControlViewModel(brightnessRepository, audioRepository, hvacRepository) as T
        else -> error("Unknown ViewModel ${modelClass.name}")
    }
}
