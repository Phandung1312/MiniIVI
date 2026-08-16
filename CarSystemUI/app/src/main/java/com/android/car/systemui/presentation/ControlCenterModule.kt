package com.android.car.systemui.presentation

import com.android.car.systemui.core.CarSystemUIStartable
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class ControlCenterModule {
    @Binds
    @IntoMap
    @ClassKey(ControlCenterComponent::class)
    abstract fun bindControlCenterComponent(component: ControlCenterComponent): CarSystemUIStartable
}
