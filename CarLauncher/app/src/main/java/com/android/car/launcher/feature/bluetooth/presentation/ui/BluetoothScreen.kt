package com.android.car.launcher.feature.bluetooth.presentation.ui

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.car.launcher.R
import com.android.car.launcher.core.ui.MiniIviColors
import com.android.car.launcher.core.ui.WallpaperBackground
import com.android.car.launcher.feature.bluetooth.domain.model.BluetoothState
import com.android.car.launcher.feature.bluetooth.domain.model.DeviceInfo

@Composable
internal fun BluetoothScreen(
    state: BluetoothState,
    onBack: () -> Unit,
    onEnable: () -> Unit,
    onScan: () -> Unit,
    onRename: (String) -> Boolean,
) {
    WallpaperBackground {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MiniIviColors.Primary.copy(alpha = 0.04f)),
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
                        .background(MiniIviColors.SurfaceRaised, CircleShape)
                        .border(1.dp, MiniIviColors.Border, CircleShape),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.back),
                        tint = MiniIviColors.TextPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(18.dp))
                Text(
                    stringResource(R.string.bluetooth),
                    color = MiniIviColors.TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(24.dp))

            when {
                !state.supported -> StatusMessage(stringResource(R.string.bluetooth_not_supported))
                else -> BluetoothContent(state, onEnable, onScan, onRename)
            }
        }
    }
}

@Composable
private fun BluetoothContent(
    state: BluetoothState,
    onEnable: () -> Unit,
    onScan: () -> Unit,
    onRename: (String) -> Boolean,
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var renameFailed by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_device)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = {
                            renameText = it
                            renameFailed = false
                        },
                        label = { Text(stringResource(R.string.new_device_name)) },
                        singleLine = true,
                        isError = renameFailed,
                        supportingText = if (renameFailed) {
                            { Text(stringResource(R.string.rename_failed)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        if (onRename(renameText)) {
                            showRenameDialog = false
                        } else {
                            renameFailed = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    val cardShape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(MiniIviColors.SurfaceRaised, MiniIviColors.Surface),
                ),
            )
            .border(1.dp, MiniIviColors.Border, cardShape)
            .heightIn(min = 116.dp)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    if (state.enabled) MiniIviColors.Primary.copy(alpha = 0.18f)
                    else MiniIviColors.TextSecondary.copy(alpha = 0.12f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (state.discovering) Icons.AutoMirrored.Filled.BluetoothSearching
                else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (state.enabled) MiniIviColors.Primary else MiniIviColors.TextSecondary,
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
                color = MiniIviColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                if (state.enabled) stringResource(R.string.bluetooth_on_description)
                else stringResource(R.string.bluetooth_off_description),
                color = MiniIviColors.TextSecondary,
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
            LocalDeviceCard(
                name = state.localName,
                address = state.localAddress,
                renameEnabled = state.enabled,
                onRename = {
                    renameText = state.localName.orEmpty()
                    renameFailed = false
                    showRenameDialog = true
                },
            )
        }

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
private fun LocalDeviceCard(
    name: String?,
    address: String?,
    renameEnabled: Boolean,
    onRename: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiniIviColors.Surface, shape)
            .border(1.dp, MiniIviColors.Border, shape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MiniIviColors.Primary.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MiniIviColors.Primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 18.dp),
        ) {
            Text(
                stringResource(R.string.this_device),
                color = MiniIviColors.Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                name ?: stringResource(R.string.unknown_device),
                color = MiniIviColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${stringResource(R.string.bluetooth_address)}: " +
                    (address ?: stringResource(R.string.address_unavailable)),
                color = MiniIviColors.TextSecondary,
                fontSize = 14.sp,
            )
        }
        Button(
            onClick = onRename,
            enabled = renameEnabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MiniIviColors.Primary),
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.rename))
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
            color = MiniIviColors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = CircleShape,
            color = MiniIviColors.Primary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, MiniIviColors.Border),
        ) {
            Text(
                count.toString(),
                color = MiniIviColors.Primary,
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
                if (connected) Color(0x66E1F0E6) else MiniIviColors.Surface,
                shape,
            )
            .border(
                1.dp,
                if (connected) Color(0xFF8CC5A0) else MiniIviColors.Border,
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
                    if (connected) Color(0x66CDE6D5) else MiniIviColors.Primary.copy(alpha = 0.16f),
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Devices,
                contentDescription = null,
                tint = if (connected) Color(0xFF39794F) else MiniIviColors.Primary,
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
                color = MiniIviColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(device.address, color = MiniIviColors.TextSecondary, fontSize = 14.sp)
        }
        if (connected) {
            Row(
                modifier = Modifier
                    .background(Color(0x66CDE6D5), CircleShape)
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF39794F),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(R.string.connected),
                    color = Color(0xFF39794F),
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
        color = MiniIviColors.Surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MiniIviColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                tint = MiniIviColors.TextSecondary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(R.string.no_devices),
                color = MiniIviColors.TextSecondary,
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
        color = MiniIviColors.Surface,
        border = BorderStroke(1.dp, MiniIviColors.Border),
    ) {
        Text(
            message,
            color = MiniIviColors.TextPrimary,
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
            containerColor = MiniIviColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = MiniIviColors.Primary.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.72f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
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
