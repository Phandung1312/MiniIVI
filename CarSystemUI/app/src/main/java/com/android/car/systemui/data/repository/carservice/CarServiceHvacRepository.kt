package com.android.car.systemui.data.repository.carservice

import com.android.car.systemui.data.mapper.carservice.toApi
import com.android.car.systemui.data.mapper.carservice.toDomain
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.HvacState
import com.android.car.systemui.domain.policy.HvacTemperaturePolicy
import com.android.car.systemui.domain.repository.HvacRepository
import com.android.car.systemui.di.ApplicationScope
import com.miniivi.car.client.MiniIviCarClient
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CarServiceHvacRepository @Inject constructor(
    private val client: MiniIviCarClient,
    @ApplicationScope
    scope: CoroutineScope,
) : HvacRepository {
    override val state: StateFlow<HvacState> = client.hvacState
        .map { it.toDomain() }
        .stateIn(scope, SharingStarted.Eagerly, HvacState())

    override fun refresh() { client.refreshHvac() }

    override fun adjustTemperature(zone: ClimateZone, delta: Float) {
        val current = state.value
        val target = when (zone) {
            ClimateZone.LEFT -> current.leftZone
            ClimateZone.RIGHT -> current.rightZone
        } ?: return
        if (target.temperature == null) return
        client.setHvacTemperature(zone.toApi(), HvacTemperaturePolicy.adjust(target, delta))
    }

    override fun setAc(enabled: Boolean) {
        client.setAcEnabled(enabled)
    }
}
