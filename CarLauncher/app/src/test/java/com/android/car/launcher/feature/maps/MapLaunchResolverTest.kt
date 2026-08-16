package com.android.car.launcher.feature.maps.domain.usecase

import com.android.car.launcher.feature.maps.data.repository.MapLaunchTargetRepositoryImpl
import com.miniivi.maps.contract.MapPreviewContract
import org.junit.Assert.assertEquals
import org.junit.Test

class MapLaunchResolverTest {
    @Test
    fun launchTargetAlwaysTargetsMiniIviMaps() {
        val target = ResolveMapLaunchTargetUseCase(MapLaunchTargetRepositoryImpl())()

        assertEquals(MapPreviewContract.MAPS_PACKAGE, target.packageName)
        assertEquals(MapPreviewContract.MAP_ACTIVITY, target.className)
    }
}
