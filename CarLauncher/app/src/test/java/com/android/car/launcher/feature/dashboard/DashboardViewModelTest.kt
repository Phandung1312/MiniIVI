package com.android.car.launcher.feature.dashboard.presentation.viewmodel

import com.android.car.launcher.feature.dashboard.domain.model.HvacState
import com.android.car.launcher.feature.dashboard.domain.model.VehicleStatusState
import com.android.car.launcher.feature.dashboard.domain.repository.HvacRepository
import com.android.car.launcher.feature.dashboard.domain.repository.LauncherNavigationDestination
import com.android.car.launcher.feature.dashboard.domain.repository.NavigationStateReporter
import com.android.car.launcher.feature.dashboard.domain.repository.VehicleRepository
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
import com.android.car.launcher.feature.maps.data.repository.MapLaunchTargetRepositoryImpl
import com.android.car.launcher.feature.maps.domain.usecase.ResolveMapLaunchTargetUseCase
import com.android.car.launcher.feature.media.domain.model.MediaState
import com.android.car.launcher.feature.media.domain.repository.MediaRepository
import com.android.car.launcher.feature.media.domain.usecase.LoadMediaLibraryUseCase
import com.android.car.launcher.feature.media.domain.usecase.ObserveMediaStateUseCase
import com.android.car.launcher.feature.media.domain.usecase.PlayNextMediaTrackUseCase
import com.android.car.launcher.feature.media.domain.usecase.PlayPreviousMediaTrackUseCase
import com.android.car.launcher.feature.media.domain.usecase.ToggleMediaPlaybackUseCase
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
        val media = FakeMediaRepository()
        val hvac = FakeHvacRepository()
        val vehicle = FakeVehicleRepository()
        val reporter = FakeNavigationStateReporter()
        val viewModel = createViewModel(media, hvac, vehicle, reporter)
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

        viewModel.onResumed()
        viewModel.onPlayPause()
        viewModel.onNext()
        viewModel.onPrevious()

        assertEquals(1, hvac.refreshCount)
        assertEquals(1, vehicle.refreshCount)
        assertEquals(listOf("toggle", "next", "previous"), media.actions)
        assertEquals(LauncherNavigationDestination.Home, reporter.destinations.last())
    }
}

private fun createViewModel(
    media: FakeMediaRepository,
    hvac: FakeHvacRepository,
    vehicle: FakeVehicleRepository,
    reporter: FakeNavigationStateReporter,
) = DashboardViewModel(
    observeMedia = ObserveDashboardMediaUseCase(ObserveMediaStateUseCase(media)),
    observeHvac = ObserveHvacStateUseCase(hvac),
    observeVehicle = ObserveVehicleStatusUseCase(vehicle),
    startHvac = StartHvacMonitoringUseCase(hvac),
    startVehicle = StartVehicleMonitoringUseCase(vehicle),
    refreshHvac = RefreshHvacStateUseCase(hvac),
    refreshVehicle = RefreshVehicleStatusUseCase(vehicle),
    loadMedia = LoadDashboardMediaUseCase(LoadMediaLibraryUseCase(media)),
    togglePlayback = ToggleDashboardPlaybackUseCase(ToggleMediaPlaybackUseCase(media)),
    playNext = PlayNextDashboardTrackUseCase(PlayNextMediaTrackUseCase(media)),
    playPrevious = PlayPreviousDashboardTrackUseCase(PlayPreviousMediaTrackUseCase(media)),
    openApp = OpenDashboardAppUseCase(
        ResolveMapLaunchTargetUseCase(MapLaunchTargetRepositoryImpl()),
    ),
    reportNavigationState = ReportNavigationStateUseCase(reporter),
)

private class FakeMediaRepository : MediaRepository {
    override val state = MutableStateFlow(MediaState())
    var loadCount = 0
    val actions = mutableListOf<String>()

    override suspend fun loadSongs() {
        loadCount++
    }

    override fun togglePlayback() { actions += "toggle" }
    override fun playNext() { actions += "next" }
    override fun playPrevious() { actions += "previous" }
    override fun play(index: Int) { actions += "play:$index" }
    override fun releasePlayer() = Unit
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

private class FakeNavigationStateReporter : NavigationStateReporter {
    val destinations = mutableListOf<LauncherNavigationDestination>()
    override fun report(destination: LauncherNavigationDestination) {
        destinations += destination
    }
}
