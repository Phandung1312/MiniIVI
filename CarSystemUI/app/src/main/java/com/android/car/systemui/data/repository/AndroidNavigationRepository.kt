package com.android.car.systemui.data.repository

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNavigationRepository @Inject constructor(
    private val currentUserProvider: CurrentUserProvider,
) : NavigationRepository {
    override fun goHome() {
        logDebug("event=navigation_requested destination=home")
        injectKey(KeyEvent.KEYCODE_HOME)
    }

    override fun openSettings() {
        logDebug("event=navigation_requested destination=settings")
        runCatching {
            val user = currentUserProvider.userHandle()
            val launcherApps = currentUserProvider.context().getSystemService(LauncherApps::class.java)
            val settingsActivity = launcherApps
                ?.getActivityList(CAR_SETTINGS_PACKAGE, user)
                ?.firstOrNull()
            if (settingsActivity != null) {
                launcherApps.startMainActivity(settingsActivity.componentName, user, Rect(), null)
            } else {
                currentUserProvider.launch(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            Log.i(TAG, "event=navigation_completed destination=settings")
        }.onFailure { error ->
            Log.e(TAG, "event=navigation_failed destination=settings", error)
        }
    }

    override fun openAppList() {
        logDebug("event=navigation_requested destination=app_list")
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

    override fun openPhone() {
        logDebug("event=navigation_requested destination=phone")
        launch(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openWifiSettings() {
        logDebug("event=navigation_requested destination=wifi_settings")
        launch(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openWirelessSettings() {
        logDebug("event=navigation_requested destination=wireless_settings")
        launch(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openBluetoothSettings() {
        logDebug("event=navigation_requested destination=bluetooth_settings")
        launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun openCamera(): Boolean {
        logDebug("event=navigation_requested destination=camera")
        val intent = Intent(CAMERA_ACTION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            val context = currentUserProvider.context()
            context.packageManager.resolveActivity(intent, 0) != null && launch(intent)
        }.onFailure { error -> Log.e(TAG, "event=navigation_failed destination=camera", error) }
            .getOrDefault(false)
    }

    private fun launch(intent: Intent): Boolean = try {
            currentUserProvider.launch(intent)
            Log.i(
                TAG,
                "event=navigation_completed destination=${intent.component ?: intent.action}",
            )
            true
        } catch (error: ActivityNotFoundException) {
            Log.e(
                TAG,
                "event=navigation_failed destination=${intent.component ?: intent.action} " +
                    "reason=not_found",
                error,
            )
            false
        } catch (error: SecurityException) {
            Log.e(TAG, "event=navigation_failed reason=security_exception", error)
            false
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
            Log.i(TAG, "event=navigation_completed destination=key key_code=$keyCode")
        }.onFailure { error ->
            Log.e(TAG, "event=navigation_failed destination=key key_code=$keyCode", error)
        }
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "MiniIviNavigation"
        const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
        const val CAR_SETTINGS_PACKAGE = "com.android.car.settings"
        const val CAR_LAUNCHER_PACKAGE = "com.android.car.launcher"
        const val APP_LIST_ACTIVITY = "com.android.car.launcher.feature.dashboard.HomeActivity"
        const val START_DESTINATION_EXTRA = "com.android.car.launcher.extra.START_DESTINATION"
        const val APPS_DESTINATION = "apps"
        const val CAMERA_ACTION = "com.miniivi.car.action.OPEN_CAMERA_VIEW"
    }
}
