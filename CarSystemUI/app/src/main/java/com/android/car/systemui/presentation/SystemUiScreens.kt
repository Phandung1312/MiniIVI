package com.android.car.systemui.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.android.car.systemui.R
import com.android.car.systemui.domain.model.AudioState
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.HvacState
import com.android.car.systemui.domain.model.QuickControl
import com.android.car.systemui.domain.model.QuickControlsState
import com.android.car.systemui.domain.model.TemperatureZone
import com.android.car.systemui.presentation.model.ControlCenterUiState
import com.android.car.systemui.presentation.model.NavigationDestination
import java.util.Locale

@Composable
fun NavigationRailScreen(
    controlCenterVisible: Boolean,
    onHome: () -> Unit,
    onAppList: () -> Unit,
    onControlCenter: () -> Unit,
    onSettings: () -> Unit,
    selectedDestination: NavigationDestination = NavigationDestination.HOME,
    quickControls: QuickControlsState = QuickControlsState(),
    audio: AudioState = AudioState(),
    onPhone: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemUiRailBrush)
            .drawBehind {
                drawLine(
                    color = SystemUiGlassBorder,
                    start = Offset(size.width - 0.5f, 0f),
                    end = Offset(size.width - 0.5f, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        BoxWithConstraints {
            val buttonSize = when {
                maxHeight < 600.dp -> 72.8.dp
                maxHeight < 760.dp -> 83.2.dp
                else -> 93.6.dp
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NavigationBrandMark(buttonSize)
                NavigationButton(
                    icon = null,
                    iconPainter = painterResource(R.drawable.ic_navigation_home),
                    description = R.string.navigation_home,
                    size = buttonSize,
                    selected = selectedDestination == NavigationDestination.HOME,
                    tag = "navigation_home",
                    onClick = onHome,
                )
                NavigationButton(
                    icon = null,
                    iconPainter = painterResource(R.drawable.ic_navigation_apps),
                    description = R.string.navigation_app_list,
                    size = buttonSize,
                    selected = selectedDestination == NavigationDestination.APP_LIST,
                    tag = "navigation_apps",
                    onClick = onAppList,
                )
                StatusPanel(
                    state = quickControls,
                    audio = audio,
                    buttonSize = buttonSize,
                    modifier = Modifier.width(buttonSize).weight(1f),
                )
                NavigationButton(
                    icon = null,
                    iconPainter = painterResource(R.drawable.ic_navigation_phone),
                    description = R.string.navigation_call,
                    size = buttonSize,
                    selected = selectedDestination == NavigationDestination.PHONE,
                    tag = "navigation_call",
                    onClick = onPhone,
                )
                NavigationButton(
                    icon = null,
                    iconPainter = painterResource(R.drawable.ic_vehicle_front_simplified),
                    description = R.string.navigation_control_center,
                    size = buttonSize,
                    selected = selectedDestination == NavigationDestination.CONTROL_CENTER,
                    tag = "navigation_vehicle_controls",
                    onClick = onControlCenter,
                )
                NavigationButton(
                    icon = null,
                    iconPainter = painterResource(R.drawable.ic_navigation_settings),
                    description = R.string.navigation_settings,
                    size = buttonSize,
                    selected = selectedDestination == NavigationDestination.SETTINGS,
                    tag = "navigation_settings",
                    onClick = onSettings,
                )
            }
        }
    }
}

@Composable
private fun NavigationBrandMark(size: Dp) {
    val label = stringResource(R.string.navigation_brand)
    SystemUiGlassPanel(
        modifier = Modifier
            .size(size)
            .testTag("navigation_brand")
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(22.dp),
        raised = true,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "IVI",
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (size >= 83.2.dp) 28.6.sp else 23.4.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun NavigationButton(
    icon: ImageVector?,
    description: Int,
    size: Dp,
    selected: Boolean = false,
    tag: String,
    onClick: () -> Unit,
    iconPainter: Painter? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    val background = if (selected) MaterialTheme.colorScheme.primary
    else SystemUiGlassSurface.copy(alpha = 0.34f)
    Surface(
        modifier = Modifier
            .size(size)
            .testTag(tag)
            .semantics { this.selected = selected },
        shape = shape,
        color = background,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            val tint = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
            val iconModifier = Modifier.size(size * 0.48f)
            when {
                iconPainter != null -> Icon(
                    painter = iconPainter,
                    contentDescription = stringResource(description),
                    tint = tint,
                    modifier = iconModifier,
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = stringResource(description),
                    tint = tint,
                    modifier = iconModifier,
                )
            }
        }
    }
}

@Composable
private fun StatusPanel(
    state: QuickControlsState,
    audio: AudioState,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
) {
    val wifiAvailable = state.available && QuickControl.WIFI in state.capabilities
    val bluetoothAvailable = state.available && QuickControl.BLUETOOTH in state.capabilities
    val showMute = audio.available && audio.volume == 0
    BoxWithConstraints(
        modifier = modifier
            .width(buttonSize)
            .testTag("navigation_status_panel"),
    ) {
        val dense = maxHeight < 110.dp
        SystemUiGlassPanel(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (dense) 5.dp else 8.dp, vertical = if (dense) 4.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(if (dense) 3.dp else 8.dp),
            ) {
                StatusPanelItem(
                    icon = if (state.wifiEnabled) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                    label = stringResource(R.string.navigation_wifi),
                    enabled = state.wifiEnabled,
                    connected = state.wifiConnected,
                    available = wifiAvailable,
                    dense = dense,
                    tag = "navigation_wifi",
                    modifier = Modifier.weight(1f),
                )
                StatusPanelItem(
                    icon = if (state.bluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                    label = stringResource(R.string.navigation_bluetooth),
                    enabled = state.bluetoothEnabled,
                    connected = state.bluetoothConnected,
                    available = bluetoothAvailable,
                    dense = dense,
                    tag = "navigation_bluetooth",
                    modifier = Modifier.weight(1f),
                )
                if (showMute) {
                    StatusPanelItem(
                        icon = Icons.Rounded.VolumeOff,
                        label = stringResource(R.string.navigation_audio_muted),
                        enabled = true,
                        connected = false,
                        available = true,
                        dense = dense,
                        tag = "navigation_audio_mute",
                        disabledVisual = true,
                        descriptionOverride = stringResource(R.string.navigation_audio_muted),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPanelItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    connected: Boolean,
    available: Boolean,
    dense: Boolean,
    tag: String,
    disabledVisual: Boolean = false,
    descriptionOverride: String? = null,
    modifier: Modifier = Modifier,
) {
    val status = when {
        !available -> stringResource(R.string.navigation_unavailable)
        !enabled -> stringResource(R.string.navigation_disabled)
        connected -> stringResource(R.string.navigation_connected)
        else -> stringResource(R.string.navigation_not_connected)
    }
    val description = descriptionOverride
        ?: stringResource(R.string.navigation_connectivity_description, label, status)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !available -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    !enabled || disabledVisual -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(if (dense) 22.dp else 30.dp),
            )
            if (connected && enabled && available) {
                Box(
                    modifier = Modifier
                        .size(if (dense) 6.dp else 9.dp)
                        .align(Alignment.Center)
                        .offset(
                            x = if (dense) 8.dp else 11.dp,
                            y = if (dense) 8.dp else 11.dp,
                        )
                        .background(Color(0xFF7BE2A1), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun LegacyControlCenterOverlay(
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
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180)),
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.90f))
                .clickable(onClick = onDismiss),
        ) {
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f),
            ) {
                SystemUiGlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.94f)
                        .padding(20.dp),
                    shape = RoundedCornerShape(28.dp),
                    raised = true,
                    onClick = {},
                ) {
                    ControlCenterContent(
                        state = state,
                        onDismiss = onDismiss,
                        onBrightnessChanged = onBrightnessChanged,
                        onBrightnessChangeFinished = onBrightnessChangeFinished,
                        onVolumeChanged = onVolumeChanged,
                        onTemperatureDecrease = onTemperatureDecrease,
                        onTemperatureIncrease = onTemperatureIncrease,
                        onAcChanged = onAcChanged,
                        onSettings = onSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlCenterContent(
    state: ControlCenterUiState,
    onDismiss: () -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onTemperatureDecrease: (ClimateZone) -> Unit,
    onTemperatureIncrease: (ClimateZone) -> Unit,
    onAcChanged: (Boolean) -> Unit,
    onSettings: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        val errorMessage = listOfNotNull(
            state.brightness.errorMessage,
            state.audio.errorMessage,
            state.hvac.errorMessage,
        ).firstOrNull()
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.control_center_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.navigation_settings))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.control_close))
                }
            }
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                if (maxWidth >= 720.dp) {
                    val controlBodyHeight = (maxHeight - SLIDER_LABEL_SPACE).coerceIn(140.dp, 280.dp)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        ControlCenterSliders(
                            state = state,
                            controlBodyHeight = controlBodyHeight,
                            onBrightnessChanged = onBrightnessChanged,
                            onBrightnessChangeFinished = onBrightnessChangeFinished,
                            onVolumeChanged = onVolumeChanged,
                        )
                        HvacCard(
                            state = state.hvac,
                            onTemperatureDecrease = onTemperatureDecrease,
                            onTemperatureIncrease = onTemperatureIncrease,
                            onAcChanged = onAcChanged,
                            controlBodyHeight = controlBodyHeight,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    val controlBodyHeight = (
                        (maxHeight - SLIDER_LABEL_SPACE - NARROW_LAYOUT_GAP) / 2
                        ).coerceIn(112.dp, 180.dp)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ControlCenterSliders(
                            state = state,
                            controlBodyHeight = controlBodyHeight,
                            onBrightnessChanged = onBrightnessChanged,
                            onBrightnessChangeFinished = onBrightnessChangeFinished,
                            onVolumeChanged = onVolumeChanged,
                        )
                        HvacCard(
                            state = state.hvac,
                            onTemperatureDecrease = onTemperatureDecrease,
                            onTemperatureIncrease = onTemperatureIncrease,
                            onAcChanged = onAcChanged,
                            controlBodyHeight = controlBodyHeight,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            errorMessage?.let { message ->
                Spacer(Modifier.height(4.dp))
                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ControlCenterSliders(
    state: ControlCenterUiState,
    controlBodyHeight: Dp,
    onBrightnessChanged: (Float) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally)) {
        VerticalControl(
            value = state.displayedBrightness,
            enabled = state.brightness.available,
            icon = Icons.Rounded.BrightnessHigh,
            label = stringResource(R.string.control_brightness),
            activeColor = Color(0xFFE8E0F5),
            activeContentColor = Color(0xFF201A29),
            onValueChange = onBrightnessChanged,
            onValueChangeFinished = onBrightnessChangeFinished,
            controlHeight = controlBodyHeight,
            modifier = Modifier.testTag("brightness_slider"),
        )
        VerticalControl(
            value = state.audio.progress,
            enabled = state.audio.available,
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = stringResource(R.string.control_volume),
            activeColor = MaterialTheme.colorScheme.primary,
            activeContentColor = Color.White,
            onValueChange = onVolumeChanged,
            controlHeight = controlBodyHeight,
            modifier = Modifier.testTag("volume_slider"),
        )
    }
}

@Composable
private fun VerticalControl(
    value: Float,
    enabled: Boolean,
    icon: ImageVector,
    label: String,
    activeColor: Color,
    activeContentColor: Color,
    onValueChange: (Float) -> Unit,
    controlHeight: Dp,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
) {
    var heightPx by remember { mutableIntStateOf(1) }
    val normalized = value.coerceIn(0f, 1f)
    fun updateFromY(y: Float) {
        onValueChange((1f - y / heightPx).coerceIn(0f, 1f))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .width(108.dp)
                .height(controlHeight)
                .alpha(if (enabled) 1f else 0.42f)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(42.dp),
                    ambientColor = SystemUiGlassShadow,
                    spotColor = SystemUiGlassShadow,
                )
                .clip(RoundedCornerShape(42.dp))
                .background(CONTROL_CARD_COLOR)
                .border(1.dp, CARD_BORDER_COLOR, RoundedCornerShape(42.dp))
                .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(normalized, 0f..1f)
                    setProgress { target ->
                        if (enabled) onValueChange(target.coerceIn(0f, 1f))
                        enabled
                    }
                }
                .then(
                    if (enabled) Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            updateFromY(down.position.y)
                            var pressed = true
                            while (pressed) {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()
                                updateFromY(change.position.y)
                                pressed = change.pressed
                                change.consume()
                            }
                            onValueChangeFinished()
                        }
                    } else Modifier,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(normalized)
                    .align(Alignment.BottomCenter)
                    .background(activeColor),
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (normalized > 0.78f) activeContentColor
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun HvacCard(
    state: HvacState,
    onTemperatureDecrease: (ClimateZone) -> Unit,
    onTemperatureIncrease: (ClimateZone) -> Unit,
    onAcChanged: (Boolean) -> Unit,
    controlBodyHeight: Dp,
    modifier: Modifier = Modifier,
) {
    SystemUiGlassPanel(
        modifier = modifier
            .height(controlBodyHeight)
            .testTag("hvac_card"),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AcUnit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("HVAC", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(
                        text = when {
                            state.connecting -> stringResource(R.string.climate_waiting)
                            !state.available -> stringResource(R.string.climate_unavailable)
                            else -> stringResource(
                                R.string.climate_cabin_value,
                                formatTemperature(state.cabinTemperature),
                            )
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        fontSize = 13.sp,
                    )
                }
                Text(stringResource(R.string.climate_ac), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = state.acOn,
                    onCheckedChange = onAcChanged,
                    enabled = state.acAvailable,
                )
            }
            if (state.available) {
                Spacer(Modifier.height(12.dp))
                if (state.dualZone) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TemperatureControl(
                            label = stringResource(R.string.climate_left),
                            zone = state.leftZone,
                            onDecrease = { onTemperatureDecrease(ClimateZone.LEFT) },
                            onIncrease = { onTemperatureIncrease(ClimateZone.LEFT) },
                            modifier = Modifier.weight(1f),
                        )
                        TemperatureControl(
                            label = stringResource(R.string.climate_right),
                            zone = state.rightZone,
                            onDecrease = { onTemperatureDecrease(ClimateZone.RIGHT) },
                            onIncrease = { onTemperatureIncrease(ClimateZone.RIGHT) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    TemperatureControl(
                        label = stringResource(R.string.climate_single_zone),
                        zone = state.leftZone,
                        onDecrease = { onTemperatureDecrease(ClimateZone.LEFT) },
                        onIncrease = { onTemperatureIncrease(ClimateZone.LEFT) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TemperatureControl(
    label: String,
    zone: TemperatureZone?,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SystemUiGlassPanel(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(formatTemperature(zone?.temperature), fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClimateButton(Icons.Rounded.Remove, R.string.temperature_decrease, zone != null, onDecrease)
                ClimateButton(Icons.Rounded.Add, R.string.temperature_increase, zone != null, onIncrease)
            }
        }
    }
}

@Composable
private fun ClimateButton(icon: ImageVector, description: Int, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = BUTTON_CARD_COLOR,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, stringResource(description), modifier = Modifier.size(26.dp))
        }
    }
}

private fun formatTemperature(value: Float?): String =
    value?.let { String.format(Locale.getDefault(), "%.1f\u00B0C", it) } ?: "--\u00B0C"

private val CONTROL_CARD_COLOR = SystemUiGlassSurface
private val BUTTON_CARD_COLOR = Color(0x80594B6D)
private val CARD_BORDER_COLOR = SystemUiGlassBorder
private val SLIDER_LABEL_SPACE = 34.dp
private val NARROW_LAYOUT_GAP = 8.dp
