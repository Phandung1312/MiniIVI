package com.android.car.launcher.feature.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.android.car.launcher.core.ui.MiniIviTheme
import com.android.car.launcher.core.lifecycle.LifeCycleLogger
import com.android.car.launcher.feature.bluetooth.service.BluetoothMonitorService
import com.android.car.launcher.feature.bluetooth.ui.BluetoothRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BluetoothActivity : LifeCycleLogger() {
    private val viewModel by viewModels<BluetoothViewModel>()

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniIviTheme {
                BluetoothRoute(
                    viewModel = viewModel,
                    onBack = ::finish,
                    onEnable = ::requestEnableBluetooth,
                )
            }
        }
        viewModel.refresh()
    }

    override fun onStart() {
        super.onStart()
        startService(Intent(this, BluetoothMonitorService::class.java))
        viewModel.refresh()
    }

    override fun onDestroy() {
        stopService(Intent(this, BluetoothMonitorService::class.java))
        super.onDestroy()
    }

    private fun requestEnableBluetooth() {
        try {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (error: SecurityException) {
            Log.e(TAG, "BLUETOOTH_CONNECT must be pre-granted by the system image", error)
        }
    }

    private companion object {
        const val TAG = "BluetoothActivity"
    }
}
