package com.android.car.systemui.data.mapper.carservice

import com.android.car.systemui.domain.model.AudioState
import com.miniivi.car.api.AudioState as ApiAudioState

internal fun ApiAudioState.toDomain(): AudioState = AudioState(
    volume = volume,
    minimum = minimum,
    maximum = maximum,
    available = available,
    errorMessage = diagnosticMessage,
)
