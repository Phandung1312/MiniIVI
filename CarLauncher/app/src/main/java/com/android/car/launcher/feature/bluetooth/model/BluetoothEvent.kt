package com.android.car.launcher.feature.bluetooth.model

sealed interface BluetoothEvent {
    data class AdapterStateChanged(val enabled: Boolean) : BluetoothEvent
    data object DiscoveryStarted : BluetoothEvent
    data object DiscoveryFinished : BluetoothEvent
    data class DeviceFound(val device: DeviceInfo) : BluetoothEvent
    data class DeviceConnected(val device: DeviceInfo) : BluetoothEvent
    data class DeviceDisconnected(val address: String) : BluetoothEvent
    data class DeviceNameChanged(val device: DeviceInfo) : BluetoothEvent
    data class BondStateChanged(val device: DeviceInfo) : BluetoothEvent
}
