package com.android.car.systemui.domain.repository

import com.android.car.systemui.domain.model.ClimateFanDirection
import com.android.car.systemui.domain.model.ClimateWindow
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.ExtendedControlsState
import com.android.car.systemui.domain.model.QuickControl
import com.android.car.systemui.domain.model.TemperatureUnit
import kotlinx.coroutines.flow.StateFlow

interface ExtendedControlsRepository {
    val state: StateFlow<ExtendedControlsState>
    fun refresh()
    fun setPower(enabled: Boolean)
    fun setAuto(enabled: Boolean)
    fun setSync(enabled: Boolean)
    fun setRecirculation(enabled: Boolean)
    fun setFanSpeed(zone: ClimateZone, speed: Int)
    fun setFanDirection(zone: ClimateZone, direction: ClimateFanDirection)
    fun setDefroster(window: ClimateWindow, enabled: Boolean)
    fun setSeatHeating(zone: ClimateZone, level: Int)
    fun setSeatVentilation(zone: ClimateZone, level: Int)
    fun setMaxAc(enabled: Boolean)
    fun setMaxDefrost(enabled: Boolean)
    fun setAutoRecirculation(enabled: Boolean)
    fun setSteeringWheelHeat(level: Int)
    fun setTemperatureUnit(unit: TemperatureUnit)
    fun setQuickControl(control: QuickControl, enabled: Boolean)
    fun requestScreenOff()
}
