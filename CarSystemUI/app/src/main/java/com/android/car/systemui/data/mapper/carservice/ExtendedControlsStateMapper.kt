package com.android.car.systemui.data.mapper.carservice

import com.android.car.systemui.domain.model.ClimateCapability
import com.android.car.systemui.domain.model.ClimateControlState
import com.android.car.systemui.domain.model.ClimateFanDirection
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.ClimateZoneControlState
import com.android.car.systemui.domain.model.ExtendedControlsState
import com.android.car.systemui.domain.model.QuickControl
import com.android.car.systemui.domain.model.QuickControlsState
import com.android.car.systemui.domain.model.TemperatureUnit
import com.miniivi.car.api.ClimateCapability as ApiClimateCapability
import com.miniivi.car.api.ClimateControlState as ApiClimateControlState
import com.miniivi.car.api.ClimateFanDirection as ApiClimateFanDirection
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacZone
import com.miniivi.car.api.QuickControl as ApiQuickControl
import com.miniivi.car.api.QuickControlsState as ApiQuickControlsState
import com.miniivi.car.api.TemperatureUnit as ApiTemperatureUnit

internal fun ApiClimateControlState.toDomain(): ClimateControlState = ClimateControlState(
    connecting = status == FeatureStatus.CONNECTING,
    available = available,
    powerOn = powerOn,
    acOn = acOn,
    autoOn = autoOn,
    syncOn = syncOn,
    recirculationOn = recirculationOn,
    hasCabinTemperature = hasCabinTemperature,
    cabinTemperatureCelsius = cabinTemperatureCelsius,
    driverZone = driverZone.toDomain(),
    passengerZone = passengerZone.toDomain(),
    frontDefrostOn = frontDefrostOn,
    rearDefrostOn = rearDefrostOn,
    maxAcOn = maxAcOn,
    maxDefrostOn = maxDefrostOn,
    autoRecirculationOn = autoRecirculationOn,
    steeringWheelHeatLevel = steeringWheelHeatLevel,
    maximumSteeringWheelHeatLevel = maximumSteeringWheelHeatLevel,
    temperatureUnit = temperatureUnit.toDomainTemperatureUnit(),
    capabilities = realCapabilities.toClimateCapabilities(),
    errorMessage = diagnosticMessage,
)

internal fun ApiQuickControlsState.toDomain(): QuickControlsState = QuickControlsState(
    connecting = status == FeatureStatus.CONNECTING,
    available = available,
    wifiEnabled = wifiEnabled,
    wifiConnected = wifiConnected,
    bluetoothEnabled = bluetoothEnabled,
    bluetoothConnected = bluetoothConnected,
    hotspotEnabled = hotspotEnabled,
    valetModeEnabled = valetModeEnabled,
    capabilities = realCapabilities.toQuickControls(),
    screenOffAvailable = realCapabilities and ApiQuickControl.SCREEN_OFF_CAPABILITY != 0L,
    errorMessage = diagnosticMessage,
)

internal fun toDomain(
    climate: ApiClimateControlState,
    quickControls: ApiQuickControlsState,
): ExtendedControlsState = ExtendedControlsState(
    climate = climate.toDomain(),
    quickControls = quickControls.toDomain(),
)

private fun com.miniivi.car.api.ClimateZoneControlState.toDomain(): ClimateZoneControlState =
    ClimateZoneControlState(
        zone = zone.toDomainClimateZone(),
        temperatureCelsius = temperatureCelsius,
        minimumTemperatureCelsius = minimumCelsius,
        maximumTemperatureCelsius = maximumCelsius,
        fanSpeed = fanSpeed,
        minimumFanSpeed = minimumFanSpeed,
        maximumFanSpeed = maximumFanSpeed,
        fanDirection = fanDirection.toDomainFanDirection(),
        availableFanDirections = availableFanDirections.map { it.toDomainFanDirection() },
        seatHeatingLevel = seatHeatingLevel,
        maximumSeatHeatingLevel = maximumSeatHeatingLevel,
        seatVentilationLevel = seatVentilationLevel,
        maximumSeatVentilationLevel = maximumSeatVentilationLevel,
    )

private fun Int.toDomainClimateZone(): ClimateZone = when (this) {
    HvacZone.LEFT -> ClimateZone.LEFT
    HvacZone.RIGHT -> ClimateZone.RIGHT
    else -> ClimateZone.RIGHT
}

private fun Int.toDomainFanDirection(): ClimateFanDirection = when (this) {
    ApiClimateFanDirection.FACE -> ClimateFanDirection.FACE
    ApiClimateFanDirection.FEET -> ClimateFanDirection.FEET
    ApiClimateFanDirection.FACE_AND_FEET -> ClimateFanDirection.FACE_AND_FEET
    ApiClimateFanDirection.DEFROST -> ClimateFanDirection.DEFROST
    else -> ClimateFanDirection.FACE
}

private fun Int.toDomainTemperatureUnit(): TemperatureUnit = when (this) {
    ApiTemperatureUnit.CELSIUS -> TemperatureUnit.CELSIUS
    ApiTemperatureUnit.FAHRENHEIT -> TemperatureUnit.FAHRENHEIT
    else -> TemperatureUnit.FAHRENHEIT
}

private fun Long.toClimateCapabilities(): Set<ClimateCapability> {
    val capabilities = mutableSetOf<ClimateCapability>()
    fun addIfSet(mask: Long, value: ClimateCapability) {
        if (this@toClimateCapabilities and mask != 0L) capabilities += value
    }
    addIfSet(ApiClimateCapability.POWER, ClimateCapability.POWER)
    addIfSet(ApiClimateCapability.AC, ClimateCapability.AC)
    addIfSet(ApiClimateCapability.AUTO, ClimateCapability.AUTO)
    addIfSet(ApiClimateCapability.SYNC, ClimateCapability.SYNC)
    addIfSet(ApiClimateCapability.RECIRCULATION, ClimateCapability.RECIRCULATION)
    addIfSet(ApiClimateCapability.FAN_SPEED, ClimateCapability.FAN_SPEED)
    addIfSet(ApiClimateCapability.FAN_DIRECTION, ClimateCapability.FAN_DIRECTION)
    addIfSet(ApiClimateCapability.FRONT_DEFROST, ClimateCapability.FRONT_DEFROST)
    addIfSet(ApiClimateCapability.REAR_DEFROST, ClimateCapability.REAR_DEFROST)
    addIfSet(ApiClimateCapability.SEAT_HEATING, ClimateCapability.SEAT_HEATING)
    addIfSet(ApiClimateCapability.SEAT_VENTILATION, ClimateCapability.SEAT_VENTILATION)
    addIfSet(ApiClimateCapability.MAX_AC, ClimateCapability.MAX_AC)
    addIfSet(ApiClimateCapability.MAX_DEFROST, ClimateCapability.MAX_DEFROST)
    addIfSet(ApiClimateCapability.AUTO_RECIRCULATION, ClimateCapability.AUTO_RECIRCULATION)
    addIfSet(ApiClimateCapability.STEERING_WHEEL_HEAT, ClimateCapability.STEERING_WHEEL_HEAT)
    addIfSet(ApiClimateCapability.TEMPERATURE_UNIT, ClimateCapability.TEMPERATURE_UNIT)
    return capabilities
}

private fun Long.toQuickControls(): Set<QuickControl> {
    val controls = mutableSetOf<QuickControl>()
    fun addIfSet(mask: Long, value: QuickControl) {
        if (this@toQuickControls and mask != 0L) controls += value
    }
    addIfSet(ApiQuickControl.WIFI_CAPABILITY, QuickControl.WIFI)
    addIfSet(ApiQuickControl.BLUETOOTH_CAPABILITY, QuickControl.BLUETOOTH)
    addIfSet(ApiQuickControl.HOTSPOT_CAPABILITY, QuickControl.HOTSPOT)
    return controls
}
