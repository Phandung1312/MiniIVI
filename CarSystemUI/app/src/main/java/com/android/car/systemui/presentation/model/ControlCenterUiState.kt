package com.android.car.systemui.presentation.model

import com.android.car.systemui.domain.model.AudioState
import com.android.car.systemui.domain.model.BrightnessState
import com.android.car.systemui.domain.model.ExtendedControlsState
import com.android.car.systemui.domain.model.HvacState

data class ControlCenterUiState(
    val brightness: BrightnessState = BrightnessState(),
    val audio: AudioState = AudioState(),
    val hvac: HvacState = HvacState(),
    val brightnessPreview: Float? = null,
    val extendedControls: ExtendedControlsState = ExtendedControlsState(),
    val moreClimateVisible: Boolean = false,
    val cameraVisible: Boolean = false,
    val screenCurtainVisible: Boolean = false,
) {
    val displayedBrightness: Float
        get() = brightnessPreview ?: brightness.progress
}
