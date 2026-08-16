package com.android.car.systemui.domain.model

enum class ClimateFanDirection {
    FACE,
    FEET,
    FACE_AND_FEET,
    DEFROST;

    companion object {
        val STANDARD_OPTIONS: List<ClimateFanDirection> = listOf(
            FACE,
            FEET,
            FACE_AND_FEET,
        )
    }
}
