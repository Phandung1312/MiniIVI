package com.android.car.systemui.domain.repository

import com.android.car.systemui.domain.model.BrightnessState
import kotlinx.coroutines.flow.StateFlow

interface BrightnessRepository {
    val state: StateFlow<BrightnessState>
    fun refresh()
    suspend fun setBrightness(progress: Float)
}
