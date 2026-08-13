package com.miniivi.maps

import android.app.Application

class MapsApplication : Application() {
    internal val locationTracker: MapLocationTracker by lazy { MapLocationTracker(this) }
}
