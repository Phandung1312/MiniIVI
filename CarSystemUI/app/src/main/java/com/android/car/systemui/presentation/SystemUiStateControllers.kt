package com.android.car.systemui.presentation

import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.ExtendedControlsState
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.repository.AudioLevelMapper
import com.android.car.systemui.data.repository.AudioRepository
import com.android.car.systemui.data.repository.BrightnessRepository
import com.android.car.systemui.data.repository.ExtendedControlsRepository
import com.android.car.systemui.data.repository.HvacRepository
import com.android.car.systemui.data.repository.NavigationRepository
import com.android.car.systemui.di.ApplicationScope
import com.miniivi.car.api.ClimateWindow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

enum class NavigationDestination {
    HOME,
    APP_LIST,
    PHONE,
    SETTINGS,
    CONTROL_CENTER,
    NONE,
}

data class SystemUiState(
    val controlCenterVisible: Boolean = false,
    val selectedDestination: NavigationDestination = NavigationDestination.HOME,
)

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

@Singleton
class SystemUiStateController @Inject constructor(
    private val navigationRepository: NavigationRepository,
) {
    private val mutableState = MutableStateFlow(SystemUiState())
    private var destinationBeforeControlCenter = NavigationDestination.HOME
    val state = mutableState.asStateFlow()

    fun toggleControlCenter() {
        val current = mutableState.value
        if (current.controlCenterVisible) {
            mutableState.value = current.copy(
                controlCenterVisible = false,
                selectedDestination = destinationBeforeControlCenter,
            )
        } else {
            destinationBeforeControlCenter = current.selectedDestination
            mutableState.value = current.copy(
                controlCenterVisible = true,
                selectedDestination = NavigationDestination.CONTROL_CENTER,
            )
        }
    }

    fun dismissControlCenter() {
        if (!mutableState.value.controlCenterVisible) return
        mutableState.value = mutableState.value.copy(
            controlCenterVisible = false,
            selectedDestination = destinationBeforeControlCenter,
        )
    }

    fun goHome() {
        selectDestination(NavigationDestination.HOME)
        navigationRepository.goHome()
    }

    fun openSettings() {
        selectDestination(NavigationDestination.SETTINGS)
        navigationRepository.openSettings()
    }

    fun openAppList() {
        selectDestination(NavigationDestination.APP_LIST)
        navigationRepository.openAppList()
    }

    fun openPhone() {
        selectDestination(NavigationDestination.PHONE)
        navigationRepository.openPhone()
    }

    fun onExternalAppOpened() = selectDestination(NavigationDestination.NONE)

    fun onLauncherDestinationChanged(destination: NavigationDestination) {
        require(
            destination == NavigationDestination.HOME ||
                destination == NavigationDestination.APP_LIST ||
                destination == NavigationDestination.NONE,
        ) {
            "Unsupported launcher destination: $destination"
        }
        selectDestination(destination)
    }

    private fun selectDestination(destination: NavigationDestination) {
        if (destination != NavigationDestination.CONTROL_CENTER) {
            destinationBeforeControlCenter = destination
        }
        mutableState.value = mutableState.value.copy(
            controlCenterVisible = false,
            selectedDestination = destination,
        )
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Singleton
class ControlCenterStateController @Inject constructor(
    private val navigationRepository: NavigationRepository,
    private val brightnessRepository: BrightnessRepository,
    private val audioRepository: AudioRepository,
    private val hvacRepository: HvacRepository,
    private val extendedControlsRepository: ExtendedControlsRepository,
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
        brightnessRepository.start()
        audioRepository.start()
        hvacRepository.start()
        extendedControlsRepository.start()
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
        brightnessRepository.stop()
        audioRepository.stop()
        hvacRepository.stop()
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
        val volume = AudioLevelMapper.toVolume(progress, audio.minimum, audio.maximum)
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
