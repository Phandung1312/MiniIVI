package com.android.car.launcher.feature.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.car.launcher.feature.bluetooth.repository.BluetoothRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val repository: BluetoothRepository,
) : ViewModel() {
    val state = repository.state.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        com.android.car.launcher.feature.bluetooth.model.BluetoothState(supported = false),
    )

    init {
        repository.start()
        repository.refresh()
    }

    fun refresh() = repository.refresh()

    fun startDiscovery() {
        repository.startDiscovery()
    }

    fun enable() {
        repository.enable()
    }

    fun renameLocalDevice(name: String): Boolean = repository.renameLocalDevice(name)
}
