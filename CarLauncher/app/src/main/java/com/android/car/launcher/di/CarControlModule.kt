package com.android.car.launcher.di

import android.content.Context
import com.miniivi.car.client.MiniIviCarClient
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
}
