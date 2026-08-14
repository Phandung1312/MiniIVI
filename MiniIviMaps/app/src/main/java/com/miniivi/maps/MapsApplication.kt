package com.miniivi.maps

import android.app.Application
import android.util.Log

class MapsApplication : Application() {
    internal val locationTracker: MapLocationTracker by lazy { MapLocationTracker(this) }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "event=application_created app=maps")
    }

    private companion object {
        const val TAG = "MiniIviMaps"
    }
}
