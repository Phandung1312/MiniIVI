package com.android.car.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import com.android.car.systemui.service.CarSystemUIService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        if (Process.myUid() / PER_USER_RANGE != 0) {
            Log.i(TAG, "event=boot_ignored reason=secondary_user action=${intent.action}")
            return
        }
        Log.i(TAG, "event=boot_received action=${intent.action}")
        context.startService(Intent(context, CarSystemUIService::class.java))
    }

    private companion object {
        const val PER_USER_RANGE = 100_000
        const val TAG = "MiniIviSystemUiBoot"
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
