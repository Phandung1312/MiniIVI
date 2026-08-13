package com.android.car.launcher.feature.dashboard.repository

import com.miniivi.car.api.HvacState
import com.miniivi.car.api.VehicleStatusState
import com.miniivi.car.client.MiniIviCarClient
import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository {
    val state: StateFlow<VehicleStatusState>
    fun start()
    fun refresh()
}

interface HvacRepository {
    val state: StateFlow<HvacState>
    fun start()
    fun refresh()
}

class CarServiceVehicleRepository(
    private val client: MiniIviCarClient,
) : VehicleRepository {
    override val state: StateFlow<VehicleStatusState> = client.vehicleStatusState
    override fun start() = client.start()
    override fun refresh() { client.refreshVehicleStatus() }
}

class CarServiceHvacRepository(
    private val client: MiniIviCarClient,
) : HvacRepository {
    override val state: StateFlow<HvacState> = client.hvacState
    override fun start() = client.start()
    override fun refresh() { client.refreshHvac() }
}
