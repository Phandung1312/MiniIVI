package com.android.car.launcher.feature.bluetooth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.car.launcher.feature.bluetooth.domain.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.domain.usecase.EnableBluetoothUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.ObserveBluetoothStateUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.RefreshBluetoothUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.RenameBluetoothDeviceUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.StartBluetoothDiscoveryUseCase
import com.android.car.launcher.feature.bluetooth.domain.usecase.StartBluetoothUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    observeBluetoothState: ObserveBluetoothStateUseCase,
    private val startBluetooth: StartBluetoothUseCase,
    private val refreshBluetooth: RefreshBluetoothUseCase,
    private val enableBluetooth: EnableBluetoothUseCase,
    private val startDiscovery: StartBluetoothDiscoveryUseCase,
    private val renameDevice: RenameBluetoothDeviceUseCase,
) : ViewModel() {
    val state = observeBluetoothState()
        .stateIn(viewModelScope, SharingStarted.Eagerly, BluetoothState())

    init {
        startBluetooth()
        refreshBluetooth()
    }

    fun refresh() = refreshBluetooth()

    fun startDiscovery() {
        startDiscovery.invoke()
    }

    fun enable() {
        enableBluetooth.invoke()
    }

    fun renameLocalDevice(name: String): Boolean = renameDevice(name)
}
