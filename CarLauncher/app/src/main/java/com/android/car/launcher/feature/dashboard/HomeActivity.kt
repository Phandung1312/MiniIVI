package com.android.car.launcher.feature.dashboard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.android.car.launcher.R
import com.android.car.launcher.core.navigation.AppDestination
import com.android.car.launcher.core.navigation.navigateTo
import com.android.car.launcher.feature.dashboard.ui.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    private val tag = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate")
        setContent {
            MaterialTheme {
                HomeScreen(
                    apps = HomeAppCatalog.apps,
                    onAppClick = ::openApp,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume")
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
}
