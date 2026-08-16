package com.android.car.systemui.domain.model

data class HvacState(
    val connecting: Boolean = true,
    val available: Boolean = false,
    val cabinTemperature: Float? = null,
    val leftZone: TemperatureZone? = null,
    val rightZone: TemperatureZone? = null,
    val acAvailable: Boolean = false,
    val acOn: Boolean = false,
    val errorMessage: String? = null,
) {
    val dualZone: Boolean
        get() = leftZone != null && rightZone != null && leftZone.zone != rightZone.zone
}
