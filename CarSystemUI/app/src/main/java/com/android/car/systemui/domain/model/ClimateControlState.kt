package com.android.car.systemui.domain.model

data class ClimateControlState(
    val connecting: Boolean = true,
    val available: Boolean = false,
    val powerOn: Boolean = true,
    val acOn: Boolean = true,
    val autoOn: Boolean = true,
    val syncOn: Boolean = false,
    val recirculationOn: Boolean = false,
    val hasCabinTemperature: Boolean = false,
    val cabinTemperatureCelsius: Float = 25f,
    val driverZone: ClimateZoneControlState = ClimateZoneControlState(ClimateZone.LEFT),
    val passengerZone: ClimateZoneControlState = ClimateZoneControlState(
        zone = ClimateZone.RIGHT,
        temperatureCelsius = 22.5f,
    ),
    val frontDefrostOn: Boolean = false,
    val rearDefrostOn: Boolean = false,
    val maxAcOn: Boolean = false,
    val maxDefrostOn: Boolean = false,
    val autoRecirculationOn: Boolean = false,
    val steeringWheelHeatLevel: Int = 0,
    val maximumSteeringWheelHeatLevel: Int = 3,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val capabilities: Set<ClimateCapability> = emptySet(),
    val errorMessage: String? = null,
)
