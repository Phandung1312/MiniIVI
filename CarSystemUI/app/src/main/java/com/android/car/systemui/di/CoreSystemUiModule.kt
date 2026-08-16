package com.android.car.systemui.di

import android.content.Context
import android.view.WindowManager
import com.miniivi.car.client.MiniIviCarClient
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
