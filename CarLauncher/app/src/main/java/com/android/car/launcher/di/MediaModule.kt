package com.android.car.launcher.di

import com.android.car.launcher.feature.media.MediaController
import com.android.car.launcher.feature.media.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {
    @Binds
    abstract fun bindMediaController(repository: MediaRepository): MediaController
}
