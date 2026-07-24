package com.android.car.launcher.feature.bluetooth

import androidx.lifecycle.ViewModel
import com.android.car.launcher.feature.bluetooth.repository.BluetoothRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val repository: BluetoothRepository,
) : ViewModel() {
    val state = repository.state

    fun refresh() = repository.refresh()

    fun startDiscovery() {
        repository.startDiscovery()
    }
}
