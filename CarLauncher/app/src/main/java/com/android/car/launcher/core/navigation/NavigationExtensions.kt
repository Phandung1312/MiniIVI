package com.android.car.launcher.core.navigation

import android.content.Context
import android.content.Intent

fun Context.navigateTo(destination: AppDestination) {
    startActivity(Intent(this, destination.activityClass))
}
