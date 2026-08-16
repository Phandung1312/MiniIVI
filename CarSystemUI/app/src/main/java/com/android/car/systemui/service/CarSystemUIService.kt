package com.android.car.systemui.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.android.car.systemui.CarSystemUIApplication

/** Process anchor that asks the application to ensure all SystemUI components are started. */
class CarSystemUIService : Service() {
    override fun onCreate() {
        super.onCreate()
        val systemUiApplication = application as? CarSystemUIApplication
            ?: error("CarSystemUIService requires CarSystemUIApplication")
        systemUiApplication.startComponentsIfNeeded()
        Log.i(TAG, "event=service_created feature=car_systemui")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "event=start_command action=${intent?.action ?: "none"} flags=$flags start_id=$startId",
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private companion object {
        const val TAG = "MiniIviSystemUi"
    }
}
