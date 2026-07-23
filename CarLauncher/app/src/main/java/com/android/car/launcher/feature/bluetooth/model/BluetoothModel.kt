package com.android.car.launcher.feature.bluetooth.model

data class BluetoothState(
    val enabled: Boolean = false,

    val connectedDevices: List<DeviceInfo> = emptyList(),

    val latestConnectedDevice: DeviceInfo? = null,

    val latestDisconnectedDevice: DeviceInfo? = null
)

data class DeviceInfo(
    val address: String,
    val name: String?
)