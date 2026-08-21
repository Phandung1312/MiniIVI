package com.android.car.launcher.feature.dashboard.data.repository

interface NavigationStateConnection {
    interface Listener {
        fun onConnected(service: NavigationStateEndpoint)
        fun onDisconnected()
    }

    fun start(listener: Listener)
    fun stop()
}
