package com.android.car.launcher.feature.dashboard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.car.launcher.R
import com.android.car.launcher.core.navigation.AppDestination
import com.android.car.launcher.core.navigation.navigateTo
import com.android.car.launcher.core.ui.MiniIviTheme
import com.android.car.launcher.feature.dashboard.ui.HomeScreen
import com.android.car.launcher.feature.maps.MapLaunchResolver
import com.miniivi.car.api.NavigationStateContract
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    private val viewModel by viewModels<DashboardViewModel>()
    private var destination by mutableStateOf(HomeDestination.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "event=activity_created component=HomeActivity")
        destination = HomeStartDestination.from(intent)
        logDebug("event=destination_selected destination=${destination.name.lowercase()}")
        setContent {
            MiniIviTheme {
                val state by viewModel.state.collectAsState()
                BackHandler(enabled = destination == HomeDestination.Apps) {
                    showHome()
                }
                HomeScreen(
                    destination = destination,
                    state = state,
                    apps = HomeAppCatalog.apps,
                    onBackToHome = ::showHome,
                    onAppClick = ::openApp,
                    onPlayPause = viewModel::onPlayPause,
                    onNext = viewModel::onNext,
                    onPrevious = viewModel::onPrevious,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destination = HomeStartDestination.from(intent)
        reportCurrentDestination()
        logDebug("event=destination_selected destination=${destination.name.lowercase()} source=new_intent")
    }

    override fun onResume() {
        super.onResume()
        logDebug("event=activity_resumed component=HomeActivity")
        reportCurrentDestination()
        viewModel.refresh()
    }

    override fun onPause() {
        logDebug("event=activity_paused component=HomeActivity")
        super.onPause()
    }

    private fun openApp(app: HomeApp) {
        logDebug("event=app_launch_requested app_id=${app.id}")
        when (val target = app.target) {
            HomeAppTarget.Media -> {
                navigateTo(AppDestination.Media)
                reportExternalAppOpened()
            }
            HomeAppTarget.Bluetooth -> {
                navigateTo(AppDestination.Bluetooth)
                reportExternalAppOpened()
            }
            HomeAppTarget.Maps -> {
                if (!launch(MapLaunchResolver.resolve())) showAppUnavailable()
            }
            is HomeAppTarget.Packages -> {
                val intent = target.launchers.firstNotNullOfOrNull(::resolvePackageLaunch)
                if (intent == null || !launch(intent)) showAppUnavailable()
            }
            HomeAppTarget.Dialer -> launchOrShowUnavailable(Intent(Intent.ACTION_DIAL))
            HomeAppTarget.Browser -> {
                launchOrShowUnavailable(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
                )
            }
            HomeAppTarget.Settings -> launchOrShowUnavailable(Intent(Settings.ACTION_SETTINGS))
            HomeAppTarget.Mock -> showAppUnavailable()
        }
    }

    private fun launchOrShowUnavailable(intent: Intent) {
        if (!launch(intent)) showAppUnavailable()
    }

    private fun resolvePackageLaunch(launcher: PackageLaunch): Intent? {
        if (launcher.category == null) {
            return packageManager.getLaunchIntentForPackage(launcher.packageName)
        }

        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(launcher.category)
            .setPackage(launcher.packageName)
        return intent.takeIf { it.resolveActivity(packageManager) != null }
    }

    private fun launch(intent: Intent): Boolean = try {
        startActivity(intent)
        reportExternalAppOpened()
        Log.i(
            TAG,
            "event=app_launch_completed target=${intent.component ?: intent.action ?: "unknown"}",
        )
        true
    } catch (exception: ActivityNotFoundException) {
        Log.w(
            TAG,
            "event=app_launch_failed target=${intent.component ?: intent.action ?: "unknown"} " +
                "reason=not_found",
            exception,
        )
        false
    }

    private fun showHome() {
        destination = HomeDestination.Home
        reportCurrentDestination()
    }

    private fun reportCurrentDestination() {
        val destinationValue = when (destination) {
            HomeDestination.Home -> NavigationStateContract.DESTINATION_HOME
            HomeDestination.Apps -> NavigationStateContract.DESTINATION_APP_LIST
        }
        sendNavigationState(destinationValue)
    }

    private fun reportExternalAppOpened() {
        sendNavigationState(NavigationStateContract.DESTINATION_NONE)
    }

    private fun sendNavigationState(destination: String) {
        val intent = Intent(NavigationStateContract.ACTION_DESTINATION_CHANGED)
            .setPackage(SYSTEM_UI_PACKAGE)
            .putExtra(NavigationStateContract.EXTRA_DESTINATION, destination)
        sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(Process.SYSTEM_UID))
        logDebug("event=navigation_state_reported destination=$destination")
    }

    private fun showAppUnavailable() {
        Log.w(TAG, "event=app_unavailable")
        Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_SHORT).show()
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "MiniIviLauncher"
        const val SYSTEM_UI_PACKAGE = "com.android.car.systemui"
    }

}

internal enum class HomeDestination { Home, Apps }

internal object HomeStartDestination {
    const val EXTRA = "com.android.car.launcher.extra.START_DESTINATION"
    const val APPS = "apps"

    fun from(intent: Intent?): HomeDestination = fromValue(intent?.getStringExtra(EXTRA))

    fun fromValue(value: String?): HomeDestination =
        if (value == APPS) HomeDestination.Apps
        else HomeDestination.Home
}
