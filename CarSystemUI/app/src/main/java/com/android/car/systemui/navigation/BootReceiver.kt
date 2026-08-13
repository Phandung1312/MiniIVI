package com.android.car.systemui.navigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import com.android.car.systemui.di.SystemUiDependencies

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        if (Process.myUid() / PER_USER_RANGE != 0) return
        context.startService(Intent(context, BottomNavigationService::class.java))
        SystemUiDependencies.from(context).startupRepository.initialize(context)
    }

    private companion object {
        const val PER_USER_RANGE = 100_000
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
