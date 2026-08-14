package com.android.car.launcher

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "event=application_created app=launcher")
    }

    private companion object {
        const val TAG = "MiniIviLauncher"
    }
}
