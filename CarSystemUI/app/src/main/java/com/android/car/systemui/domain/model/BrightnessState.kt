package com.android.car.systemui.domain.model

data class BrightnessState(
    val progress: Float = 0.5f,
    val available: Boolean = false,
    val automatic: Boolean = false,
    val errorMessage: String? = null,
)
