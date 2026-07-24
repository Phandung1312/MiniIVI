package com.android.car.launcher.feature.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.car.launcher.R
import com.android.car.launcher.core.lifecycle.LifeCycleLogger
import com.android.car.launcher.core.ui.LoadingActionButton
import com.android.car.launcher.feature.bluetooth.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.model.DeviceInfo
import com.android.car.launcher.feature.bluetooth.service.BluetoothMonitorService
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
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                BluetoothScreen(
                    state = state,
                    onBack = ::finish,
                    onEnable = ::requestEnableBluetooth,
                    onScan = viewModel::startDiscovery,
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
        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }
}

@Composable
private fun BluetoothScreen(
    state: BluetoothState,
    onBack: () -> Unit,
    onEnable: () -> Unit,
    onScan: () -> Unit,
) {
    Surface(color = Color(0xFF0B0D10), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.back),
                        tint = Color.White,
                    )
                }
                Text(
                    stringResource(R.string.bluetooth),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))

            when {
                !state.supported -> StatusMessage(stringResource(R.string.bluetooth_not_supported))
                else -> BluetoothContent(state, onEnable, onScan)
            }
        }
    }
}

@Composable
private fun BluetoothContent(
    state: BluetoothState,
    onEnable: () -> Unit,
    onScan: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1D2228), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Bluetooth,
            contentDescription = null,
            tint = if (state.enabled) Color(0xFF65B5FF) else Color(0xFF78818B),
            modifier = Modifier
                .background(Color(0xFF2B333D), CircleShape)
                .padding(12.dp),
        )
        Text(
            if (state.enabled) stringResource(R.string.bluetooth_on)
            else stringResource(R.string.bluetooth_off),
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (!state.enabled) {
            Button(onClick = onEnable) { Text(stringResource(R.string.turn_on)) }
        } else {
            LoadingActionButton(
                text = stringResource(R.string.scan),
                loadingText = stringResource(R.string.scanning),
                loading = state.discovering,
                onClick = onScan,
            )
        }
    }
    Spacer(Modifier.height(18.dp))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            DeviceSectionTitle(
                title = stringResource(R.string.paired_devices),
                count = state.pairedDevices.size,
            )
        }
        if (state.pairedDevices.isEmpty()) {
            item { EmptyDevices() }
        } else {
            items(state.pairedDevices, key = { "paired-${it.address}" }) { device ->
                DeviceRow(device, connected = state.connectedDevices.any { it.address == device.address })
            }
        }

        item {
            DeviceSectionTitle(
                title = stringResource(R.string.nearby_devices),
                count = state.nearbyDevices.size,
            )
        }
        if (state.nearbyDevices.isEmpty()) {
            item { EmptyDevices() }
        } else {
            items(state.nearbyDevices, key = { "nearby-${it.address}" }) { device ->
                DeviceRow(device, connected = state.connectedDevices.any { it.address == device.address })
            }
        }
    }
}

@Composable
private fun DeviceSectionTitle(title: String, count: Int) {
    Text(
        "$title ($count)",
        color = Color(0xFFB8C0CC),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun DeviceRow(device: DeviceInfo, connected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1D2228), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Devices, contentDescription = null, tint = Color(0xFF65B5FF))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                device.name ?: stringResource(R.string.unknown_device),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Text(device.address, color = Color(0xFF8E99A6), fontSize = 13.sp)
        }
        if (connected) {
            Text("Connected", color = Color(0xFF77D89A), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyDevices() {
    Text(
        stringResource(R.string.no_devices),
        color = Color(0xFF78818B),
        modifier = Modifier.padding(vertical = 10.dp),
    )
}

@Composable
private fun StatusMessage(message: String) {
    Text(message, color = Color(0xFFB8C0CC), fontSize = 18.sp)
}
