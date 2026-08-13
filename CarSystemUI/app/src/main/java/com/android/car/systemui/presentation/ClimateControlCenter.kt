@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.android.car.systemui.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.LockPerson
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ScreenLockLandscape
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SevereCold
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.android.car.systemui.R
import com.android.car.systemui.data.model.ClimateZone
import com.miniivi.car.api.ClimateFanDirection
import com.miniivi.car.api.ClimateZoneControlState
import com.miniivi.car.api.HvacZone
import com.miniivi.car.api.QuickControl
import com.miniivi.car.api.TemperatureUnit
import java.util.Locale

@Composable
fun ControlCenterOverlay(
    visible: Boolean,
    state: ControlCenterUiState,
    onDismiss: () -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onTemperatureDecrease: (ClimateZone) -> Unit,
    onTemperatureIncrease: (ClimateZone) -> Unit,
    onAcChanged: (Boolean) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onPowerChanged: (Boolean) -> Unit = {},
    onAutoChanged: (Boolean) -> Unit = {},
    onSyncChanged: (Boolean) -> Unit = {},
    onRecirculationChanged: (Boolean) -> Unit = {},
    onFanSpeedChanged: (ClimateZone, Int) -> Unit = { _, _ -> },
    onFanDirectionChanged: (ClimateZone, Int) -> Unit = { _, _ -> },
    onFrontDefrostChanged: (Boolean) -> Unit = {},
    onRearDefrostChanged: (Boolean) -> Unit = {},
    onSeatHeatingChanged: (ClimateZone, Int) -> Unit = { _, _ -> },
    onSeatVentilationChanged: (ClimateZone, Int) -> Unit = { _, _ -> },
    onQuickControlChanged: (Int, Boolean) -> Unit = { _, _ -> },
    onShowMoreClimate: () -> Unit = {},
    onHideMoreClimate: () -> Unit = {},
    onMaxAcChanged: (Boolean) -> Unit = {},
    onMaxDefrostChanged: (Boolean) -> Unit = {},
    onAutoRecirculationChanged: (Boolean) -> Unit = {},
    onSteeringWheelHeatChanged: (Int) -> Unit = {},
    onTemperatureUnitChanged: (Int) -> Unit = {},
    onOpenWifiSettings: () -> Unit = {},
    onOpenWirelessSettings: () -> Unit = {},
    onOpenBluetoothSettings: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onHideCamera: () -> Unit = {},
    onScreenOff: () -> Unit = {},
    onDismissScreenCurtain: () -> Unit = {},
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f)),
        ) {
            SystemUiGlassPanel(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                shape = RoundedCornerShape(28.dp),
                raised = false,
            ) {
                BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp)) {
                    val compact = maxWidth < 1100.dp || maxHeight < 640.dp
                    Column(Modifier.fillMaxSize()) {
                        ControlHeader(onSettings, onDismiss, compact)
                        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
                        ) {
                            ClimateDeck(
                                state = state,
                                compact = compact,
                                onTemperatureDecrease = onTemperatureDecrease,
                                onTemperatureIncrease = onTemperatureIncrease,
                                onPowerChanged = onPowerChanged,
                                onAcChanged = onAcChanged,
                                onAutoChanged = onAutoChanged,
                                onSyncChanged = onSyncChanged,
                                onRecirculationChanged = onRecirculationChanged,
                                onFanSpeedChanged = onFanSpeedChanged,
                                onFanDirectionChanged = onFanDirectionChanged,
                                onFrontDefrostChanged = onFrontDefrostChanged,
                                onRearDefrostChanged = onRearDefrostChanged,
                                onShowMoreClimate = onShowMoreClimate,
                                modifier = Modifier.weight(3.1f).fillMaxHeight(),
                            )
                            UtilityDeck(
                                state = state,
                                compact = compact,
                                onBrightnessChanged = onBrightnessChanged,
                                onBrightnessChangeFinished = onBrightnessChangeFinished,
                                onVolumeChanged = onVolumeChanged,
                                onQuickControlChanged = onQuickControlChanged,
                                onOpenWifiSettings = onOpenWifiSettings,
                                onOpenWirelessSettings = onOpenWirelessSettings,
                                onOpenBluetoothSettings = onOpenBluetoothSettings,
                                onOpenCamera = onOpenCamera,
                                onScreenOff = onScreenOff,
                                modifier = Modifier.weight(1.18f).fillMaxHeight(),
                            )
                        }
                        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                        SeatDeck(
                            state = state,
                            compact = compact,
                            onSeatHeatingChanged = onSeatHeatingChanged,
                            onSeatVentilationChanged = onSeatVentilationChanged,
                        )
                    }
                }
            }
            if (state.moreClimateVisible) {
                MoreClimateDialog(
                    state = state,
                    onDismiss = onHideMoreClimate,
                    onMaxAcChanged = onMaxAcChanged,
                    onMaxDefrostChanged = onMaxDefrostChanged,
                    onAutoRecirculationChanged = onAutoRecirculationChanged,
                    onSteeringWheelHeatChanged = onSteeringWheelHeatChanged,
                    onTemperatureUnitChanged = onTemperatureUnitChanged,
                )
            }
            if (state.cameraVisible) MockCameraOverlay(onHideCamera)
            if (state.screenCurtainVisible) ScreenCurtain(onDismissScreenCurtain)
        }
    }
}

@Composable
private fun ControlHeader(onSettings: () -> Unit, onDismiss: () -> Unit, compact: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.climate_controls_title),
            fontSize = if (compact) 24.sp else 32.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        B02ControlTile(
            icon = Icons.Rounded.Settings,
            label = stringResource(R.string.navigation_settings),
            compact = true,
            showLabel = false,
            onClick = onSettings,
        )
        Spacer(Modifier.width(10.dp))
        B02ControlTile(
            icon = Icons.Rounded.Close,
            label = stringResource(R.string.control_close),
            compact = true,
            showLabel = false,
            onClick = onDismiss,
        )
    }
}

@Composable
private fun ClimateDeck(
    state: ControlCenterUiState,
    compact: Boolean,
    onTemperatureDecrease: (ClimateZone) -> Unit,
    onTemperatureIncrease: (ClimateZone) -> Unit,
    onPowerChanged: (Boolean) -> Unit,
    onAcChanged: (Boolean) -> Unit,
    onAutoChanged: (Boolean) -> Unit,
    onSyncChanged: (Boolean) -> Unit,
    onRecirculationChanged: (Boolean) -> Unit,
    onFanSpeedChanged: (ClimateZone, Int) -> Unit,
    onFanDirectionChanged: (ClimateZone, Int) -> Unit,
    onFrontDefrostChanged: (Boolean) -> Unit,
    onRearDefrostChanged: (Boolean) -> Unit,
    onShowMoreClimate: () -> Unit,
    modifier: Modifier,
) {
    val climate = state.extendedControls.climate
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
        ) {
            TemperatureDeck(
                label = stringResource(R.string.climate_driver),
                zone = climate.driverZone,
                compact = compact,
                onDecrease = { onTemperatureDecrease(ClimateZone.LEFT) },
                onIncrease = { onTemperatureIncrease(ClimateZone.LEFT) },
                onDirection = { onFanDirectionChanged(ClimateZone.LEFT, it) },
                modifier = Modifier.weight(0.82f).fillMaxHeight(),
            )
            Column(
                modifier = Modifier.weight(1.82f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
            ) {
                CabinAirflowCanvas(
                    powerOn = climate.powerOn,
                    driverDirection = climate.driverZone.fanDirection,
                    passengerDirection = climate.passengerZone.fanDirection,
                    driverFanStrength = climate.driverZone.fanSpeed.toFloat() /
                        climate.driverZone.maximumFanSpeed.coerceAtLeast(1),
                    passengerFanStrength = climate.passengerZone.fanSpeed.toFloat() /
                        climate.passengerZone.maximumFanSpeed.coerceAtLeast(1),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                FanControl(
                    zone = climate.driverZone,
                    compact = compact,
                    onChange = {
                        onFanSpeedChanged(ClimateZone.LEFT, it)
                        if (climate.syncOn) onFanSpeedChanged(ClimateZone.RIGHT, it)
                    },
                )
            }
            TemperatureDeck(
                label = stringResource(R.string.climate_passenger),
                zone = climate.passengerZone,
                compact = compact,
                onDecrease = { onTemperatureDecrease(ClimateZone.RIGHT) },
                onIncrease = { onTemperatureIncrease(ClimateZone.RIGHT) },
                onDirection = { onFanDirectionChanged(ClimateZone.RIGHT, it) },
                modifier = Modifier.weight(0.82f).fillMaxHeight(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(if (compact) 60.dp else 84.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp),
        ) {
            ClimateToggle(Icons.Rounded.PowerSettingsNew, "Power", climate.powerOn, compact) {
                onPowerChanged(!climate.powerOn)
            }
            ClimateToggle(Icons.Rounded.AcUnit, "A/C", climate.acOn, compact) {
                onAcChanged(!climate.acOn)
            }
            ClimateToggle(Icons.Rounded.Thermostat, "AUTO", climate.autoOn, compact) {
                onAutoChanged(!climate.autoOn)
            }
            ClimateToggle(Icons.Rounded.Sync, "SYNC", climate.syncOn, compact) {
                onSyncChanged(!climate.syncOn)
            }
            ClimateToggle(Icons.Rounded.DirectionsCar, "Recirculation", climate.recirculationOn, compact) {
                onRecirculationChanged(!climate.recirculationOn)
            }
            ClimateToggle(Icons.Rounded.SevereCold, "Front defrost", climate.frontDefrostOn, compact) {
                onFrontDefrostChanged(!climate.frontDefrostOn)
            }
            ClimateToggle(Icons.Rounded.SevereCold, "Rear defrost", climate.rearDefrostOn, compact) {
                onRearDefrostChanged(!climate.rearDefrostOn)
            }
            ClimateToggle(Icons.Rounded.MoreHoriz, "More climate", false, compact, onShowMoreClimate)
        }
    }
}

@Composable
private fun TemperatureDeck(
    label: String,
    zone: ClimateZoneControlState,
    compact: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onDirection: (Int) -> Unit,
    modifier: Modifier,
) {
    SystemUiGlassPanel(modifier, RoundedCornerShape(24.dp)) {
        Column(
            Modifier.fillMaxSize().padding(if (compact) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            Text(formatTemperature(zone.temperatureCelsius), fontSize = if (compact) 29.sp else 42.sp)
            Row {
                B02ControlTile(
                    Icons.Rounded.Remove,
                    "Decrease $label temperature",
                    compact = true,
                    showLabel = false,
                    modifier = Modifier.weight(1f).height(if (compact) 48.dp else 64.dp),
                    onClick = onDecrease,
                )
                Spacer(Modifier.width(8.dp))
                B02ControlTile(
                    Icons.Rounded.Add,
                    "Increase $label temperature",
                    compact = true,
                    showLabel = false,
                    modifier = Modifier.weight(1f).height(if (compact) 48.dp else 64.dp),
                    onClick = onIncrease,
                )
            }
            val directions = zone.availableFanDirections.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 10.dp)) {
                directions.forEach { direction ->
                    B02ControlTile(
                        icon = airflowIcon(direction),
                        label = directionLabel(direction),
                        selected = zone.fanDirection == direction,
                        compact = compact,
                        showLabel = false,
                        modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 76.dp),
                        onClick = { onDirection(direction) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FanControl(zone: ClimateZoneControlState, compact: Boolean, onChange: (Int) -> Unit) {
    SystemUiGlassPanel(
        modifier = Modifier.fillMaxWidth().height(if (compact) 50.dp else 62.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Air, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Fan ${zone.fanSpeed}", modifier = Modifier.padding(horizontal = 8.dp))
            B02MiniButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Decrease fan") {
                onChange((zone.fanSpeed - 1).coerceAtLeast(zone.minimumFanSpeed))
            }
            SegmentedLevel(
                level = zone.fanSpeed - zone.minimumFanSpeed,
                maximum = (zone.maximumFanSpeed - zone.minimumFanSpeed).coerceAtLeast(1),
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            )
            B02MiniButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Increase fan") {
                onChange((zone.fanSpeed + 1).coerceAtMost(zone.maximumFanSpeed))
            }
        }
    }
}

@Composable
private fun CabinAirflowCanvas(
    powerOn: Boolean,
    driverDirection: Int,
    passengerDirection: Int,
    driverFanStrength: Float,
    passengerFanStrength: Float,
    modifier: Modifier,
) {
    SystemUiGlassPanel(modifier, RoundedCornerShape(26.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .semantics { contentDescription = "Cabin airflow" },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.climate_cabin_top_down),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 2.dp)
                    .graphicsLayer(alpha = if (powerOn) 1f else 0.48f),
            )
            Canvas(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
                fun ribbon(startX: Float, centerX: Float, direction: Int, strength: Float) {
                    val normalizedStrength = strength.coerceIn(0.12f, 1f)
                    val active = Color(0xFFB99CFF).copy(
                        alpha = if (powerOn) 0.38f + normalizedStrength * 0.5f else 0.12f,
                    )
                    val glow = Color(0xFFA98CF5).copy(
                        alpha = if (powerOn) 0.08f + normalizedStrength * 0.18f else 0.04f,
                    )
                    val faceY = size.height * 0.48f
                    val feetY = size.height * 0.73f
                    val targetY = if (direction == ClimateFanDirection.FEET) feetY else faceY
                    repeat(3) { index ->
                        val spread = (index - 1) * size.width * 0.018f
                        val path = Path().apply {
                            moveTo(startX + spread, size.height * 0.16f)
                            cubicTo(
                                startX + spread * 1.5f,
                                size.height * 0.27f,
                                centerX - spread,
                                targetY * 0.74f,
                                centerX + spread,
                                targetY,
                            )
                        }
                        drawPath(path, glow, style = Stroke(width = 10f + normalizedStrength * 7f, cap = StrokeCap.Round))
                        drawPath(path, active, style = Stroke(width = 2.5f + normalizedStrength * 2f, cap = StrokeCap.Round))
                    }
                    if (direction == ClimateFanDirection.FACE_AND_FEET) {
                        repeat(3) { index ->
                            val spread = (index - 1) * size.width * 0.018f
                            val lower = Path().apply {
                                moveTo(centerX + spread, faceY)
                                cubicTo(centerX + spread, size.height * 0.58f, centerX - spread, size.height * 0.66f, centerX + spread, feetY)
                            }
                            drawPath(lower, glow, style = Stroke(width = 12f, cap = StrokeCap.Round))
                            drawPath(lower, active, style = Stroke(width = 3f, cap = StrokeCap.Round))
                        }
                    }
                }
                fun rearRibbon(centerX: Float, strength: Float) {
                    val normalizedStrength = strength.coerceIn(0.12f, 1f)
                    val active = Color(0xFFB99CFF).copy(
                        alpha = if (powerOn) 0.30f + normalizedStrength * 0.42f else 0.10f,
                    )
                    repeat(3) { index ->
                        val spread = (index - 1) * size.width * 0.016f
                        val path = Path().apply {
                            moveTo(centerX + spread, size.height * 0.52f)
                            cubicTo(
                                centerX - spread,
                                size.height * 0.61f,
                                centerX + spread,
                                size.height * 0.70f,
                                centerX + spread,
                                size.height * 0.80f,
                            )
                        }
                        drawPath(path, active, style = Stroke(width = 2f + normalizedStrength * 2f, cap = StrokeCap.Round))
                    }
                }
                ribbon(size.width * 0.23f, size.width * 0.33f, driverDirection, driverFanStrength)
                ribbon(size.width * 0.47f, size.width * 0.40f, driverDirection, driverFanStrength)
                ribbon(size.width * 0.53f, size.width * 0.60f, passengerDirection, passengerFanStrength)
                ribbon(size.width * 0.77f, size.width * 0.67f, passengerDirection, passengerFanStrength)
                rearRibbon(size.width * 0.35f, driverFanStrength)
                rearRibbon(size.width * 0.65f, passengerFanStrength)
            }
        }
    }
}

@Composable
private fun UtilityDeck(
    state: ControlCenterUiState,
    compact: Boolean,
    onBrightnessChanged: (Float) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onQuickControlChanged: (Int, Boolean) -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenWirelessSettings: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenCamera: () -> Unit,
    onScreenOff: () -> Unit,
    modifier: Modifier,
) {
    val quick = state.extendedControls.quickControls
    SystemUiGlassPanel(modifier, RoundedCornerShape(26.dp)) {
        Column(
            Modifier.fillMaxSize().padding(if (compact) 9.dp else 13.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
        ) {
            HorizontalControl(
                Icons.Rounded.BrightnessHigh,
                stringResource(R.string.control_brightness),
                state.displayedBrightness,
                state.brightness.available,
                onBrightnessChanged,
                onBrightnessChangeFinished,
                compact,
            )
            HorizontalControl(
                Icons.AutoMirrored.Rounded.VolumeUp,
                stringResource(R.string.control_volume),
                state.audio.progress,
                state.audio.available,
                onVolumeChanged,
                {},
                compact,
            )
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickTile(Icons.Rounded.Wifi, "Wi-Fi", quick.wifiEnabled, compact, Modifier.weight(1f), {
                        onQuickControlChanged(QuickControl.WIFI, !quick.wifiEnabled)
                    }, onOpenWifiSettings)
                    QuickTile(Icons.Rounded.WifiTethering, "Hotspot", quick.hotspotEnabled, compact, Modifier.weight(1f), {
                        onQuickControlChanged(QuickControl.HOTSPOT, !quick.hotspotEnabled)
                    }, onOpenWirelessSettings)
                    QuickTile(Icons.Rounded.LockPerson, "Valet Mode", quick.valetModeEnabled, compact, Modifier.weight(1f), {
                        onQuickControlChanged(QuickControl.VALET_MODE, !quick.valetModeEnabled)
                    })
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickTile(Icons.Rounded.Bluetooth, "Bluetooth", quick.bluetoothEnabled, compact, Modifier.weight(1f), {
                        onQuickControlChanged(QuickControl.BLUETOOTH, !quick.bluetoothEnabled)
                    }, onOpenBluetoothSettings)
                    QuickTile(Icons.Rounded.CameraAlt, "Camera", false, compact, Modifier.weight(1f), {
                        onOpenCamera()
                    })
                    QuickTile(Icons.Rounded.ScreenLockLandscape, "Screen Off", false, compact, Modifier.weight(1f), onScreenOff)
                }
            }
        }
    }
}

@Composable
private fun HorizontalControl(
    icon: ImageVector,
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
    compact: Boolean,
) {
    SystemUiGlassPanel(
        Modifier.fillMaxWidth().height(if (compact) 64.dp else 82.dp).alpha(if (enabled) 1f else 0.45f),
        RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(label, fontSize = if (compact) 12.sp else 15.sp)
                Slider(value.coerceIn(0f, 1f), onValueChange, enabled = enabled, onValueChangeFinished = onFinished)
            }
        }
    }
}

@Composable
private fun SeatDeck(
    state: ControlCenterUiState,
    compact: Boolean,
    onSeatHeatingChanged: (ClimateZone, Int) -> Unit,
    onSeatVentilationChanged: (ClimateZone, Int) -> Unit,
) {
    val climate = state.extendedControls.climate
    SystemUiGlassPanel(
        Modifier.fillMaxWidth().height(if (compact) 58.dp else 96.dp),
        RoundedCornerShape(24.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = if (compact) 12.dp else 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeatControls(
                "Driver Seat",
                ClimateZone.LEFT,
                climate.driverZone,
                compact,
                onSeatHeatingChanged,
                onSeatVentilationChanged,
                Modifier.weight(1f),
            )
            Spacer(Modifier.width(if (compact) 10.dp else 28.dp))
            Box(Modifier.width(1.dp).fillMaxHeight(0.55f).background(SystemUiGlassBorder))
            Spacer(Modifier.width(if (compact) 10.dp else 28.dp))
            SeatControls(
                "Passenger Seat",
                ClimateZone.RIGHT,
                climate.passengerZone,
                compact,
                onSeatHeatingChanged,
                onSeatVentilationChanged,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SeatControls(
    label: String,
    zone: ClimateZone,
    state: ClimateZoneControlState,
    compact: Boolean,
    onHeat: (ClimateZone, Int) -> Unit,
    onVent: (ClimateZone, Int) -> Unit,
    modifier: Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)) {
        Text(
            label,
            modifier = Modifier.width(if (compact) 96.dp else 145.dp).padding(start = if (compact) 0.dp else 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = if (compact) 12.sp else 15.sp,
        )
        LevelTile(Icons.Rounded.EventSeat, "${label} heating", state.seatHeatingLevel, state.maximumSeatHeatingLevel, SeatHeat, compact) {
            onHeat(zone, nextLevel(state.seatHeatingLevel, state.maximumSeatHeatingLevel))
        }
        LevelTile(Icons.Rounded.Air, "${label} ventilation", state.seatVentilationLevel, state.maximumSeatVentilationLevel, MaterialTheme.colorScheme.primary, compact) {
            onVent(zone, nextLevel(state.seatVentilationLevel, state.maximumSeatVentilationLevel))
        }
    }
}

@Composable
private fun LevelTile(icon: ImageVector, label: String, level: Int, maximum: Int, color: Color, compact: Boolean, onClick: () -> Unit) {
    B02Surface(
        modifier = Modifier
            .height(if (compact) 42.dp else 64.dp)
            .width(if (compact) 104.dp else 190.dp),
        selected = level > 0,
        label = label,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(if (compact) 22.dp else 30.dp))
            SegmentedLevel(level, maximum.coerceAtLeast(1), Modifier.weight(1f).padding(start = 10.dp), color)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ClimateToggle(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    B02ControlTile(icon, label, selected, compact, true, Modifier.weight(1f).fillMaxHeight(), onClick = onClick)
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    compact: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    B02ControlTile(icon, label, selected, compact, true, modifier, onClick, onLongClick)
}

@Composable
private fun B02ControlTile(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    compact: Boolean = false,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val defaultSize = if (compact) 48.dp else 58.dp
    B02Surface(
        modifier = modifier.then(if (modifier == Modifier) Modifier.size(defaultSize) else Modifier),
        selected = selected,
        label = label,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column(
            Modifier.fillMaxSize().padding(if (compact) 5.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE4E8EB),
                modifier = Modifier.size(if (compact) 24.dp else 34.dp),
            )
            if (showLabel) {
                Text(label, fontSize = if (compact) 9.sp else 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) Box(Modifier.padding(top = 3.dp).width(22.dp).height(3.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun B02Surface(
    modifier: Modifier,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier
            .shadow(5.dp, shape, ambientColor = SystemUiGlassShadow, spotColor = SystemUiGlassShadow)
            .clip(shape)
            .background(B02MetalBrush)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(0.22f), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = label },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            var y = -size.width
            while (y < size.height) {
                drawLine(Color.White.copy(alpha = 0.025f), Offset(0f, y), Offset(size.width, y + size.width), 0.6f)
                y += 7f
            }
        }
        content()
    }
}

@Composable
private fun B02MiniButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    B02ControlTile(icon, label, compact = true, showLabel = false, modifier = Modifier.size(42.dp), onClick = onClick)
}

@Composable
private fun SegmentedLevel(level: Int, maximum: Int, modifier: Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(maximum) { index ->
            Box(Modifier.weight(1f).height(5.dp).background(if (index < level) color else Color.White.copy(0.14f), RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun MoreClimateDialog(
    state: ControlCenterUiState,
    onDismiss: () -> Unit,
    onMaxAcChanged: (Boolean) -> Unit,
    onMaxDefrostChanged: (Boolean) -> Unit,
    onAutoRecirculationChanged: (Boolean) -> Unit,
    onSteeringWheelHeatChanged: (Int) -> Unit,
    onTemperatureUnitChanged: (Int) -> Unit,
) {
    val climate = state.extendedControls.climate
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(4f)
            .background(Color.Black.copy(alpha = 0.62f))
            .combinedClickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        SystemUiGlassPanel(
            modifier = Modifier
                .widthIn(max = 920.dp)
                .fillMaxWidth(0.72f)
                .combinedClickable(onClick = {}),
            shape = RoundedCornerShape(28.dp),
            raised = true,
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.more_climate),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    B02ControlTile(Icons.Rounded.Close, "Close more climate", compact = true, showLabel = false, onClick = onDismiss)
                }
                Row(Modifier.fillMaxWidth().height(118.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClimateToggle(Icons.Rounded.AcUnit, "MAX A/C", climate.maxAcOn, false) { onMaxAcChanged(!climate.maxAcOn) }
                    ClimateToggle(Icons.Rounded.SevereCold, "MAX Defrost", climate.maxDefrostOn, false) { onMaxDefrostChanged(!climate.maxDefrostOn) }
                    ClimateToggle(Icons.Rounded.DirectionsCar, "Auto recirculation", climate.autoRecirculationOn, false) { onAutoRecirculationChanged(!climate.autoRecirculationOn) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Steering wheel heat", modifier = Modifier.weight(1f), fontSize = 16.sp)
                    LevelTile(Icons.Rounded.Thermostat, "Steering wheel heat", climate.steeringWheelHeatLevel, climate.maximumSteeringWheelHeatLevel, SeatHeat, false) {
                        onSteeringWheelHeatChanged(nextLevel(climate.steeringWheelHeatLevel, climate.maximumSteeringWheelHeatLevel))
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Temperature units", modifier = Modifier.weight(1f), fontSize = 16.sp)
                    B02ControlTile(
                        Icons.Rounded.Thermostat,
                        if (climate.temperatureUnit == TemperatureUnit.CELSIUS) "Celsius" else "Fahrenheit",
                        selected = true,
                        modifier = Modifier.width(180.dp).height(64.dp),
                        onClick = {
                            onTemperatureUnitChanged(if (climate.temperatureUnit == TemperatureUnit.CELSIUS) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MockCameraOverlay(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().zIndex(5f).background(Color(0xFA141019)).combinedClickable(onClick = onDismiss)) {
        Column(Modifier.fillMaxSize().padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("360° Camera", fontSize = 28.sp, modifier = Modifier.weight(1f))
                B02ControlTile(Icons.Rounded.Close, "Close camera", compact = true, showLabel = false, onClick = onDismiss)
            }
            Canvas(Modifier.fillMaxSize().padding(52.dp).semantics { contentDescription = "Mock 360 degree camera view" }) {
                drawCircle(Color(0xFFA98CF5).copy(0.12f), size.minDimension * 0.42f, center)
                drawCircle(Color(0xFFA98CF5).copy(0.5f), size.minDimension * 0.42f, center, style = Stroke(4f))
                drawRoundRect(Color(0xFF6F5E88), Offset(size.width * 0.39f, size.height * 0.2f), androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height * 0.6f), androidx.compose.ui.geometry.CornerRadius(50f, 50f))
                repeat(8) { index ->
                    val x = if (index % 2 == 0) size.width * 0.32f else size.width * 0.68f
                    val y = size.height * (0.18f + (index / 2) * 0.21f)
                    drawCircle(if (index < 2) SeatHeat else Color(0xFFA98CF5), 7f, Offset(x, y))
                }
            }
        }
    }
}

@Composable
private fun ScreenCurtain(onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().zIndex(10f).background(Color.Black).combinedClickable(onClick = onDismiss)
            .semantics { contentDescription = "Screen off. Tap to wake" },
        contentAlignment = Alignment.Center,
    ) {
        Text("Tap to wake", color = Color.White.copy(0.18f))
    }
}

private fun airflowIcon(direction: Int): ImageVector = when (direction) {
    ClimateFanDirection.FEET -> Icons.Rounded.Air
    ClimateFanDirection.FACE_AND_FEET -> Icons.Rounded.Sync
    ClimateFanDirection.DEFROST -> Icons.Rounded.SevereCold
    else -> Icons.Rounded.Air
}

private fun directionLabel(direction: Int): String = when (direction) {
    ClimateFanDirection.FEET -> "Feet airflow"
    ClimateFanDirection.FACE_AND_FEET -> "Face and feet airflow"
    ClimateFanDirection.DEFROST -> "Windshield airflow"
    else -> "Face airflow"
}

private fun nextLevel(level: Int, maximum: Int): Int = if (level >= maximum) 0 else level + 1

private fun formatTemperature(value: Float): String = String.format(Locale.ENGLISH, "%.1f°C", value)

private val B02MetalBrush = Brush.linearGradient(
    listOf(Color(0xFF4A4F55), Color(0xFF30353A), Color(0xFF25292E), Color(0xFF15181C)),
)
private val SeatHeat = Color(0xFFF0A8D8)
