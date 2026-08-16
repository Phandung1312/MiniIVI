package com.android.car.systemui.data.mapper.carservice

import com.android.car.systemui.domain.model.ClimateFanDirection
import com.android.car.systemui.domain.model.ClimateWindow
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.QuickControl
import com.android.car.systemui.domain.model.TemperatureUnit
import com.miniivi.car.api.ClimateFanDirection as ApiClimateFanDirection
import com.miniivi.car.api.ClimateWindow as ApiClimateWindow
import com.miniivi.car.api.HvacZone
import com.miniivi.car.api.QuickControl as ApiQuickControl
import com.miniivi.car.api.TemperatureUnit as ApiTemperatureUnit

internal fun ClimateZone.toApi(): Int = when (this) {
    ClimateZone.LEFT -> HvacZone.LEFT
    ClimateZone.RIGHT -> HvacZone.RIGHT
}

internal fun ClimateFanDirection.toApi(): Int = when (this) {
    ClimateFanDirection.FACE -> ApiClimateFanDirection.FACE
    ClimateFanDirection.FEET -> ApiClimateFanDirection.FEET
    ClimateFanDirection.FACE_AND_FEET -> ApiClimateFanDirection.FACE_AND_FEET
    ClimateFanDirection.DEFROST -> ApiClimateFanDirection.DEFROST
}

internal fun ClimateWindow.toApi(): Int = when (this) {
    ClimateWindow.FRONT -> ApiClimateWindow.FRONT
    ClimateWindow.REAR -> ApiClimateWindow.REAR
}

internal fun TemperatureUnit.toApi(): Int = when (this) {
    TemperatureUnit.CELSIUS -> ApiTemperatureUnit.CELSIUS
    TemperatureUnit.FAHRENHEIT -> ApiTemperatureUnit.FAHRENHEIT
}

internal fun QuickControl.toApi(): Int = when (this) {
    QuickControl.WIFI -> ApiQuickControl.WIFI
    QuickControl.BLUETOOTH -> ApiQuickControl.BLUETOOTH
    QuickControl.HOTSPOT -> ApiQuickControl.HOTSPOT
    QuickControl.VALET_MODE -> ApiQuickControl.VALET_MODE
}
