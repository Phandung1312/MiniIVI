package com.android.car.systemui.presentation.controller

import com.android.car.systemui.domain.repository.NavigationRepository
import com.android.car.systemui.presentation.model.NavigationDestination
import com.android.car.systemui.presentation.model.SystemUiState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SystemUiStateController @Inject constructor(
    private val navigationRepository: NavigationRepository,
) {
    private val mutableState = MutableStateFlow(SystemUiState())
    private var destinationBeforeControlCenter = NavigationDestination.HOME
    val state = mutableState.asStateFlow()

    fun toggleControlCenter() {
        val current = mutableState.value
        if (current.controlCenterVisible) {
            mutableState.value = current.copy(
                controlCenterVisible = false,
                selectedDestination = destinationBeforeControlCenter,
            )
        } else {
            destinationBeforeControlCenter = current.selectedDestination
            mutableState.value = current.copy(
                controlCenterVisible = true,
                selectedDestination = NavigationDestination.CONTROL_CENTER,
            )
        }
    }

    fun dismissControlCenter() {
        if (!mutableState.value.controlCenterVisible) return
        mutableState.value = mutableState.value.copy(
            controlCenterVisible = false,
            selectedDestination = destinationBeforeControlCenter,
        )
    }

    fun goHome() {
        selectDestination(NavigationDestination.HOME)
        navigationRepository.goHome()
    }

    fun openSettings() {
        selectDestination(NavigationDestination.SETTINGS)
        navigationRepository.openSettings()
    }

    fun openAppList() {
        selectDestination(NavigationDestination.APP_LIST)
        navigationRepository.openAppList()
    }

    fun openPhone() {
        selectDestination(NavigationDestination.PHONE)
        navigationRepository.openPhone()
    }

    fun onExternalAppOpened() = selectDestination(NavigationDestination.NONE)

    fun onLauncherDestinationChanged(destination: NavigationDestination) {
        require(
            destination == NavigationDestination.HOME ||
                destination == NavigationDestination.APP_LIST ||
                destination == NavigationDestination.NONE,
        ) {
            "Unsupported launcher destination: $destination"
        }
        selectDestination(destination)
    }

    private fun selectDestination(destination: NavigationDestination) {
        if (destination != NavigationDestination.CONTROL_CENTER) {
            destinationBeforeControlCenter = destination
        }
        mutableState.value = mutableState.value.copy(
            controlCenterVisible = false,
            selectedDestination = destination,
        )
    }
}
