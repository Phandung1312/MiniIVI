package com.android.car.launcher.feature.dashboard.domain.repository

import com.android.car.launcher.feature.dashboard.domain.model.HvacState
import com.android.car.launcher.feature.dashboard.domain.model.VehicleStatusState
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    val state: Flow<VehicleStatusState>

    fun start()
    fun refresh()
}

interface HvacRepository {
    val state: Flow<HvacState>

    fun start()
    fun refresh()
}
