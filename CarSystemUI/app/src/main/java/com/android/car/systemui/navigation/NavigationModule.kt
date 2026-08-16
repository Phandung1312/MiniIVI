package com.android.car.systemui.navigation

import com.android.car.systemui.core.CarSystemUIStartable
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {
    @Binds
    @IntoMap
    @ClassKey(NavigationComponent::class)
    abstract fun bindNavigationComponent(component: NavigationComponent): CarSystemUIStartable
}
