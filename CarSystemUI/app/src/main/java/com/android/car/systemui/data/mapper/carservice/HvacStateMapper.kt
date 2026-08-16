package com.android.car.systemui.data.mapper.carservice

import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.HvacState
import com.android.car.systemui.domain.model.TemperatureZone
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacZone
import com.miniivi.car.api.HvacZoneState as ApiHvacZoneState
import com.miniivi.car.api.HvacState as ApiHvacState

internal fun ApiHvacState.toDomain(): HvacState = HvacState(
    connecting = status == FeatureStatus.CONNECTING,
    available = available,
    cabinTemperature = cabinTemperatureCelsius.takeIf { hasCabinTemperature },
    leftZone = leftZone?.toDomain(),
    rightZone = rightZone?.toDomain(),
    acAvailable = acAvailable,
    acOn = acOn,
    errorMessage = diagnosticMessage,
)

private fun ApiHvacZoneState.toDomain(): TemperatureZone = TemperatureZone(
    zone = zone.toDomainClimateZone(),
    temperature = temperatureCelsius.takeIf { hasTemperature },
    minimum = minimumCelsius,
    maximum = maximumCelsius,
)

private fun Int.toDomainClimateZone(): ClimateZone = when (this) {
    HvacZone.LEFT -> ClimateZone.LEFT
    HvacZone.RIGHT -> ClimateZone.RIGHT
    else -> ClimateZone.RIGHT
}
