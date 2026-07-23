package com.android.car.launcher.feature.bluetooth.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.IBinder
import com.android.car.launcher.feature.bluetooth.model.BluetoothEvent
import com.android.car.launcher.feature.bluetooth.repository.BluetoothRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothMonitorService : Service() {

    @Inject
    lateinit var repository: BluetoothRepository

    private val scope =
        CoroutineScope(
            Dispatchers.IO +
                    SupervisorJob()
        )

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        handleIntent(intent)

        return START_STICKY
    }

    private fun handleIntent(
        intent: Intent?
    ) {

        when (intent?.action) {

            BluetoothAdapter.ACTION_STATE_CHANGED -> {

                val state =
                    intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )

                scope.launch {

                    if (state ==
                        BluetoothAdapter.STATE_ON
                    ) {

                        repository.onEvent(
                            BluetoothEvent.BluetoothOn
                        )
                    }

                    if (state ==
                        BluetoothAdapter.STATE_OFF
                    ) {

                        repository.onEvent(
                            BluetoothEvent.BluetoothOff
                        )
                    }
                }
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {

                sendConnectedEvent(intent)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {

                sendDisconnectedEvent(intent)
            }

            BluetoothDevice.ACTION_NAME_CHANGED -> {

                sendNameChanged(intent)
            }
        }
    }

    private fun onEvent(bluetoothOn: BluetoothEvent.BluetoothOn) {}

    private fun sendConnectedEvent(
        intent: Intent
    ) {

        val device =
            intent.getParcelableExtra<BluetoothDevice>(
                BluetoothDevice.EXTRA_DEVICE
            ) ?: return

        scope.launch {

            repository.onEvent(
                BluetoothEvent.DeviceConnected(
                    address = device.address,
                    name = device.name
                )
            )
        }
    }

    private fun sendDisconnectedEvent(
        intent: Intent
    ) {

        val device =
            intent.getParcelableExtra<BluetoothDevice>(
                BluetoothDevice.EXTRA_DEVICE
            ) ?: return

        scope.launch {

            repository.onEvent(
                BluetoothEvent.DeviceDisconnected(
                    address = device.address,
                    name = device.name
                )
            )
        }
    }

    private fun sendNameChanged(
        intent: Intent
    ) {

        val device =
            intent.getParcelableExtra<BluetoothDevice>(
                BluetoothDevice.EXTRA_DEVICE
            ) ?: return

        scope.launch {

            repository.onEvent(
                BluetoothEvent.DeviceNameChanged(
                    address = device.address,
                    name = device.name
                )
            )
        }
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}