package com.android.car.launcher.feature.dashboard.data.mapper

import com.android.car.launcher.feature.dashboard.domain.model.FeatureStatus
import com.android.car.launcher.feature.dashboard.domain.model.HvacState
import com.android.car.launcher.feature.dashboard.domain.model.VehicleStatusState
import com.miniivi.car.api.HvacState as CarHvacState
import com.miniivi.car.api.VehicleStatusState as CarVehicleStatusState
import com.miniivi.car.api.FeatureStatus as CarFeatureStatus

fun CarHvacState.toDomain() = HvacState(
    status = status.toDomainStatus(),
    available = available,
    hasCabinTemperature = hasCabinTemperature,
    cabinTemperatureCelsius = cabinTemperatureCelsius,
    acAvailable = acAvailable,
    acOn = acOn,
)

fun CarVehicleStatusState.toDomain() = VehicleStatusState(
    status = status.toDomainStatus(),
    available = available,
    hasBatteryPercentage = hasBatteryPercentage,
    batteryPercentage = batteryPercentage,
    hasOutsideTemperature = hasOutsideTemperature,
    outsideTemperatureCelsius = outsideTemperatureCelsius,
    hasRange = hasRange,
    rangeKilometers = rangeKilometers,
    hasTirePressure = hasTirePressure,
    minimumTirePressureKpa = minimumTirePressureKpa,
    tiresHealthy = tiresHealthy,
)

private fun Int.toDomainStatus() = when (this) {
    CarFeatureStatus.CONNECTING -> FeatureStatus.CONNECTING
    CarFeatureStatus.READY -> FeatureStatus.READY
    CarFeatureStatus.UNAVAILABLE -> FeatureStatus.UNAVAILABLE
    else -> FeatureStatus.ERROR
}
