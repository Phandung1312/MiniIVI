package com.android.car.launcher.feature.bluetooth.model

sealed interface BluetoothEvent {

    data object BluetoothOn : BluetoothEvent

    data object BluetoothOff : BluetoothEvent

    data class DeviceConnected(
        val address: String,
        val name: String?
    ) : BluetoothEvent

    data class DeviceDisconnected(
        val address: String,
        val name: String?
    ) : BluetoothEvent

    data class DeviceNameChanged(
        val address: String,
        val name: String?
    ) : BluetoothEvent

    data class BondStateChanged(
        val address: String,
        val name: String?,
        val state: Int
    ) : BluetoothEvent
}