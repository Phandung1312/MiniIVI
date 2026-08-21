package com.android.car.launcher.feature.dashboard.data.repository

import com.android.car.launcher.feature.dashboard.domain.repository.LauncherNavigationDestination
import com.miniivi.navigation.contract.NavigationContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateReporterTest {
    @Test
    fun cachesOnlyLatestDestinationUntilServiceConnects() {
        val connection = FakeNavigationStateConnection()
        val reporter = CarSystemUiNavigationStateReporter(connection)
        val service = RecordingNavigationStateService()

        reporter.report(LauncherNavigationDestination.Home)
        reporter.report(LauncherNavigationDestination.AppList)
        assertTrue(service.destinations.isEmpty())

        connection.connect(service)

        assertEquals(listOf(NavigationContract.DESTINATION_APP_LIST), service.destinations)
    }

    @Test
    fun reportsImmediatelyWhenConnectedAndReplaysAfterReconnect() {
        val connection = FakeNavigationStateConnection()
        val reporter = CarSystemUiNavigationStateReporter(connection)
        val firstService = RecordingNavigationStateService()
        val secondService = RecordingNavigationStateService()

        reporter.report(LauncherNavigationDestination.Home)
        connection.connect(firstService)
        reporter.report(LauncherNavigationDestination.None)
        connection.disconnect()
        reporter.report(LauncherNavigationDestination.AppList)
        connection.connect(secondService)

        assertEquals(
            listOf(
                NavigationContract.DESTINATION_HOME,
                NavigationContract.DESTINATION_NONE,
            ),
            firstService.destinations,
        )
        assertEquals(listOf(NavigationContract.DESTINATION_APP_LIST), secondService.destinations)
    }

    @Test
    fun keepsLatestDestinationWhenRemoteCallFails() {
        val connection = FakeNavigationStateConnection()
        val reporter = CarSystemUiNavigationStateReporter(connection)
        val failingService = FailingNavigationStateService()
        val recoveredService = RecordingNavigationStateService()

        reporter.report(LauncherNavigationDestination.Home)
        connection.connect(failingService)
        reporter.report(LauncherNavigationDestination.Home)

        assertEquals(2, connection.startCount)
        connection.connect(recoveredService)

        assertEquals(listOf(NavigationContract.DESTINATION_HOME), recoveredService.destinations)
    }
}

private class FakeNavigationStateConnection : NavigationStateConnection {
    var startCount = 0
    private var listener: NavigationStateConnection.Listener? = null

    override fun start(listener: NavigationStateConnection.Listener) {
        startCount++
        this.listener = listener
    }

    override fun stop() {
        listener = null
    }

    fun connect(service: NavigationStateEndpoint) {
        checkNotNull(listener).onConnected(service)
    }

    fun disconnect() {
        listener?.onDisconnected()
    }
}

private open class RecordingNavigationStateService : NavigationStateEndpoint {
    val destinations = mutableListOf<Int>()

    override fun reportDestination(destination: Int): Boolean {
        destinations += destination
        return true
    }
}

private class FailingNavigationStateService : RecordingNavigationStateService() {
    override fun reportDestination(destination: Int): Boolean = false
}
