package com.android.car.launcher.feature.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.car.launcher.R
import com.android.car.launcher.core.lifecycle.LifeCycleLogger
import com.android.car.launcher.core.ui.WallpaperBackground
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

@Composable
private fun BluetoothScreen(
    state: BluetoothState,
    onBack: () -> Unit,
    onEnable: () -> Unit,
    onScan: () -> Unit,
) {
    WallpaperBackground {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x99101620), Color(0xD90A0E14)),
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xCC252C36), CircleShape)
                        .border(1.dp, Color(0x334F8FCB), CircleShape),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.back),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(18.dp))
                Text(
                    stringResource(R.string.bluetooth),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))

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
    val cardShape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xF22B3542), Color(0xE61C2530)),
                ),
            )
            .border(1.dp, Color(0x555F9FDC), cardShape)
            .heightIn(min = 116.dp)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    if (state.enabled) Color(0xFF174D78) else Color(0xFF303943),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (state.discovering) Icons.AutoMirrored.Filled.BluetoothSearching
                else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (state.enabled) Color(0xFF76C5FF) else Color(0xFF8D99A7),
                modifier = Modifier.size(36.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                if (state.enabled) stringResource(R.string.bluetooth_on)
                else stringResource(R.string.bluetooth_off),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                if (state.enabled) stringResource(R.string.bluetooth_on_description)
                else stringResource(R.string.bluetooth_off_description),
                color = Color(0xFFADB9C7),
                fontSize = 15.sp,
            )
        }
        if (!state.enabled) {
            BluetoothActionButton(
                text = stringResource(R.string.turn_on),
                onClick = onEnable,
            )
        } else {
            BluetoothActionButton(
                text = stringResource(R.string.scan),
                loadingText = stringResource(R.string.scanning),
                loading = state.discovering,
                onClick = onScan,
            )
        }
    }
    Spacer(Modifier.height(24.dp))

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = Color(0xFFE4ECF5),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = CircleShape,
            color = Color(0xFF283746),
            border = BorderStroke(1.dp, Color(0x445F9FDC)),
        ) {
            Text(
                count.toString(),
                color = Color(0xFF8FD0FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceInfo, connected: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (connected) Color(0xF2293942) else Color(0xE61C242D),
                shape,
            )
            .border(
                1.dp,
                if (connected) Color(0x6677D89A) else Color(0x2E9AB4CC),
                shape,
            )
            .heightIn(min = 84.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (connected) Color(0xFF214C3A) else Color(0xFF263B4D),
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Devices,
                contentDescription = null,
                tint = if (connected) Color(0xFF7CE0A3) else Color(0xFF76C5FF),
                modifier = Modifier.size(28.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Text(
                device.name ?: stringResource(R.string.unknown_device),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(device.address, color = Color(0xFF95A3B2), fontSize = 14.sp)
        }
        if (connected) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF214C3A), CircleShape)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF7CE0A3),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(R.string.connected),
                    color = Color(0xFF91E9B3),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EmptyDevices() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0x991A2129),
        border = BorderStroke(1.dp, Color(0x269AB4CC)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                tint = Color(0xFF6F7E8D),
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(R.string.no_devices),
                color = Color(0xFF8E9AA7),
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun StatusMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xE61C242D),
        border = BorderStroke(1.dp, Color(0x334F8FCB)),
    ) {
        Text(
            message,
            color = Color(0xFFC3CFDB),
            fontSize = 19.sp,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun BluetoothActionButton(
    text: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    loadingText: String = text,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.height(60.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3B9EEA),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF285E86),
            disabledContentColor = Color(0xFFD8EEFF),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFFD8EEFF),
                strokeWidth = 2.5.dp,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            if (loading) loadingText else text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
