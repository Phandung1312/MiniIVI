package com.android.car.launcher.feature.bluetooth.model

data class BluetoothState(
    val supported: Boolean = true,
    val permissionGranted: Boolean = false,
    val enabled: Boolean = false,
    val discovering: Boolean = false,
    val pairedDevices: List<DeviceInfo> = emptyList(),
    val nearbyDevices: List<DeviceInfo> = emptyList(),
    val connectedDevices: List<DeviceInfo> = emptyList(),
)

data class DeviceInfo(
    val address: String,
    val name: String?,
    val bonded: Boolean = false,
)
