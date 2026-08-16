package com.android.car.launcher.feature.dashboard.data.repository

import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.UserHandle
import com.android.car.launcher.feature.dashboard.domain.repository.LauncherNavigationDestination
import com.android.car.launcher.feature.dashboard.domain.repository.NavigationStateReporter
import com.miniivi.car.api.NavigationStateContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarSystemUiNavigationStateReporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : NavigationStateReporter {
    override fun report(destination: LauncherNavigationDestination) {
        val destinationValue = when (destination) {
            LauncherNavigationDestination.Home -> NavigationStateContract.DESTINATION_HOME
            LauncherNavigationDestination.AppList -> NavigationStateContract.DESTINATION_APP_LIST
            LauncherNavigationDestination.None -> NavigationStateContract.DESTINATION_NONE
        }
        val intent = Intent(NavigationStateContract.ACTION_DESTINATION_CHANGED)
            .setPackage(SYSTEM_UI_PACKAGE)
            .putExtra(NavigationStateContract.EXTRA_DESTINATION, destinationValue)
        context.sendBroadcastAsUser(
            intent,
            UserHandle.getUserHandleForUid(Process.SYSTEM_UID),
        )
    }

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.car.systemui"
    }
}
