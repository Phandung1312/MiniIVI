package com.android.car.systemui.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import com.android.car.systemui.framework.FrameworkPlatformApi

class LauncherReadyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BootHandoffContract.ACTION_LAUNCHER_FIRST_FRAME_DRAWN) return

        if (BootUserPolicy.shouldRelayLauncherReady(Process.myUid())) {
            val relay = Intent(BootHandoffContract.ACTION_LAUNCHER_FIRST_FRAME_DRAWN)
                .setClass(context, LauncherReadyReceiver::class.java)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            FrameworkPlatformApi.sendBroadcastAsUser(context, relay, FrameworkPlatformApi.systemUser())
            return
        }

        context.startService(BootHandoffService.dismissIntent(context))
    }
}
