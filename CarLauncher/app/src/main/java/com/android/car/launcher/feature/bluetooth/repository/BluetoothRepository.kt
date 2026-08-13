package com.android.car.launcher.feature.bluetooth.repository

import com.android.car.launcher.feature.bluetooth.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.model.DeviceInfo
import com.miniivi.car.api.BluetoothDeviceInfo
import com.miniivi.car.api.QuickControl
import com.miniivi.car.client.MiniIviCarClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothRepository @Inject constructor(
    private val client: MiniIviCarClient,
) {
    val state: Flow<BluetoothState> = client.bluetoothState.map { remote ->
        BluetoothState(
            supported = remote.supported,
            enabled = remote.enabled,
            discovering = remote.discovering,
            localName = remote.localName,
            localAddress = remote.localAddress,
            pairedDevices = remote.pairedDevices.map { it.toUiModel() },
            nearbyDevices = remote.nearbyDevices.map { it.toUiModel() },
            connectedDevices = remote.connectedDevices.map { it.toUiModel() },
        )
    }

    fun start() = client.start()
    fun refresh() { client.refreshBluetooth() }
    fun enable(): Boolean = client.setQuickControlEnabled(QuickControl.BLUETOOTH, true)
    fun startDiscovery(): Boolean = client.requestBluetoothDiscovery()
    fun renameLocalDevice(name: String): Boolean = client.renameLocalBluetoothDevice(name)

    private fun BluetoothDeviceInfo.toUiModel() = DeviceInfo(address, name, bonded)
}
