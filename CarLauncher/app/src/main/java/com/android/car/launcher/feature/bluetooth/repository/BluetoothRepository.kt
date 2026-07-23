package com.android.car.launcher.feature.bluetooth.repository

import com.android.car.launcher.feature.bluetooth.model.BluetoothEvent
import com.android.car.launcher.feature.bluetooth.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.model.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothRepository @Inject constructor()  {

    private val _state = MutableStateFlow(
        BluetoothState()
    )

    val bluetoothState =
        _state.asStateFlow()

    suspend fun onEvent(
        event: BluetoothEvent
    ) {

        when (event) {

            BluetoothEvent.BluetoothOn -> {
                _state.update {
                    it.copy(enabled = true)
                }
            }

            BluetoothEvent.BluetoothOff -> {
                _state.update {
                    it.copy(
                        enabled = false,
                        connectedDevices = emptyList()
                    )
                }
            }

            is BluetoothEvent.DeviceConnected -> {

                val device = DeviceInfo(
                    event.address,
                    event.name
                )

                _state.update {
                    it.copy(
                        latestConnectedDevice = device,
                        connectedDevices =
                            (it.connectedDevices + device)
                                .distinctBy { d -> d.address }
                    )
                }
            }

            is BluetoothEvent.DeviceDisconnected -> {

                val device = DeviceInfo(
                    event.address,
                    event.name
                )

                _state.update {
                    it.copy(
                        latestDisconnectedDevice = device,
                        connectedDevices =
                            it.connectedDevices.filter {
                                    d ->
                                d.address != event.address
                            }
                    )
                }
            }

            is BluetoothEvent.DeviceNameChanged -> {

                _state.update {

                    val updated =
                        it.connectedDevices.map { dev ->

                            if (dev.address == event.address)
                                dev.copy(name = event.name)
                            else
                                dev
                        }

                    it.copy(
                        connectedDevices = updated
                    )
                }
            }

            is BluetoothEvent.BondStateChanged -> {

            }
        }
    }
}