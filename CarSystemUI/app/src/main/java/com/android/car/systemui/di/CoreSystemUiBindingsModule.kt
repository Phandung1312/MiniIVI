package com.android.car.systemui.di

import com.android.car.systemui.data.policy.AndroidSystemUserPolicy
import com.android.car.systemui.data.repository.carservice.CarServiceAudioRepository
import com.android.car.systemui.data.repository.carservice.CarServiceBrightnessRepository
import com.android.car.systemui.data.repository.carservice.CarServiceExtendedControlsRepository
import com.android.car.systemui.data.repository.carservice.CarServiceHvacRepository
import com.android.car.systemui.data.repository.navigation.AndroidNavigationRepository
import com.android.car.systemui.data.repository.startup.AndroidStartupRepository
import com.android.car.systemui.data.session.CarServiceSessionImpl
import com.android.car.systemui.domain.policy.SystemUserPolicy
import com.android.car.systemui.domain.repository.AudioRepository
import com.android.car.systemui.domain.repository.BrightnessRepository
import com.android.car.systemui.domain.repository.CarServiceSession
import com.android.car.systemui.domain.repository.ExtendedControlsRepository
import com.android.car.systemui.domain.repository.HvacRepository
import com.android.car.systemui.domain.repository.NavigationRepository
import com.android.car.systemui.domain.repository.StartupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreSystemUiBindingsModule {
    @Binds
    @Singleton
    abstract fun bindSystemUserPolicy(
        policy: AndroidSystemUserPolicy,
    ): SystemUserPolicy

    @Binds
    @Singleton
    abstract fun bindCarServiceSession(
        session: CarServiceSessionImpl,
    ): CarServiceSession

    @Binds
    @Singleton
    abstract fun bindNavigationRepository(
        repository: AndroidNavigationRepository,
    ): NavigationRepository

    @Binds
    @Singleton
    abstract fun bindStartupRepository(
        repository: AndroidStartupRepository,
    ): StartupRepository

    @Binds
    @Singleton
    abstract fun bindBrightnessRepository(
        repository: CarServiceBrightnessRepository,
    ): BrightnessRepository

    @Binds
    @Singleton
    abstract fun bindAudioRepository(
        repository: CarServiceAudioRepository,
    ): AudioRepository

    @Binds
    @Singleton
    abstract fun bindHvacRepository(
        repository: CarServiceHvacRepository,
    ): HvacRepository

    @Binds
    @Singleton
    abstract fun bindExtendedControlsRepository(
        repository: CarServiceExtendedControlsRepository,
    ): ExtendedControlsRepository
}
