package com.android.car.systemui.data.mapper.carservice

import com.android.car.systemui.domain.model.BrightnessState
import com.miniivi.car.api.BrightnessState as ApiBrightnessState

internal fun ApiBrightnessState.toDomain(): BrightnessState = BrightnessState(
    progress = progress,
    available = available,
    automatic = automatic,
    errorMessage = diagnosticMessage,
)
