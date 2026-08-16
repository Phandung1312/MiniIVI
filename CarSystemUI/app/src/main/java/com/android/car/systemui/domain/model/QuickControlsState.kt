package com.android.car.systemui.domain.model

data class QuickControlsState(
    val connecting: Boolean = true,
    val available: Boolean = false,
    val wifiEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val bluetoothConnected: Boolean = false,
    val hotspotEnabled: Boolean = false,
    val valetModeEnabled: Boolean = false,
    val capabilities: Set<QuickControl> = emptySet(),
    val screenOffAvailable: Boolean = false,
    val errorMessage: String? = null,
)
