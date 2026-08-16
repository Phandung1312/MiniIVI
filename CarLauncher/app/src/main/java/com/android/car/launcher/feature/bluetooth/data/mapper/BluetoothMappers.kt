package com.android.car.launcher.feature.bluetooth.data.mapper

import com.android.car.launcher.feature.bluetooth.domain.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.domain.model.DeviceInfo
import com.miniivi.car.api.BluetoothDeviceInfo
import com.miniivi.car.api.BluetoothFeatureState

fun BluetoothFeatureState.toDomain(): BluetoothState = BluetoothState(
    supported = supported,
    enabled = enabled,
    discovering = discovering,
    localName = localName,
    localAddress = localAddress,
    pairedDevices = pairedDevices.map(BluetoothDeviceInfo::toDomain),
    nearbyDevices = nearbyDevices.map(BluetoothDeviceInfo::toDomain),
    connectedDevices = connectedDevices.map(BluetoothDeviceInfo::toDomain),
)

private fun BluetoothDeviceInfo.toDomain() = DeviceInfo(
    address = address,
    name = name,
    bonded = bonded,
)
