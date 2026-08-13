package com.android.car.launcher.feature.dashboard

import com.android.car.launcher.feature.dashboard.repository.HvacRepository
import com.android.car.launcher.feature.dashboard.repository.VehicleRepository
import com.android.car.launcher.feature.media.MediaController
import com.android.car.launcher.feature.media.MediaState
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.VehicleStatusState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun combinesRepositoryStateAndDelegatesLifecycleAndActions() = runTest(dispatcher) {
        val media = FakeMediaController()
        val hvac = FakeHvacRepository()
        val vehicle = FakeVehicleRepository()
        val viewModel = DashboardViewModel(media, hvac, vehicle)
        runCurrent()

        assertEquals(1, hvac.startCount)
        assertEquals(1, vehicle.startCount)
        assertEquals(1, media.loadCount)

        val hvacState = HvacState(available = true, cabinTemperatureCelsius = 24f)
        val vehicleState = VehicleStatusState(available = true, batteryPercentage = 65f)
        hvac.mutable.value = hvacState
        vehicle.mutable.value = vehicleState
        runCurrent()

        assertEquals(hvacState, viewModel.state.value.hvac)
        assertEquals(vehicleState, viewModel.state.value.vehicleStatus)

        viewModel.refresh()
        viewModel.onPlayPause()
        viewModel.onNext()
        viewModel.onPrevious()

        assertEquals(1, hvac.refreshCount)
        assertEquals(1, vehicle.refreshCount)
        assertEquals(listOf("toggle", "next", "previous"), media.actions)
    }
}

private class FakeMediaController : MediaController {
    override val state = MutableStateFlow(MediaState())
    var loadCount = 0
    val actions = mutableListOf<String>()
    override suspend fun loadSongs() { loadCount++ }
    override fun togglePlayback() { actions += "toggle" }
    override fun playNext() { actions += "next" }
    override fun playPrevious() { actions += "previous" }
}

private class FakeHvacRepository : HvacRepository {
    val mutable = MutableStateFlow(HvacState())
    override val state = mutable
    var startCount = 0
    var refreshCount = 0
    override fun start() { startCount++ }
    override fun refresh() { refreshCount++ }
}

private class FakeVehicleRepository : VehicleRepository {
    val mutable = MutableStateFlow(VehicleStatusState())
    override val state = mutable
    var startCount = 0
    var refreshCount = 0
    override fun start() { startCount++ }
    override fun refresh() { refreshCount++ }
}
