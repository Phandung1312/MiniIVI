package com.android.car.systemui.navigation

import com.android.car.systemui.presentation.model.NavigationDestination
import com.miniivi.navigation.contract.NavigationContract

internal object NavigationStateDestinationMapper {
    fun fromContractValue(value: Int): NavigationDestination? = when (value) {
        NavigationContract.DESTINATION_HOME -> NavigationDestination.HOME
        NavigationContract.DESTINATION_APP_LIST -> NavigationDestination.APP_LIST
        NavigationContract.DESTINATION_NONE -> NavigationDestination.NONE
        else -> null
    }
}
