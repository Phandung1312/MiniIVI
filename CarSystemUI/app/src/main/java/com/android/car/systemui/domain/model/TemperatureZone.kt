package com.android.car.systemui.domain.model

data class TemperatureZone(
    val zone: ClimateZone,
    val temperature: Float?,
    val minimum: Float,
    val maximum: Float,
)
