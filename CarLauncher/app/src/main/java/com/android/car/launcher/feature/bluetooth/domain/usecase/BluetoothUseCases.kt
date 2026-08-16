package com.android.car.launcher.feature.bluetooth.domain.usecase

import com.android.car.launcher.feature.bluetooth.domain.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.domain.repository.BluetoothRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveBluetoothStateUseCase @Inject constructor(
    private val repository: BluetoothRepository,
) {
    operator fun invoke(): Flow<BluetoothState> = repository.state
}

class StartBluetoothUseCase @Inject constructor(
    private val repository: BluetoothRepository,
) {
    operator fun invoke() = repository.start()
}

class RefreshBluetoothUseCase @Inject constructor(
    private val repository: BluetoothRepository,
) {
    operator fun invoke() = repository.refresh()
}

class EnableBluetoothUseCase @Inject constructor(
    private val repository: BluetoothRepository,
) {
    operator fun invoke(): Boolean = repository.enable()
}

class StartBluetoothDiscoveryUseCase @Inject constructor(
    private val repository: BluetoothRepository,
) {
    operator fun invoke(): Boolean = repository.startDiscovery()
}

class RenameBluetoothDeviceUseCase @Inject constructor(
    private val repository: BluetoothRepository,
) {
    operator fun invoke(name: String): Boolean = repository.renameLocalDevice(name)
}
