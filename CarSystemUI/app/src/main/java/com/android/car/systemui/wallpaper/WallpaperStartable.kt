package com.android.car.systemui.wallpaper

import android.content.res.Configuration
import com.android.car.systemui.core.CarSystemUIStartable
import com.android.car.systemui.domain.repository.StartupRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperStartable @Inject constructor(
    private val startupRepository: StartupRepository,
) : CarSystemUIStartable {
    override fun start() = startupRepository.initialize()

    override fun onConfigurationChanged(configuration: Configuration) = Unit
}
