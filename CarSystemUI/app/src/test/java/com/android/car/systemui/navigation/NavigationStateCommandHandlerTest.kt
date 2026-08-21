package com.android.car.systemui.navigation

import com.android.car.systemui.domain.repository.NavigationRepository
import com.android.car.systemui.presentation.controller.SystemUiStateController
import com.android.car.systemui.presentation.model.NavigationDestination
import com.miniivi.navigation.contract.NavigationContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateCommandHandlerTest {
    @Test
    fun validDestinationsAreDispatchedToSystemUiOnTheMainExecutor() {
        val controller = SystemUiStateController(FakeNavigationRepository())
        val pendingActions = mutableListOf<() -> Unit>()
        val handler = NavigationStateCommandHandler(controller) { action ->
            pendingActions += action
        }

        handler.reportDestination(NavigationContract.DESTINATION_HOME)
        handler.reportDestination(NavigationContract.DESTINATION_APP_LIST)
        assertEquals(NavigationDestination.HOME, controller.state.value.selectedDestination)
        assertEquals(2, pendingActions.size)

        pendingActions.removeFirst().invoke()
        assertEquals(NavigationDestination.HOME, controller.state.value.selectedDestination)
        pendingActions.removeFirst().invoke()
        assertEquals(NavigationDestination.APP_LIST, controller.state.value.selectedDestination)

        handler.reportDestination(NavigationContract.DESTINATION_NONE)
        pendingActions.single().invoke()
        assertEquals(NavigationDestination.NONE, controller.state.value.selectedDestination)
    }

    @Test
    fun invalidDestinationsAreIgnored() {
        val controller = SystemUiStateController(FakeNavigationRepository())
        val pendingActions = mutableListOf<() -> Unit>()
        val handler = NavigationStateCommandHandler(controller) { action ->
            pendingActions += action
        }

        handler.reportDestination(999)

        assertTrue(pendingActions.isEmpty())
        assertEquals(NavigationDestination.HOME, controller.state.value.selectedDestination)
    }
}

private class FakeNavigationRepository : NavigationRepository {
    override fun goHome() = Unit
    override fun openSettings() = Unit
    override fun openAppList() = Unit
    override fun openPhone() = Unit
    override fun openWifiSettings() = Unit
    override fun openWirelessSettings() = Unit
    override fun openBluetoothApp() = Unit
    override fun openCamera(): Boolean = false
}
