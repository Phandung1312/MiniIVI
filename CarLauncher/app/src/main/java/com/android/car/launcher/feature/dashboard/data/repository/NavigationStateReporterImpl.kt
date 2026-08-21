package com.android.car.launcher.feature.dashboard.data.repository

import com.android.car.launcher.feature.dashboard.domain.repository.LauncherNavigationDestination
import com.android.car.launcher.feature.dashboard.domain.repository.NavigationStateReporter
import com.miniivi.navigation.contract.NavigationContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarSystemUiNavigationStateReporter @Inject constructor(
    private val connection: NavigationStateConnection,
) : NavigationStateReporter {
    private var started = false
    private var service: NavigationStateEndpoint? = null
    private var latestDestination: Int? = null

    private val connectionListener = object : NavigationStateConnection.Listener {
        override fun onConnected(service: NavigationStateEndpoint) {
            this@CarSystemUiNavigationStateReporter.service = service
            latestDestination?.let { reportToService(service, it) }
        }

        override fun onDisconnected() {
            service = null
        }
    }

    override fun report(destination: LauncherNavigationDestination) {
        val destinationValue = when (destination) {
            LauncherNavigationDestination.Home -> NavigationContract.DESTINATION_HOME
            LauncherNavigationDestination.AppList -> NavigationContract.DESTINATION_APP_LIST
            LauncherNavigationDestination.None -> NavigationContract.DESTINATION_NONE
        }
        latestDestination = destinationValue
        if (!started) {
            started = true
            connection.start(connectionListener)
        }
        service?.let { reportToService(it, destinationValue) }
    }

    private fun reportToService(
        service: NavigationStateEndpoint,
        destination: Int,
    ) {
        if (!service.reportDestination(destination)) {
            this.service = null
            connection.stop()
            if (started) connection.start(connectionListener)
        }
    }
}
