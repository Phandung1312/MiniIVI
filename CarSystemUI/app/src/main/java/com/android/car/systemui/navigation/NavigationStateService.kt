package com.android.car.systemui.navigation

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.android.car.systemui.presentation.controller.SystemUiStateController
import com.miniivi.navigation.contract.INavigationStateService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NavigationStateService : Service() {
    @Inject
    lateinit var systemUiStateController: SystemUiStateController

    private val mainHandler = Handler(Looper.getMainLooper())
    private val commandHandler by lazy {
        NavigationStateCommandHandler(
            systemUiStateController = systemUiStateController,
            postToMain = { action -> mainHandler.post(action) },
        )
    }
    private val binder = object : INavigationStateService.Stub() {
        override fun reportDestination(destination: Int) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "event=destination_reported destination=$destination")
            }
            commandHandler.reportDestination(destination)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "event=service_created feature=navigation_state")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "event=service_bound feature=navigation_state")
        return binder
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        Log.i(TAG, "event=service_destroyed feature=navigation_state")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MiniIviNavigation"
    }
}
