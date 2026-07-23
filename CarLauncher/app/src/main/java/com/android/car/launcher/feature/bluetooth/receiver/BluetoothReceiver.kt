package com.android.car.launcher.feature.bluetooth.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.android.car.launcher.feature.bluetooth.model.BluetoothEvent
import com.android.car.launcher.feature.bluetooth.model.DeviceInfo
import com.android.car.launcher.feature.bluetooth.repository.BluetoothRepository
import javax.inject.Inject

/**
 * Converts Android Bluetooth broadcasts into domain events consumed by the Service.
 */
class BluetoothReceiver @Inject constructor(
    private val repository: BluetoothRepository,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> BluetoothEvent.AdapterStateChanged(true)
                    BluetoothAdapter.STATE_OFF -> BluetoothEvent.AdapterStateChanged(false)
                    else -> null
                }
            }

            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> BluetoothEvent.DiscoveryStarted
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> BluetoothEvent.DiscoveryFinished
            BluetoothDevice.ACTION_FOUND -> intent.deviceInfo()?.let(BluetoothEvent::DeviceFound)
            BluetoothDevice.ACTION_ACL_CONNECTED -> intent.deviceInfo()?.let(BluetoothEvent::DeviceConnected)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                intent.deviceInfo()?.let { BluetoothEvent.DeviceDisconnected(it.address) }
            }

            BluetoothDevice.ACTION_NAME_CHANGED -> intent.deviceInfo()?.let(BluetoothEvent::DeviceNameChanged)
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> intent.deviceInfo()?.let(BluetoothEvent::BondStateChanged)
            else -> null
        }

        if (event != null) {
            Log.d(TAG, "Received ${intent.action}")
            repository.onEvent(event)
        }
    }

    @SuppressLint("MissingPermission")
    private fun Intent.deviceInfo(): DeviceInfo? {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return null

        return try {
            DeviceInfo(
                address = device.address,
                name = device.name,
                bonded = device.bondState == BluetoothDevice.BOND_BONDED,
            )
        } catch (error: SecurityException) {
            Log.w(TAG, "Bluetooth permission missing while reading device", error)
            null
        }
    }

    private companion object {
        const val TAG = "BluetoothReceiver"
    }
}
