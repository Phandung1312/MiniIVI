package com.android.car.systemui.core

import android.content.res.Configuration

/** A major process-scoped CarSystemUI feature started by the application. */
interface CarSystemUIStartable {
    fun start()
    fun onConfigurationChanged(configuration: Configuration)
}
