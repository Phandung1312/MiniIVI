package com.android.car.launcher.feature.bluetooth.data.di

import com.android.car.launcher.feature.bluetooth.data.repository.BluetoothRepositoryImpl
import com.android.car.launcher.feature.bluetooth.domain.repository.BluetoothRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BluetoothModule {
    @Binds
    abstract fun bindBluetoothRepository(
        repository: BluetoothRepositoryImpl,
    ): BluetoothRepository
}
