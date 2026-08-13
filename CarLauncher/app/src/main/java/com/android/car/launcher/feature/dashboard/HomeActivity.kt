package com.android.car.launcher.feature.dashboard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Choreographer
import android.view.ViewTreeObserver
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    private val tag = javaClass.simpleName
    private val viewModel by viewModels<DashboardViewModel>()
    private var destination by mutableStateOf(HomeDestination.Home)
    private val firstFrameNotifier = FirstFrameNotifier(::sendLauncherFirstFrameDrawn)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate")
        destination = HomeStartDestination.from(intent)
        setContent {
            MiniIviTheme {
                val state by viewModel.state.collectAsState()
                BackHandler(enabled = destination == HomeDestination.Apps) {
                    destination = HomeDestination.Home
                }
                HomeScreen(
                    destination = destination,
                    state = state,
                    apps = HomeAppCatalog.apps,
                    onBackToHome = { destination = HomeDestination.Home },
                    onAppClick = ::openApp,
                    onPlayPause = viewModel::onPlayPause,
                    onNext = viewModel::onNext,
                    onPrevious = viewModel::onPrevious,
                )
            }
        }
        notifyAfterFirstFrame()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destination = HomeStartDestination.from(intent)
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume")
        viewModel.refresh()
    }

    override fun onPause() {
        Log.d(tag, "onPause")
        super.onPause()
    }

    private fun openApp(app: HomeApp) {
        when (val target = app.target) {
            HomeAppTarget.Media -> navigateTo(AppDestination.Media)
            HomeAppTarget.Bluetooth -> navigateTo(AppDestination.Bluetooth)
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
        true
    } catch (exception: ActivityNotFoundException) {
        Log.w(tag, "No activity can handle ${intent.action}", exception)
        false
    }

    private fun showAppUnavailable() {
        Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_SHORT).show()
    }

    private fun notifyAfterFirstFrame() {
        val decorView = window.decorView
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnPreDrawListener(this)
                }
                Choreographer.getInstance().postFrameCallback {
                    firstFrameNotifier.notifyFrameSubmitted()
                }
                return true
            }
        }
        decorView.viewTreeObserver.addOnPreDrawListener(listener)
    }

    private fun sendLauncherFirstFrameDrawn() {
        val signal = Intent(ACTION_LAUNCHER_FIRST_FRAME_DRAWN)
            .setPackage(CAR_SYSTEM_UI_PACKAGE)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        sendBroadcast(signal)
        reportFullyDrawn()
        Log.i(tag, "Launcher first-frame signal sent")
    }

    private companion object {
        const val ACTION_LAUNCHER_FIRST_FRAME_DRAWN =
            "com.miniivi.intent.action.LAUNCHER_FIRST_FRAME_DRAWN"
        const val CAR_SYSTEM_UI_PACKAGE = "com.android.car.systemui"
    }
}

internal class FirstFrameNotifier(private val notify: () -> Unit) {
    private var notified = false

    fun notifyFrameSubmitted() {
        if (notified) return
        notified = true
        notify()
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
