package com.android.car.systemui.domain.policy

import kotlin.math.roundToInt

internal object AudioVolumePolicy {
    fun toVolume(progress: Float, minimum: Int, maximum: Int): Int =
        (minimum + progress.coerceIn(0f, 1f) * (maximum - minimum))
            .roundToInt()
            .coerceIn(minimum, maximum)
}
