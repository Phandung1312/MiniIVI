package com.android.car.systemui.data.repository

import android.content.Context
import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.model.ExtendedControlsState
import kotlinx.coroutines.flow.StateFlow

interface BrightnessRepository {
    val state: StateFlow<BrightnessState>
    fun start()
    fun stop()
    fun refresh()
    suspend fun setBrightness(progress: Float)
}

interface AudioRepository {
    val state: StateFlow<AudioState>
    fun start()
    fun stop()
    fun refresh()
    fun setVolume(volume: Int)
}

interface HvacRepository {
    val state: StateFlow<HvacState>
    fun start()
    fun stop()
    fun refresh()
    fun adjustTemperature(zone: ClimateZone, delta: Float)
    fun setAc(enabled: Boolean)
}

interface ExtendedControlsRepository {
    val state: StateFlow<ExtendedControlsState>
    fun start()
    fun refresh()
    fun setPower(enabled: Boolean)
    fun setAuto(enabled: Boolean)
    fun setSync(enabled: Boolean)
    fun setRecirculation(enabled: Boolean)
    fun setFanSpeed(zone: ClimateZone, speed: Int)
    fun setFanDirection(zone: ClimateZone, direction: Int)
    fun setDefroster(window: Int, enabled: Boolean)
    fun setSeatHeating(zone: ClimateZone, level: Int)
    fun setSeatVentilation(zone: ClimateZone, level: Int)
    fun setMaxAc(enabled: Boolean)
    fun setMaxDefrost(enabled: Boolean)
    fun setAutoRecirculation(enabled: Boolean)
    fun setSteeringWheelHeat(level: Int)
    fun setTemperatureUnit(unit: Int)
    fun setQuickControl(control: Int, enabled: Boolean)
    fun requestScreenOff()
}

interface NavigationRepository {
    fun goHome()
    fun openSettings()
    fun openAppList()
    fun openPhone()
    fun openWifiSettings()
    fun openWirelessSettings()
    fun openBluetoothSettings()
    fun openCamera(): Boolean
}

interface StartupRepository {
    fun initialize(context: Context)
}
