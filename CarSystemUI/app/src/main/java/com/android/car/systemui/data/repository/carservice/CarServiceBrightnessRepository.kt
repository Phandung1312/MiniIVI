package com.android.car.systemui.data.repository.carservice

import com.android.car.systemui.data.mapper.carservice.toDomain
import com.android.car.systemui.domain.model.BrightnessState
import com.android.car.systemui.domain.repository.BrightnessRepository
import com.android.car.systemui.di.ApplicationScope
import com.miniivi.car.client.MiniIviCarClient
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CarServiceBrightnessRepository @Inject constructor(
    private val client: MiniIviCarClient,
    @ApplicationScope
    scope: CoroutineScope,
) : BrightnessRepository {
    override val state: StateFlow<BrightnessState> = client.brightnessState
        .map { it.toDomain() }
        .stateIn(scope, SharingStarted.Eagerly, BrightnessState())

    override fun refresh() { client.refreshBrightness() }

    override suspend fun setBrightness(progress: Float) {
        client.setBrightness(progress)
    }
}
