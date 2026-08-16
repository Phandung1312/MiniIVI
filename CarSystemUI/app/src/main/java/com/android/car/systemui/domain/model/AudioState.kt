package com.android.car.systemui.domain.model

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
