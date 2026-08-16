package com.android.car.systemui.domain.model

data class ClimateZoneControlState(
    val zone: ClimateZone,
    val temperatureCelsius: Float = 22f,
    val minimumTemperatureCelsius: Float = 16f,
    val maximumTemperatureCelsius: Float = 30f,
    val fanSpeed: Int = 4,
    val minimumFanSpeed: Int = 0,
    val maximumFanSpeed: Int = 7,
    val fanDirection: ClimateFanDirection = ClimateFanDirection.FACE,
    val availableFanDirections: List<ClimateFanDirection> = listOf(
        ClimateFanDirection.FACE,
        ClimateFanDirection.FEET,
        ClimateFanDirection.FACE_AND_FEET,
    ),
    val seatHeatingLevel: Int = 0,
    val maximumSeatHeatingLevel: Int = 3,
    val seatVentilationLevel: Int = 0,
    val maximumSeatVentilationLevel: Int = 3,
)
