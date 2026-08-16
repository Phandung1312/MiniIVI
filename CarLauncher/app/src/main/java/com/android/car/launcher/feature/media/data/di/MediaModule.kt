package com.android.car.launcher.feature.media.data.di

import com.android.car.launcher.feature.media.data.repository.MediaRepositoryImpl
import com.android.car.launcher.feature.media.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds
    abstract fun bindMediaRepository(repository: MediaRepositoryImpl): MediaRepository
}
