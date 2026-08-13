package com.android.car.launcher.feature.maps

import android.content.ComponentName
import android.content.Intent
import com.miniivi.maps.contract.MapPreviewContract

internal object MapLaunchResolver {
    const val packageName = MapPreviewContract.MAPS_PACKAGE
    const val activityName = MapPreviewContract.MAP_ACTIVITY

    fun resolve(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(
            ComponentName(
                packageName,
                activityName,
            ),
        )
}
