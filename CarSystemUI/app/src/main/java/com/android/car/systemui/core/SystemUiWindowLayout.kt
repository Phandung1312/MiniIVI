package com.android.car.systemui.core

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.android.car.systemui.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemUiWindowLayout @Inject constructor(
    @ApplicationContext private val context: Context,
    private val windowManager: WindowManager,
) {
    fun navigationWidthPx(): Int =
        context.resources.getDimensionPixelSize(R.dimen.navigation_rail_width)

    fun overlayWidthPx(): Int =
        (availableDisplayWidthPx() - navigationWidthPx()).coerceAtLeast(1)

    private fun availableDisplayWidthPx(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return context.resources.displayMetrics.widthPixels
        }
        val metrics = windowManager.currentWindowMetrics
        val cutout = metrics.windowInsets.displayCutout
        return (
            metrics.bounds.width() -
                (cutout?.safeInsetLeft ?: 0) -
                (cutout?.safeInsetRight ?: 0)
            ).coerceAtLeast(1)
    }
}
