package com.android.car.systemui.domain.repository

interface NavigationRepository {
    fun goHome()
    fun openSettings()
    fun openAppList()
    fun openPhone()
    fun openWifiSettings()
    fun openWirelessSettings()
    fun openBluetoothApp()
    fun openCamera(): Boolean
}
