package com.miniivi.car.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process

class CarServiceBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        if (Process.myUid() / PER_USER_RANGE != 0) return
        context.startService(Intent(context, MiniIviCarService::class.java))
    }

    private companion object {
        const val PER_USER_RANGE = 100_000
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
