package com.android.car.launcher.feature.bluetooth.presentation.viewmodel

import com.android.car.launcher.feature.bluetooth.domain.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.domain.repository.BluetoothRepository
import com.android.car.launcher.feature.bluetooth.domain.usecase.EnableBluetoothUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.ObserveBluetoothStateUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.RefreshBluetoothUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.RenameBluetoothDeviceUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.StartBluetoothDiscoveryUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.StartBluetoothUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsRefreshesAndDelegatesBluetoothCommands() = runTest(dispatcher) {
        val repository = FakeBluetoothRepository()
        val viewModel = BluetoothViewModel(
            observeBluetoothState = ObserveBluetoothStateUseCase(repository),
            startBluetooth = StartBluetoothUseCase(repository),
            refreshBluetooth = RefreshBluetoothUseCase(repository),
            enableBluetooth = EnableBluetoothUseCase(repository),
            startDiscovery = StartBluetoothDiscoveryUseCase(repository),
            renameDevice = RenameBluetoothDeviceUseCase(repository),
        )

        assertEquals(1, repository.startCount)
        assertEquals(1, repository.refreshCount)

        repository.mutable.value = BluetoothState(supported = true, enabled = true)
        runCurrent()
        assertEquals(true, viewModel.state.value.enabled)
        viewModel.enable()
        viewModel.startDiscovery()
        assertTrue(viewModel.renameLocalDevice("MiniIVI"))

        assertEquals(listOf("enable", "discover", "rename:MiniIVI"), repository.actions)
    }
}

private class FakeBluetoothRepository : BluetoothRepository {
    val mutable = MutableStateFlow(BluetoothState())
    override val state = mutable
    var startCount = 0
    var refreshCount = 0
    val actions = mutableListOf<String>()

    override fun start() { startCount++ }
    override fun refresh() { refreshCount++ }
    override fun enable(): Boolean {
        actions += "enable"
        return true
    }
    override fun startDiscovery(): Boolean {
        actions += "discover"
        return true
    }
    override fun renameLocalDevice(name: String): Boolean {
        actions += "rename:$name"
        return true
    }
}
