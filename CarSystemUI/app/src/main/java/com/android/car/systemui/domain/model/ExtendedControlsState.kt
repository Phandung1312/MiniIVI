package com.android.car.systemui.domain.model

data class ExtendedControlsState(
    val climate: ClimateControlState = ClimateControlState(),
    val quickControls: QuickControlsState = QuickControlsState(),
)
