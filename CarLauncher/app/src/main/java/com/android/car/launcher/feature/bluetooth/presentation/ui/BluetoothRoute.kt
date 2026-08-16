package com.android.car.launcher.feature.bluetooth.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.android.car.launcher.feature.bluetooth.presentation.viewmodel.BluetoothViewModel

@Composable
internal fun BluetoothRoute(
    viewModel: BluetoothViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    BluetoothScreen(
        state = state,
        onBack = onBack,
        onEnable = viewModel::enable,
        onScan = viewModel::startDiscovery,
        onRename = viewModel::renameLocalDevice,
    )
}
