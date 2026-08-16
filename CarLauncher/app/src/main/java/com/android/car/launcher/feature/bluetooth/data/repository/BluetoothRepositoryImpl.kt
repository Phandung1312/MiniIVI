package com.android.car.launcher.feature.bluetooth.data.repository

import com.android.car.launcher.feature.bluetooth.data.mapper.toDomain
import com.android.car.launcher.feature.bluetooth.domain.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.domain.repository.BluetoothRepository
import com.miniivi.car.api.QuickControl
import com.miniivi.car.client.MiniIviCarClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    private val client: MiniIviCarClient,
) : BluetoothRepository {
    override val state: Flow<BluetoothState> = client.bluetoothState.map { it.toDomain() }

    override fun start() = client.start()

    override fun refresh() {
        client.refreshBluetooth()
    }

    override fun enable(): Boolean =
        client.setQuickControlEnabled(QuickControl.BLUETOOTH, true)

    override fun startDiscovery(): Boolean = client.requestBluetoothDiscovery()

    override fun renameLocalDevice(name: String): Boolean =
        client.renameLocalBluetoothDevice(name)
}
