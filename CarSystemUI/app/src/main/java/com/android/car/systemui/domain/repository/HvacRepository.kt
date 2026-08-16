package com.android.car.systemui.domain.repository

import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.HvacState
import kotlinx.coroutines.flow.StateFlow

interface HvacRepository {
    val state: StateFlow<HvacState>
    fun refresh()
    fun adjustTemperature(zone: ClimateZone, delta: Float)
    fun setAc(enabled: Boolean)
}
