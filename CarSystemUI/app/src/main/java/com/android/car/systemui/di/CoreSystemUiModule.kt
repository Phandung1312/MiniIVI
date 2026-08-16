package com.android.car.systemui.di

import android.content.Context
import android.view.WindowManager
import com.android.car.systemui.core.AndroidSystemUserPolicy
import com.android.car.systemui.core.SystemUserPolicy
import com.android.car.systemui.data.repository.AndroidNavigationRepository
import com.android.car.systemui.data.repository.AndroidStartupRepository
import com.android.car.systemui.data.repository.AudioRepository
import com.android.car.systemui.data.repository.BrightnessRepository
import com.android.car.systemui.data.repository.CarServiceAudioRepository
import com.android.car.systemui.data.repository.CarServiceBrightnessRepository
import com.android.car.systemui.data.repository.CarServiceExtendedControlsRepository
import com.android.car.systemui.data.repository.CarServiceHvacRepository
import com.android.car.systemui.data.repository.ExtendedControlsRepository
import com.android.car.systemui.data.repository.HvacRepository
import com.android.car.systemui.data.repository.NavigationRepository
import com.android.car.systemui.data.repository.StartupRepository
import com.miniivi.car.client.MiniIviCarClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object CoreSystemUiModule {
    @Provides
    @Singleton
    fun provideMiniIviCarClient(
        @ApplicationContext context: Context,
    ): MiniIviCarClient = MiniIviCarClient(context)

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideWindowManager(
        @ApplicationContext context: Context,
    ): WindowManager = requireNotNull(context.getSystemService(WindowManager::class.java))
}

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
