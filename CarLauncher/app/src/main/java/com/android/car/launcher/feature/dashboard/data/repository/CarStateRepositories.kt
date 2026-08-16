package com.android.car.launcher.feature.dashboard.data.repository

import com.android.car.launcher.feature.dashboard.data.mapper.toDomain
import com.android.car.launcher.feature.dashboard.domain.model.HvacState
import com.android.car.launcher.feature.dashboard.domain.model.VehicleStatusState
import com.android.car.launcher.feature.dashboard.domain.repository.HvacRepository
import com.android.car.launcher.feature.dashboard.domain.repository.VehicleRepository
import com.miniivi.car.client.MiniIviCarClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CarServiceVehicleRepository @Inject constructor(
    private val client: MiniIviCarClient,
) : VehicleRepository {
    override val state: Flow<VehicleStatusState> = client.vehicleStatusState.map { it.toDomain() }

    override fun start() = client.start()

    override fun refresh() {
        client.refreshVehicleStatus()
    }
}

@Singleton
class CarServiceHvacRepository @Inject constructor(
    private val client: MiniIviCarClient,
) : HvacRepository {
    override val state: Flow<HvacState> = client.hvacState.map { it.toDomain() }

    override fun start() = client.start()

    override fun refresh() {
        client.refreshHvac()
    }
}
