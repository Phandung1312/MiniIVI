package com.android.car.launcher.feature.maps.data.repository

import com.android.car.launcher.feature.maps.domain.model.MapLaunchTarget
import com.android.car.launcher.feature.maps.domain.repository.MapLaunchTargetRepository
import com.miniivi.maps.contract.MapPreviewContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapLaunchTargetRepositoryImpl @Inject constructor() : MapLaunchTargetRepository {
    override fun target() = MapLaunchTarget(
        packageName = MapPreviewContract.MAPS_PACKAGE,
        className = MapPreviewContract.MAP_ACTIVITY,
    )
}
