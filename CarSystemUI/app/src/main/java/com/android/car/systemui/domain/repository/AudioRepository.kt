package com.android.car.systemui.domain.repository

import com.android.car.systemui.domain.model.AudioState
import kotlinx.coroutines.flow.StateFlow

interface AudioRepository {
    val state: StateFlow<AudioState>
    fun refresh()
    fun setVolume(volume: Int)
}
