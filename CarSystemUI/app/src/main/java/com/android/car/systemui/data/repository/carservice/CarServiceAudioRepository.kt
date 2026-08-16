package com.android.car.systemui.data.repository.carservice

import com.android.car.systemui.data.mapper.carservice.toDomain
import com.android.car.systemui.domain.model.AudioState
import com.android.car.systemui.domain.repository.AudioRepository
import com.android.car.systemui.di.ApplicationScope
import com.miniivi.car.client.MiniIviCarClient
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CarServiceAudioRepository @Inject constructor(
    private val client: MiniIviCarClient,
    @ApplicationScope
    scope: CoroutineScope,
) : AudioRepository {
    override val state: StateFlow<AudioState> = client.audioState
        .map { it.toDomain() }
        .stateIn(scope, SharingStarted.Eagerly, AudioState())

    override fun refresh() { client.refreshAudio() }

    override fun setVolume(volume: Int) {
        client.setMediaVolume(volume)
    }
}
