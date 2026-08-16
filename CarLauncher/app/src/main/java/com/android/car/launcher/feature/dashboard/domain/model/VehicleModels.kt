package com.android.car.launcher.feature.dashboard.domain.model

enum class FeatureStatus {
    CONNECTING,
    READY,
    UNAVAILABLE,
    ERROR,
}

data class HvacState(
    val status: FeatureStatus = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val hasCabinTemperature: Boolean = false,
    val cabinTemperatureCelsius: Float = 0f,
    val acAvailable: Boolean = false,
    val acOn: Boolean = false,
)

data class VehicleStatusState(
    val status: FeatureStatus = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val hasBatteryPercentage: Boolean = false,
    val batteryPercentage: Float = 0f,
    val hasOutsideTemperature: Boolean = false,
    val outsideTemperatureCelsius: Float = 0f,
    val hasRange: Boolean = false,
    val rangeKilometers: Float = 0f,
    val hasTirePressure: Boolean = false,
    val minimumTirePressureKpa: Float = 0f,
    val tiresHealthy: Boolean = true,
)
