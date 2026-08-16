package com.android.car.systemui.navigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.car.systemui.presentation.controller.SystemUiStateController
import com.android.car.systemui.presentation.model.NavigationDestination
import com.miniivi.car.api.NavigationStateContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LauncherDestinationReceiver @Inject constructor(
    private val systemUiStateController: SystemUiStateController,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NavigationStateContract.ACTION_DESTINATION_CHANGED) return
        val destination = intent.getStringExtra(NavigationStateContract.EXTRA_DESTINATION)
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "event=launcher_destination_received destination=${destination ?: "unknown"}")
        }
        when (destination) {
            NavigationStateContract.DESTINATION_HOME ->
                systemUiStateController.onLauncherDestinationChanged(NavigationDestination.HOME)
            NavigationStateContract.DESTINATION_APP_LIST ->
                systemUiStateController.onLauncherDestinationChanged(NavigationDestination.APP_LIST)
            NavigationStateContract.DESTINATION_NONE ->
                systemUiStateController.onExternalAppOpened()
        }
    }

    private companion object {
        const val TAG = "MiniIviSystemUi"
    }
}
