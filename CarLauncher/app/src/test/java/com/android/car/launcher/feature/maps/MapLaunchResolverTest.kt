package com.android.car.launcher.feature.maps

import com.miniivi.maps.contract.MapPreviewContract
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLaunchResolverTest {
    @Test
    fun launchIntentAlwaysTargetsMiniIviMaps() {
        assertEquals(MapPreviewContract.MAPS_PACKAGE, MapLaunchResolver.packageName)
        assertEquals(MapPreviewContract.MAP_ACTIVITY, MapLaunchResolver.activityName)
    }
}
