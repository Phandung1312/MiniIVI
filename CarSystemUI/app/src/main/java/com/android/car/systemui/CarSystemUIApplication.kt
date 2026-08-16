package com.android.car.systemui

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import com.android.car.systemui.core.CarSystemUIInitializer
import com.android.car.systemui.core.SystemUiLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CarSystemUIApplication : Application() {
    @Inject
    lateinit var initializer: CarSystemUIInitializer

    @Inject
    lateinit var lifecycleOwner: SystemUiLifecycleOwner

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.start()
        initializer.startComponentsIfNeeded()
        Log.i(TAG, "event=application_created app=car_systemui")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        initializer.onConfigurationChanged(newConfig)
    }

    fun startComponentsIfNeeded() = initializer.startComponentsIfNeeded()

    private companion object {
        const val TAG = "MiniIviSystemUi"
    }
}
