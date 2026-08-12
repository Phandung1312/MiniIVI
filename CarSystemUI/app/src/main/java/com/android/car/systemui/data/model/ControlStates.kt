package com.android.car.systemui.data.model

import com.miniivi.car.api.ClimateControlState
import com.miniivi.car.api.QuickControlsState

data class BrightnessState(
    val progress: Float = 0.5f,
    val available: Boolean = false,
    val automatic: Boolean = false,
    val errorMessage: String? = null,
)

data class AudioState(
    val volume: Int = 0,
    val minimum: Int = 0,
    val maximum: Int = 1,
    val available: Boolean = false,
    val errorMessage: String? = null,
) {
    val progress: Float
        get() = if (maximum <= minimum) 0f
        else (volume - minimum).toFloat() / (maximum - minimum)
}

data class TemperatureZone(
    val areaId: Int,
    val temperature: Float?,
    val minimum: Float,
    val maximum: Float,
)

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
        get() = leftZone != null && rightZone != null && leftZone.areaId != rightZone.areaId
}

enum class ClimateZone { LEFT, RIGHT }

data class ExtendedControlsState(
    val climate: ClimateControlState = ClimateControlState(),
    val quickControls: QuickControlsState = QuickControlsState(),
)
