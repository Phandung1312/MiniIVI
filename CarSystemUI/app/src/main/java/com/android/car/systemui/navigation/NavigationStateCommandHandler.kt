package com.android.car.systemui.navigation

import com.android.car.systemui.presentation.controller.SystemUiStateController
import com.android.car.systemui.presentation.model.NavigationDestination

internal class NavigationStateCommandHandler(
    private val systemUiStateController: SystemUiStateController,
    private val postToMain: (() -> Unit) -> Unit,
) {
    fun reportDestination(value: Int) {
        val destination = NavigationStateDestinationMapper.fromContractValue(value)
        if (destination == null) {
            return
        }
        postToMain {
            when (destination) {
                NavigationDestination.NONE -> systemUiStateController.onExternalAppOpened()
                NavigationDestination.HOME,
                NavigationDestination.APP_LIST,
                -> systemUiStateController.onLauncherDestinationChanged(destination)
                else -> Unit
            }
        }
    }
}
