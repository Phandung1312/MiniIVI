package com.android.car.launcher.di

import android.content.Context
import com.miniivi.car.client.MiniIviCarClient
import com.android.car.launcher.feature.dashboard.repository.CarServiceHvacRepository
import com.android.car.launcher.feature.dashboard.repository.CarServiceVehicleRepository
import com.android.car.launcher.feature.dashboard.repository.HvacRepository
import com.android.car.launcher.feature.dashboard.repository.VehicleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CarControlModule {
    @Provides
    @Singleton
    fun provideMiniIviCarClient(
        @ApplicationContext context: Context,
    ): MiniIviCarClient = MiniIviCarClient(context)

    @Provides
    @Singleton
    fun provideVehicleRepository(client: MiniIviCarClient): VehicleRepository =
        CarServiceVehicleRepository(client)

    @Provides
    @Singleton
    fun provideHvacRepository(client: MiniIviCarClient): HvacRepository =
        CarServiceHvacRepository(client)
}
