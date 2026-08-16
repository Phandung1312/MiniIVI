package com.android.car.launcher.core.navigation

import android.app.Activity
import com.android.car.launcher.feature.bluetooth.presentation.BluetoothActivity
import com.android.car.launcher.feature.media.presentation.MediaActivity

sealed class AppDestination(val activityClass: Class<out Activity>) {
    data object Media : AppDestination(MediaActivity::class.java)
    data object Bluetooth : AppDestination(BluetoothActivity::class.java)
}
