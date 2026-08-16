package com.android.car.systemui.presentation

import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.model.ExtendedControlsState
import com.android.car.systemui.data.repository.AudioRepository
import com.android.car.systemui.data.repository.BrightnessRepository
import com.android.car.systemui.data.repository.HvacRepository
import com.android.car.systemui.data.repository.ExtendedControlsRepository
import com.android.car.systemui.data.repository.NavigationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SystemUiStateControllerTest {
    private val dispatcher = StandardTestDispatcher()
    private val applicationScope = CoroutineScope(dispatcher)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() {
        applicationScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun controlCenterCanBeToggledAndDismissed() {
        val viewModel = SystemUiStateController(FakeNavigationRepository())
        assertFalse(viewModel.state.value.controlCenterVisible)
        viewModel.toggleControlCenter()
        assertTrue(viewModel.state.value.controlCenterVisible)
        viewModel.dismissControlCenter()
        assertFalse(viewModel.state.value.controlCenterVisible)
    }

    @Test
    fun navigationActionsAreDelegated() {
        val repository = FakeNavigationRepository()
        val viewModel = SystemUiStateController(repository)
        assertEquals(NavigationDestination.HOME, viewModel.state.value.selectedDestination)
        viewModel.toggleControlCenter()
        viewModel.goHome()
        assertFalse(viewModel.state.value.controlCenterVisible)
        assertEquals(NavigationDestination.HOME, viewModel.state.value.selectedDestination)
        viewModel.toggleControlCenter()
        viewModel.openSettings()
        assertFalse(viewModel.state.value.controlCenterVisible)
        assertEquals(NavigationDestination.SETTINGS, viewModel.state.value.selectedDestination)
        viewModel.toggleControlCenter()
        viewModel.openAppList()
        assertFalse(viewModel.state.value.controlCenterVisible)
        assertEquals(NavigationDestination.APP_LIST, viewModel.state.value.selectedDestination)
        viewModel.openPhone()
        assertFalse(viewModel.state.value.controlCenterVisible)
        assertEquals(NavigationDestination.PHONE, viewModel.state.value.selectedDestination)
        assertEquals(listOf("home", "settings", "apps", "phone"), repository.actions)
    }

    @Test
    fun navigationSelectionIsExclusiveAndRestoresAfterControlCenterDismissal() {
        val viewModel = SystemUiStateController(FakeNavigationRepository())

        viewModel.openAppList()
        assertEquals(NavigationDestination.APP_LIST, viewModel.state.value.selectedDestination)

        viewModel.toggleControlCenter()
        assertTrue(viewModel.state.value.controlCenterVisible)
        assertEquals(
            NavigationDestination.CONTROL_CENTER,
            viewModel.state.value.selectedDestination,
        )

        viewModel.dismissControlCenter()
        assertFalse(viewModel.state.value.controlCenterVisible)
        assertEquals(NavigationDestination.APP_LIST, viewModel.state.value.selectedDestination)

        viewModel.onExternalAppOpened()
        assertFalse(viewModel.state.value.controlCenterVisible)
        assertEquals(NavigationDestination.NONE, viewModel.state.value.selectedDestination)

        viewModel.onLauncherDestinationChanged(NavigationDestination.HOME)
        assertEquals(NavigationDestination.HOME, viewModel.state.value.selectedDestination)
    }

    @Test
    fun controlCenterCombinesStateAndForwardsActions() {
        val brightness = FakeBrightnessRepository()
        val audio = FakeAudioRepository()
        val hvac = FakeHvacRepository()
        val viewModel = ControlCenterStateController(
            FakeNavigationRepository(),
            brightness,
            audio,
            hvac,
            FakeExtendedControlsRepository(),
            applicationScope,
        )
        viewModel.start()
        brightness.mutable.value = BrightnessState(progress = 0.25f, available = true)
        audio.mutable.value = AudioState(volume = 5, minimum = 0, maximum = 10, available = true)
        hvac.mutable.value = HvacState(connecting = false, available = true)
        dispatcher.scheduler.runCurrent()

        assertEquals(0.25f, viewModel.state.value.displayedBrightness, 0.001f)
        assertEquals(0.5f, viewModel.state.value.audio.progress, 0.001f)

        viewModel.onBrightnessChanged(0.8f)
        dispatcher.scheduler.runCurrent()
        assertEquals(0.8f, viewModel.state.value.brightnessPreview ?: 0f, 0.001f)
        dispatcher.scheduler.advanceTimeBy(60)
        dispatcher.scheduler.runCurrent()
        assertEquals(0.8f, brightness.lastValue ?: 0f, 0.001f)

        viewModel.onVolumeChanged(0.7f)
        assertEquals(7, audio.lastVolume)
        viewModel.increaseTemperature(ClimateZone.RIGHT)
        viewModel.setAc(true)
        assertEquals(ClimateZone.RIGHT to 0.5f, hvac.lastAdjustment)
        assertTrue(hvac.lastAc)

        viewModel.refresh()
        assertEquals(1, brightness.refreshCount)
        assertEquals(1, audio.refreshCount)
        assertEquals(1, hvac.refreshCount)
    }

    @Test
    fun systemActionsAreDelegatedAndCameraFallsBackOnlyWhenUnavailable() {
        val navigation = FakeNavigationRepository()
        val viewModel = ControlCenterStateController(
            navigation,
            FakeBrightnessRepository(),
            FakeAudioRepository(),
            FakeHvacRepository(),
            FakeExtendedControlsRepository(),
            applicationScope,
        )

        viewModel.openWifiSettings()
        viewModel.openWirelessSettings()
        viewModel.openBluetoothSettings()
        navigation.cameraAvailable = true
        viewModel.openCamera()
        dispatcher.scheduler.runCurrent()
        assertFalse(viewModel.state.value.cameraVisible)

        navigation.cameraAvailable = false
        viewModel.openCamera()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.state.value.cameraVisible)
        assertEquals(listOf("wifi", "wireless", "bluetooth", "camera", "camera"), navigation.actions)
    }
}

private class FakeNavigationRepository : NavigationRepository {
    val actions = mutableListOf<String>()
    var cameraAvailable = false
    override fun goHome() { actions += "home" }
    override fun openSettings() { actions += "settings" }
    override fun openAppList() { actions += "apps" }
    override fun openPhone() { actions += "phone" }
    override fun openWifiSettings() { actions += "wifi" }
    override fun openWirelessSettings() { actions += "wireless" }
    override fun openBluetoothSettings() { actions += "bluetooth" }
    override fun openCamera(): Boolean {
        actions += "camera"
        return cameraAvailable
    }
}

private class FakeBrightnessRepository : BrightnessRepository {
    val mutable = MutableStateFlow(BrightnessState())
    override val state = mutable
    var lastValue: Float? = null
    var refreshCount = 0
    override fun start() = Unit
    override fun stop() = Unit
    override fun refresh() { refreshCount++ }
    override suspend fun setBrightness(progress: Float) { lastValue = progress }
}

private class FakeAudioRepository : AudioRepository {
    val mutable = MutableStateFlow(AudioState())
    override val state = mutable
    var lastVolume = -1
    var refreshCount = 0
    override fun start() = Unit
    override fun stop() = Unit
    override fun refresh() { refreshCount++ }
    override fun setVolume(volume: Int) { lastVolume = volume }
}

private class FakeHvacRepository : HvacRepository {
    val mutable = MutableStateFlow(HvacState())
    override val state = mutable
    var lastAdjustment: Pair<ClimateZone, Float>? = null
    var lastAc = false
    var refreshCount = 0
    override fun start() = Unit
    override fun stop() = Unit
    override fun refresh() { refreshCount++ }
    override fun adjustTemperature(zone: ClimateZone, delta: Float) {
        lastAdjustment = zone to delta
    }
    override fun setAc(enabled: Boolean) { lastAc = enabled }
}

private class FakeExtendedControlsRepository : ExtendedControlsRepository {
    override val state = MutableStateFlow(ExtendedControlsState())
    override fun start() = Unit
    override fun refresh() = Unit
    override fun setPower(enabled: Boolean) = Unit
    override fun setAuto(enabled: Boolean) = Unit
    override fun setSync(enabled: Boolean) = Unit
    override fun setRecirculation(enabled: Boolean) = Unit
    override fun setFanSpeed(zone: ClimateZone, speed: Int) = Unit
    override fun setFanDirection(zone: ClimateZone, direction: Int) = Unit
    override fun setDefroster(window: Int, enabled: Boolean) = Unit
    override fun setSeatHeating(zone: ClimateZone, level: Int) = Unit
    override fun setSeatVentilation(zone: ClimateZone, level: Int) = Unit
    override fun setMaxAc(enabled: Boolean) = Unit
    override fun setMaxDefrost(enabled: Boolean) = Unit
    override fun setAutoRecirculation(enabled: Boolean) = Unit
    override fun setSteeringWheelHeat(level: Int) = Unit
    override fun setTemperatureUnit(unit: Int) = Unit
    override fun setQuickControl(control: Int, enabled: Boolean) = Unit
    override fun requestScreenOff() = Unit
}
