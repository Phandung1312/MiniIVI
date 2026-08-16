package com.android.car.launcher.feature.maps.data.di

import com.android.car.launcher.feature.maps.data.repository.MapLaunchTargetRepositoryImpl
import com.android.car.launcher.feature.maps.domain.repository.MapLaunchTargetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MapsModule {
    @Binds
    abstract fun bindMapLaunchTargetRepository(
        repository: MapLaunchTargetRepositoryImpl,
    ): MapLaunchTargetRepository
}
