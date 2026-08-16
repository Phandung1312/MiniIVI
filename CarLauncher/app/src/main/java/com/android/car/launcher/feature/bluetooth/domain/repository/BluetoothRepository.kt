package com.android.car.launcher.feature.bluetooth.domain.repository

import com.android.car.launcher.feature.bluetooth.domain.model.BluetoothState
import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {
    val state: Flow<BluetoothState>

    fun start()
    fun refresh()
    fun enable(): Boolean
    fun startDiscovery(): Boolean
    fun renameLocalDevice(name: String): Boolean
}
