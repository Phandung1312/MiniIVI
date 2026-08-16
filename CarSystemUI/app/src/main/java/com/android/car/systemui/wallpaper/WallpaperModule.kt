package com.android.car.systemui.wallpaper

import com.android.car.systemui.core.CarSystemUIStartable
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class WallpaperModule {
    @Binds
    @IntoMap
    @ClassKey(WallpaperStartable::class)
    abstract fun bindWallpaperStartable(startable: WallpaperStartable): CarSystemUIStartable
}
