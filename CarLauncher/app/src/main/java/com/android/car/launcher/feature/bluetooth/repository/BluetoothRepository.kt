package com.android.car.launcher.feature.bluetooth.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.android.car.launcher.feature.bluetooth.model.BluetoothEvent
import com.android.car.launcher.feature.bluetooth.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.model.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val _state = MutableStateFlow(BluetoothState(supported = adapter != null))
    val state = _state.asStateFlow()

    @SuppressLint("MissingPermission")
    fun refresh(permissionGranted: Boolean) {
        if (adapter == null) {
            _state.value = BluetoothState(supported = false)
            return
        }

        if (!permissionGranted) {
            _state.update {
                it.copy(
                    permissionGranted = false,
                    pairedDevices = emptyList(),
                    nearbyDevices = emptyList(),
                )
            }
            return
        }

        try {
            val paired = adapter.bondedDevices
                .map { device ->
                    DeviceInfo(
                        address = device.address,
                        name = device.name,
                        bonded = true,
                    )
                }
                .sortedBy { it.name ?: it.address }
            _state.update {
                it.copy(
                    permissionGranted = true,
                    enabled = adapter.isEnabled,
                    discovering = adapter.isDiscovering,
                    pairedDevices = paired,
                )
            }
        } catch (error: SecurityException) {
            Log.w(TAG, "Bluetooth permission missing while refreshing state", error)
            _state.update { it.copy(permissionGranted = false) }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (adapter == null || !_state.value.permissionGranted || !_state.value.enabled) return false
        return try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            _state.update { it.copy(nearbyDevices = emptyList()) }
            adapter.startDiscovery()
        } catch (error: SecurityException) {
            Log.w(TAG, "Bluetooth permission missing while starting discovery", error)
            false
        }
    }

    fun onEvent(event: BluetoothEvent) {
        Log.d(TAG, "Broadcast event: $event")
        when (event) {
            is BluetoothEvent.AdapterStateChanged -> _state.update {
                it.copy(
                    enabled = event.enabled,
                    discovering = false,
                    nearbyDevices = if (event.enabled) it.nearbyDevices else emptyList(),
                    connectedDevices = if (event.enabled) it.connectedDevices else emptyList(),
                )
            }

            BluetoothEvent.DiscoveryStarted -> _state.update { it.copy(discovering = true) }
            BluetoothEvent.DiscoveryFinished -> _state.update { it.copy(discovering = false) }
            is BluetoothEvent.DeviceFound -> _state.update {
                it.copy(nearbyDevices = (it.nearbyDevices + event.device).distinctBy(DeviceInfo::address))
            }

            is BluetoothEvent.DeviceConnected -> _state.update {
                it.copy(
                    connectedDevices = (it.connectedDevices + event.device)
                        .distinctBy(DeviceInfo::address),
                )
            }

            is BluetoothEvent.DeviceDisconnected -> _state.update {
                it.copy(
                    connectedDevices = it.connectedDevices.filterNot { device ->
                        device.address == event.address
                    },
                )
            }

            is BluetoothEvent.DeviceNameChanged -> _state.update {
                it.copy(
                    pairedDevices = it.pairedDevices.replaceDevice(event.device),
                    nearbyDevices = it.nearbyDevices.replaceDevice(event.device),
                    connectedDevices = it.connectedDevices.replaceDevice(event.device),
                )
            }

            is BluetoothEvent.BondStateChanged -> _state.update {
                val paired = if (event.device.bonded) {
                    (it.pairedDevices + event.device).distinctBy(DeviceInfo::address)
                } else {
                    it.pairedDevices.filterNot { device -> device.address == event.device.address }
                }
                it.copy(pairedDevices = paired)
            }
        }
    }

    private fun List<DeviceInfo>.replaceDevice(updated: DeviceInfo): List<DeviceInfo> =
        map { current -> if (current.address == updated.address) updated else current }

    private companion object {
        const val TAG = "BluetoothRepository"
    }
}
