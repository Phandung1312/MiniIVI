package com.android.car.launcher.feature.bluetooth

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.android.car.launcher.core.ui.MiniIviTheme
import com.android.car.launcher.core.lifecycle.LifeCycleLogger
import com.android.car.launcher.feature.bluetooth.ui.BluetoothRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BluetoothActivity : LifeCycleLogger() {
    private val viewModel by viewModels<BluetoothViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniIviTheme {
                BluetoothRoute(
                    viewModel = viewModel,
                    onBack = ::finish,
                )
            }
        }
    }
}
