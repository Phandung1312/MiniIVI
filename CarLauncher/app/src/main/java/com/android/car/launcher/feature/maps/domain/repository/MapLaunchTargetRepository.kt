package com.android.car.launcher.feature.maps.domain.repository

import com.android.car.launcher.feature.maps.domain.model.MapLaunchTarget

interface MapLaunchTargetRepository {
    fun target(): MapLaunchTarget
}
