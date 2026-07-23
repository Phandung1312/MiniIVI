package com.android.car.launcher.feature.bluetooth.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.car.launcher.feature.bluetooth.service.BluetoothMonitorService

class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val serviceIntent =
            Intent(
                context,
                BluetoothMonitorService::class.java
            )

        serviceIntent.action = intent.action

        val device =
            intent.getParcelableExtra<BluetoothDevice>(
                BluetoothDevice.EXTRA_DEVICE
            )

        serviceIntent.putExtra(
            BluetoothDevice.EXTRA_DEVICE,
            device
        )

        serviceIntent.putExtras(intent)

        context.startService(serviceIntent)
    }
}