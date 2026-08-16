package com.android.car.launcher.feature.dashboard.domain.usecase

import com.android.car.launcher.feature.dashboard.domain.model.AppLaunchTarget
import com.android.car.launcher.feature.dashboard.domain.model.ExternalLaunchTarget
import com.android.car.launcher.feature.maps.data.repository.MapLaunchTargetRepositoryImpl
import com.android.car.launcher.feature.maps.domain.usecase.ResolveMapLaunchTargetUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenDashboardAppUseCaseTest {
    private val useCase = OpenDashboardAppUseCase(
        ResolveMapLaunchTargetUseCase(MapLaunchTargetRepositoryImpl()),
    )

    @Test
    fun resolvesInternalFeatureTargets() {
        assertEquals(AppLaunchTarget.Media, useCase("media"))
        assertEquals(AppLaunchTarget.Bluetooth, useCase("bluetooth"))
    }

    @Test
    fun resolvesMapAndUnavailableTargetsWithoutAndroidIntents() {
        assertEquals(
            AppLaunchTarget.External(
                ExternalLaunchTarget.Component(
                    "com.miniivi.maps",
                    "com.miniivi.maps.MapActivity",
                ),
            ),
            useCase("maps"),
        )
        assertEquals(AppLaunchTarget.Unavailable, useCase("weather"))
        assertEquals(AppLaunchTarget.Unavailable, useCase("unknown"))
    }
}
