package com.android.car.systemui.presentation.model

data class SystemUiState(
    val controlCenterVisible: Boolean = false,
    val selectedDestination: NavigationDestination = NavigationDestination.HOME,
)
