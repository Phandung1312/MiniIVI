package com.android.car.launcher.feature.maps.domain.usecase

import com.android.car.launcher.feature.maps.domain.model.MapLaunchTarget
import com.android.car.launcher.feature.maps.domain.repository.MapLaunchTargetRepository
import javax.inject.Inject

class ResolveMapLaunchTargetUseCase @Inject constructor(
    private val repository: MapLaunchTargetRepository,
) {
    operator fun invoke(): MapLaunchTarget = repository.target()
}
