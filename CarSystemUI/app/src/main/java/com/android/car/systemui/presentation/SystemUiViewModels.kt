package com.android.car.systemui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.model.ExtendedControlsState
import com.android.car.systemui.data.repository.AudioRepository
import com.android.car.systemui.data.repository.AudioLevelMapper
import com.android.car.systemui.data.repository.BrightnessRepository
import com.android.car.systemui.data.repository.HvacRepository
import com.android.car.systemui.data.repository.ExtendedControlsRepository
import com.miniivi.car.api.ClimateWindow
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

data class SystemUiState(val controlCenterVisible: Boolean = false)

data class ControlCenterUiState(
    val brightness: BrightnessState = BrightnessState(),
    val audio: AudioState = AudioState(),
    val hvac: HvacState = HvacState(),
    val brightnessPreview: Float? = null,
    val extendedControls: ExtendedControlsState = ExtendedControlsState(),
    val moreClimateVisible: Boolean = false,
    val cameraVisible: Boolean = false,
    val screenCurtainVisible: Boolean = false,
) {
    val displayedBrightness: Float
        get() = brightnessPreview ?: brightness.progress
}

class SystemUiViewModel(
    private val navigationRepository: NavigationRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SystemUiState())
    val state = mutableState.asStateFlow()

    fun toggleControlCenter() {
        val visible = !mutableState.value.controlCenterVisible
        mutableState.value = mutableState.value.copy(controlCenterVisible = visible)
    }

    fun dismissControlCenter() {
        mutableState.value = mutableState.value.copy(controlCenterVisible = false)
    }

    fun goHome() {
        dismissControlCenter()
        navigationRepository.goHome()
    }

    fun openSettings() {
        dismissControlCenter()
        navigationRepository.openSettings()
    }

    fun openAppList() {
        dismissControlCenter()
        navigationRepository.openAppList()
    }

}

@OptIn(kotlinx.coroutines.FlowPreview::class)
class ControlCenterViewModel(
    private val navigationRepository: NavigationRepository,
    private val brightnessRepository: BrightnessRepository,
    private val audioRepository: AudioRepository,
    private val hvacRepository: HvacRepository,
    private val extendedControlsRepository: ExtendedControlsRepository,
) : ViewModel() {
    private val brightnessPreview = MutableStateFlow<Float?>(null)
    private val brightnessChanges = MutableSharedFlow<Float>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val moreClimateVisible = MutableStateFlow(false)
    private val cameraVisible = MutableStateFlow(false)
    private val screenCurtainVisible = MutableStateFlow(false)

    val state: StateFlow<ControlCenterUiState> = combine(
        brightnessRepository.state,
        audioRepository.state,
        hvacRepository.state,
        brightnessPreview,
    ) { brightness, audio, hvac, preview ->
        ControlCenterUiState(brightness, audio, hvac, preview)
    }.combine(extendedControlsRepository.state) { base, controls ->
        base.copy(extendedControls = controls)
    }.combine(moreClimateVisible) { base, visible ->
        base.copy(moreClimateVisible = visible)
    }.combine(cameraVisible) { base, visible ->
        base.copy(cameraVisible = visible)
    }.combine(screenCurtainVisible) { base, visible ->
        base.copy(screenCurtainVisible = visible)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ControlCenterUiState(),
    )

    init {
        brightnessRepository.start()
        audioRepository.start()
        hvacRepository.start()
        extendedControlsRepository.start()
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
        extendedControlsRepository.refresh()
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
        if (volume != audio.volume) {
            audioRepository.setVolume(volume)
        }
    }

    fun onVolumeChangeFinished() = Unit

    fun decreaseTemperature(zone: ClimateZone) =
        hvacRepository.adjustTemperature(zone, -TEMPERATURE_STEP)

    fun increaseTemperature(zone: ClimateZone) =
        hvacRepository.adjustTemperature(zone, TEMPERATURE_STEP)

    fun setAc(enabled: Boolean) = hvacRepository.setAc(enabled)

    fun setPower(enabled: Boolean) = extendedControlsRepository.setPower(enabled)
    fun setAuto(enabled: Boolean) = extendedControlsRepository.setAuto(enabled)
    fun setSync(enabled: Boolean) = extendedControlsRepository.setSync(enabled)
    fun setRecirculation(enabled: Boolean) = extendedControlsRepository.setRecirculation(enabled)
    fun setFanSpeed(zone: ClimateZone, speed: Int) =
        extendedControlsRepository.setFanSpeed(zone, speed)
    fun setFanDirection(zone: ClimateZone, direction: Int) =
        extendedControlsRepository.setFanDirection(zone, direction)
    fun setFrontDefrost(enabled: Boolean) =
        extendedControlsRepository.setDefroster(ClimateWindow.FRONT, enabled)
    fun setRearDefrost(enabled: Boolean) =
        extendedControlsRepository.setDefroster(ClimateWindow.REAR, enabled)
    fun setSeatHeating(zone: ClimateZone, level: Int) =
        extendedControlsRepository.setSeatHeating(zone, level)
    fun setSeatVentilation(zone: ClimateZone, level: Int) =
        extendedControlsRepository.setSeatVentilation(zone, level)
    fun setMaxAc(enabled: Boolean) = extendedControlsRepository.setMaxAc(enabled)
    fun setMaxDefrost(enabled: Boolean) = extendedControlsRepository.setMaxDefrost(enabled)
    fun setAutoRecirculation(enabled: Boolean) =
        extendedControlsRepository.setAutoRecirculation(enabled)
    fun setSteeringWheelHeat(level: Int) =
        extendedControlsRepository.setSteeringWheelHeat(level)
    fun setTemperatureUnit(unit: Int) = extendedControlsRepository.setTemperatureUnit(unit)
    fun setQuickControl(control: Int, enabled: Boolean) =
        extendedControlsRepository.setQuickControl(control, enabled)
    fun showMoreClimate() { moreClimateVisible.value = true }
    fun hideMoreClimate() { moreClimateVisible.value = false }
    fun openWifiSettings() = navigationRepository.openWifiSettings()
    fun openWirelessSettings() = navigationRepository.openWirelessSettings()
    fun openBluetoothSettings() = navigationRepository.openBluetoothSettings()
    fun openCamera() {
        cameraVisible.value = !navigationRepository.openCamera()
    }
    fun hideCamera() { cameraVisible.value = false }
    fun requestScreenOff() {
        extendedControlsRepository.requestScreenOff()
        screenCurtainVisible.value = true
    }
    fun dismissScreenCurtain() { screenCurtainVisible.value = false }

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
    private val extendedControlsRepository: ExtendedControlsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(SystemUiViewModel::class.java) ->
            SystemUiViewModel(navigationRepository) as T
        modelClass.isAssignableFrom(ControlCenterViewModel::class.java) ->
            ControlCenterViewModel(
                navigationRepository,
                brightnessRepository,
                audioRepository,
                hvacRepository,
                extendedControlsRepository,
            ) as T
        else -> error("Unknown ViewModel ${modelClass.name}")
    }
}
