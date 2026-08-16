package com.android.car.launcher.feature.dashboard.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.android.car.launcher.R
import com.android.car.launcher.core.navigation.AppDestination
import com.android.car.launcher.core.navigation.navigateTo
import com.android.car.launcher.core.ui.MiniIviTheme
import com.android.car.launcher.feature.dashboard.domain.model.AppLaunchTarget
import com.android.car.launcher.feature.dashboard.domain.model.ExternalLaunchTarget
import com.android.car.launcher.feature.dashboard.domain.model.HomeDestination
import com.android.car.launcher.feature.dashboard.domain.model.HomeStartDestination
import com.android.car.launcher.feature.dashboard.domain.model.PackageLaunch
import com.android.car.launcher.feature.dashboard.presentation.model.DashboardEffect
import com.android.car.launcher.feature.dashboard.presentation.ui.HomeScreen
import com.android.car.launcher.feature.dashboard.presentation.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    private val viewModel by viewModels<DashboardViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "event=activity_created component=HomeActivity")
        viewModel.onStartDestination(intent?.getStringExtra(HomeStartDestination.EXTRA))
        setContent {
            MiniIviTheme {
                val state by viewModel.state.collectAsState()
                LaunchedEffect(viewModel) {
                    viewModel.effects.collect { effect -> handleEffect(effect) }
                }
                BackHandler(enabled = state.destination == HomeDestination.Apps) {
                    viewModel.showHome()
                }
                HomeScreen(
                    state = state,
                    onBackToHome = viewModel::showHome,
                    onAppClick = { viewModel.onAppSelected(it.id) },
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
        viewModel.onStartDestination(intent.getStringExtra(HomeStartDestination.EXTRA))
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumed()
    }

    private fun handleEffect(effect: DashboardEffect) {
        when (effect) {
            is DashboardEffect.LaunchApp -> handleLaunchTarget(effect.target)
        }
    }

    private fun handleLaunchTarget(target: AppLaunchTarget) {
        when (target) {
            AppLaunchTarget.Media -> {
                navigateTo(AppDestination.Media)
                viewModel.onExternalAppOpened()
            }
            AppLaunchTarget.Bluetooth -> {
                navigateTo(AppDestination.Bluetooth)
                viewModel.onExternalAppOpened()
            }
            is AppLaunchTarget.External -> {
                val intent = createIntent(target.target)
                if (intent == null || !launch(intent)) showAppUnavailable()
            }
            AppLaunchTarget.Unavailable -> showAppUnavailable()
        }
    }

    private fun createIntent(target: ExternalLaunchTarget): Intent? = when (target) {
        is ExternalLaunchTarget.Packages ->
            target.launchers.firstNotNullOfOrNull(::resolvePackageLaunch)
        ExternalLaunchTarget.Dialer -> Intent(Intent.ACTION_DIAL)
        ExternalLaunchTarget.Browser ->
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        ExternalLaunchTarget.Settings -> Intent(Settings.ACTION_SETTINGS)
        is ExternalLaunchTarget.Component -> Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(target.packageName, target.className)
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
        viewModel.onExternalAppOpened()
        Log.i(
            TAG,
            "event=app_launch_completed target=${intent.component ?: intent.action ?: "unknown"}",
        )
        true
    } catch (exception: ActivityNotFoundException) {
        Log.w(TAG, "event=app_launch_failed reason=not_found", exception)
        false
    }

    private fun showAppUnavailable() {
        Log.w(TAG, "event=app_unavailable")
        Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val TAG = "MiniIviLauncher"
    }
}
