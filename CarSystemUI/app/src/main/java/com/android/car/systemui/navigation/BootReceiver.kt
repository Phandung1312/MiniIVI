package com.android.car.systemui.navigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import com.android.car.systemui.boot.BootHandoffService
import com.android.car.systemui.boot.BootUserPolicy
import com.android.car.systemui.di.SystemUiDependencies

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        if (!BootUserPolicy.isSystemUser(Process.myUid())) return
        if (BootUserPolicy.shouldShowHandoff(intent.action, Process.myUid())) {
            context.startService(BootHandoffService.showIntent(context))
        }
        context.startService(Intent(context, BottomNavigationService::class.java))
        SystemUiDependencies.from(context).startupRepository.initialize(context)
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
