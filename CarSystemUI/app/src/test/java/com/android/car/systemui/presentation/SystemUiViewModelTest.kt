package com.android.car.systemui.presentation

import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.repository.AudioRepository
import com.android.car.systemui.data.repository.BrightnessRepository
import com.android.car.systemui.data.repository.HvacRepository
import com.android.car.systemui.data.repository.NavigationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SystemUiViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun quickControlCanBeToggledAndDismissed() {
        val viewModel = SystemUiViewModel(FakeNavigationRepository())
        assertFalse(viewModel.state.value.quickControlVisible)
        viewModel.toggleQuickControl()
        assertTrue(viewModel.state.value.quickControlVisible)
        viewModel.dismissQuickControl()
        assertFalse(viewModel.state.value.quickControlVisible)
    }

    @Test
    fun navigationActionsAreDelegated() {
        val repository = FakeNavigationRepository()
        val viewModel = SystemUiViewModel(repository)
        viewModel.goHome()
        viewModel.openSettings()
        viewModel.openAppList()
        assertEquals(listOf("home", "settings", "apps"), repository.actions)
    }

    @Test
    fun quickControlCombinesStateAndForwardsActions() {
        val brightness = FakeBrightnessRepository()
        val audio = FakeAudioRepository()
        val hvac = FakeHvacRepository()
        val viewModel = QuickControlViewModel(brightness, audio, hvac)
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
    }
}

private class FakeNavigationRepository : NavigationRepository {
    val actions = mutableListOf<String>()
    override fun goHome() { actions += "home" }
    override fun openSettings() { actions += "settings" }
    override fun openAppList() { actions += "apps" }
}

private class FakeBrightnessRepository : BrightnessRepository {
    val mutable = MutableStateFlow(BrightnessState())
    override val state = mutable
    var lastValue: Float? = null
    override fun start() = Unit
    override fun stop() = Unit
    override fun refresh() = Unit
    override suspend fun setBrightness(progress: Float) { lastValue = progress }
}

private class FakeAudioRepository : AudioRepository {
    val mutable = MutableStateFlow(AudioState())
    override val state = mutable
    var lastVolume = -1
    override fun start() = Unit
    override fun stop() = Unit
    override fun refresh() = Unit
    override fun setVolume(volume: Int) { lastVolume = volume }
}

private class FakeHvacRepository : HvacRepository {
    val mutable = MutableStateFlow(HvacState())
    override val state = mutable
    var lastAdjustment: Pair<ClimateZone, Float>? = null
    var lastAc = false
    override fun start() = Unit
    override fun stop() = Unit
    override fun adjustTemperature(zone: ClimateZone, delta: Float) {
        lastAdjustment = zone to delta
    }
    override fun setAc(enabled: Boolean) { lastAc = enabled }
}
