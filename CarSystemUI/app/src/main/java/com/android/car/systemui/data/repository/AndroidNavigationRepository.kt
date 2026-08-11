package com.android.car.systemui.data.repository

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent

class AndroidNavigationRepository(
    private val currentUserProvider: CurrentUserProvider,
) : NavigationRepository {
    override fun goHome() = injectKey(KeyEvent.KEYCODE_HOME)

    override fun openSettings() {
        runCatching {
            val user = currentUserProvider.userHandle()
            val launcherApps = currentUserProvider.context().getSystemService(LauncherApps::class.java)
            val settingsActivity = launcherApps
                ?.getActivityList(CAR_SETTINGS_PACKAGE, user)
                ?.firstOrNull()
            if (settingsActivity != null) {
                launcherApps.startMainActivity(settingsActivity.componentName, user, Rect(), null)
            } else {
                currentUserProvider.context().startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Unable to launch Settings for foreground user", error)
        }
    }

    override fun openAppList() {
        launch(
            Intent(Intent.ACTION_MAIN)
                .setClassName(CAR_LAUNCHER_PACKAGE, APP_LIST_ACTIVITY)
                .putExtra(START_DESTINATION_EXTRA, APPS_DESTINATION)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
        )
    }

    private fun launch(intent: Intent) {
        try {
            currentUserProvider.context().startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "Unable to launch ${intent.component ?: intent.action}", error)
        } catch (error: SecurityException) {
            Log.e(TAG, "Unable to launch activity for current user", error)
        }
    }

    private fun injectKey(keyCode: Int) {
        runCatching {
            val inputManagerClass = Class.forName("android.hardware.input.InputManager")
            val inputManager = inputManagerClass.getMethod("getInstance").invoke(null)
            val inject = inputManagerClass.getMethod(
                "injectInputEvent",
                android.view.InputEvent::class.java,
                Int::class.javaPrimitiveType,
            )
            val now = SystemClock.uptimeMillis()
            inject.invoke(
                inputManager,
                KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0),
                INJECT_INPUT_EVENT_MODE_ASYNC,
            )
            inject.invoke(
                inputManager,
                KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0),
                INJECT_INPUT_EVENT_MODE_ASYNC,
            )
        }.onFailure { error -> Log.e(TAG, "Unable to inject navigation key $keyCode", error) }
    }

    private companion object {
        const val TAG = "CarSystemUI-Navigation"
        const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
        const val CAR_SETTINGS_PACKAGE = "com.android.car.settings"
        const val CAR_LAUNCHER_PACKAGE = "com.android.car.launcher"
        const val APP_LIST_ACTIVITY = "com.android.car.launcher.feature.dashboard.HomeActivity"
        const val START_DESTINATION_EXTRA = "com.android.car.launcher.extra.START_DESTINATION"
        const val APPS_DESTINATION = "apps"
    }
}
