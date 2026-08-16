package com.android.car.launcher.feature.dashboard.data.di

import com.android.car.launcher.feature.dashboard.data.repository.CarServiceHvacRepository
import com.android.car.launcher.feature.dashboard.data.repository.CarServiceVehicleRepository
import com.android.car.launcher.feature.dashboard.data.repository.CarSystemUiNavigationStateReporter
import com.android.car.launcher.feature.dashboard.domain.repository.HvacRepository
import com.android.car.launcher.feature.dashboard.domain.repository.NavigationStateReporter
import com.android.car.launcher.feature.dashboard.domain.repository.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    @Binds
    abstract fun bindVehicleRepository(repository: CarServiceVehicleRepository): VehicleRepository

    @Binds
    abstract fun bindHvacRepository(repository: CarServiceHvacRepository): HvacRepository

    @Binds
    abstract fun bindNavigationStateReporter(
        reporter: CarSystemUiNavigationStateReporter,
    ): NavigationStateReporter
}
