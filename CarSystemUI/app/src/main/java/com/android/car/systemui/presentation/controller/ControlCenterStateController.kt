package com.android.car.systemui.presentation.controller

import com.android.car.systemui.domain.model.ClimateFanDirection
import com.android.car.systemui.domain.model.ClimateWindow
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.TemperatureUnit
import com.android.car.systemui.domain.policy.AudioVolumePolicy
import com.android.car.systemui.domain.repository.AudioRepository
import com.android.car.systemui.domain.repository.BrightnessRepository
import com.android.car.systemui.domain.repository.CarServiceSession
import com.android.car.systemui.domain.repository.ExtendedControlsRepository
import com.android.car.systemui.domain.repository.HvacRepository
import com.android.car.systemui.domain.repository.NavigationRepository
import com.android.car.systemui.di.ApplicationScope
import com.android.car.systemui.domain.model.QuickControl
import com.android.car.systemui.presentation.model.ControlCenterUiState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Singleton
class ControlCenterStateController @Inject constructor(
    private val navigationRepository: NavigationRepository,
    private val brightnessRepository: BrightnessRepository,
    private val audioRepository: AudioRepository,
    private val hvacRepository: HvacRepository,
    private val extendedControlsRepository: ExtendedControlsRepository,
    private val carServiceSession: CarServiceSession,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val brightnessPreview = MutableStateFlow<Float?>(null)
    private val brightnessChanges = MutableSharedFlow<Float>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val moreClimateVisible = MutableStateFlow(false)
    private val cameraVisible = MutableStateFlow(false)
    private val screenCurtainVisible = MutableStateFlow(false)
    private var brightnessJob: Job? = null
    private var started = false

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
        applicationScope,
        SharingStarted.Eagerly,
        ControlCenterUiState(),
    )

    fun start() {
        if (started) return
        started = true
        carServiceSession.start()
        brightnessJob = applicationScope.launch {
            brightnessChanges.sample(BRIGHTNESS_WRITE_INTERVAL_MS).collect { progress ->
                brightnessRepository.setBrightness(progress)
            }
        }
    }

    fun stop() {
        if (!started) return
        started = false
        brightnessJob?.cancel()
        brightnessJob = null
        carServiceSession.stop()
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
        applicationScope.launch {
            brightnessRepository.setBrightness(finalValue)
            brightnessPreview.value = null
        }
    }

    fun onVolumeChanged(progress: Float) {
        val audio = audioRepository.state.value
        if (!audio.available) return
        val volume = AudioVolumePolicy.toVolume(progress, audio.minimum, audio.maximum)
        if (volume != audio.volume) audioRepository.setVolume(volume)
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
    fun setFanDirection(zone: ClimateZone, direction: ClimateFanDirection) =
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
    fun setTemperatureUnit(unit: TemperatureUnit) =
        extendedControlsRepository.setTemperatureUnit(unit)
    fun setQuickControl(control: QuickControl, enabled: Boolean) =
        extendedControlsRepository.setQuickControl(control, enabled)
    fun showMoreClimate() { moreClimateVisible.value = true }
    fun hideMoreClimate() { moreClimateVisible.value = false }
    fun openWifiSettings() = navigationRepository.openWifiSettings()
    fun openWirelessSettings() = navigationRepository.openWirelessSettings()
    fun openBluetoothSettings() = navigationRepository.openBluetoothSettings()
    fun openCamera() { cameraVisible.value = !navigationRepository.openCamera() }
    fun hideCamera() { cameraVisible.value = false }
    fun requestScreenOff() {
        extendedControlsRepository.requestScreenOff()
        screenCurtainVisible.value = true
    }
    fun dismissScreenCurtain() { screenCurtainVisible.value = false }

    private companion object {
        const val BRIGHTNESS_WRITE_INTERVAL_MS = 50L
        const val TEMPERATURE_STEP = 0.5f
    }
}
