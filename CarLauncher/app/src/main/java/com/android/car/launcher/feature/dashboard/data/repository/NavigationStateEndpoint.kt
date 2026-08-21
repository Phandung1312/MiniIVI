package com.android.car.launcher.feature.dashboard.data.repository

interface NavigationStateEndpoint {
    fun reportDestination(destination: Int): Boolean
}
