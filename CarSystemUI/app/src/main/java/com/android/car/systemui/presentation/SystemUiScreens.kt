package com.android.car.systemui.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.android.car.systemui.R
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.model.TemperatureZone
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BottomNavigationScreen(
    quickControlVisible: Boolean,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onAppList: () -> Unit,
    onQuickControl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xF20A0D11),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        BoxWithConstraints {
            val buttonSize = when {
                maxWidth < 480.dp -> 68.dp
                maxWidth < 720.dp -> 84.dp
                else -> 104.dp
            }
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                NavigationButton(
                    Icons.Rounded.Home,
                    R.string.navigation_home,
                    buttonSize,
                    onClick = onHome,
                )
                NavigationButton(
                    Icons.Rounded.Settings,
                    R.string.navigation_settings,
                    buttonSize,
                    onClick = onSettings,
                )
                NavigationButton(
                    Icons.Rounded.Apps,
                    R.string.navigation_app_list,
                    buttonSize,
                    onClick = onAppList,
                )
                NavigationButton(
                    Icons.Rounded.Tune,
                    R.string.navigation_control_center,
                    buttonSize,
                    selected = quickControlVisible,
                    onClick = onQuickControl,
                )
            }
        }
    }
}

@Composable
private fun NavigationButton(
    icon: ImageVector,
    description: Int,
    size: Dp,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
    else Color.White.copy(alpha = 0.06f)
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = background,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(description),
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

@Composable
fun QuickControlOverlay(
    visible: Boolean,
    state: QuickControlUiState,
    onDismiss: () -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onTemperatureDecrease: (ClimateZone) -> Unit,
    onTemperatureIncrease: (ClimateZone) -> Unit,
    onAcChanged: (Boolean) -> Unit,
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
                .background(Color.Black.copy(alpha = 0.46f))
                .clickable(onClick = onDismiss),
        ) {
            val wide = maxWidth >= 840.dp
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(if (wide) Alignment.CenterEnd else Alignment.Center),
                enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f),
                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f),
            ) {
                Surface(
                    modifier = Modifier
                        .then(if (wide) Modifier.width(560.dp) else Modifier.fillMaxWidth())
                        .fillMaxHeight(if (wide) 0.90f else 0.94f)
                        .padding(20.dp)
                        .clickable(enabled = true, onClick = {}),
                    shape = RoundedCornerShape(36.dp),
                    color = Color(0xE6222831),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    shadowElevation = 24.dp,
                ) {
                    QuickControlContent(
                        state = state,
                        onDismiss = onDismiss,
                        onBrightnessChanged = onBrightnessChanged,
                        onBrightnessChangeFinished = onBrightnessChangeFinished,
                        onVolumeChanged = onVolumeChanged,
                        onTemperatureDecrease = onTemperatureDecrease,
                        onTemperatureIncrease = onTemperatureIncrease,
                        onAcChanged = onAcChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickControlContent(
    state: QuickControlUiState,
    onDismiss: () -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onBrightnessChangeFinished: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onTemperatureDecrease: (ClimateZone) -> Unit,
    onTemperatureIncrease: (ClimateZone) -> Unit,
    onAcChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.control_center_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.control_close))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
        ) {
            VerticalControl(
                value = state.displayedBrightness,
                enabled = state.brightness.available,
                icon = Icons.Rounded.BrightnessHigh,
                label = stringResource(R.string.control_brightness),
                activeColor = Color(0xFFF7F4E8),
                activeContentColor = Color(0xFF272A2E),
                onValueChange = onBrightnessChanged,
                onValueChangeFinished = onBrightnessChangeFinished,
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
                modifier = Modifier.testTag("volume_slider"),
            )
        }
        Spacer(Modifier.height(22.dp))
        HvacCard(
            state = state.hvac,
            onTemperatureDecrease = onTemperatureDecrease,
            onTemperatureIncrease = onTemperatureIncrease,
            onAcChanged = onAcChanged,
        )
        listOfNotNull(
            state.brightness.errorMessage,
            state.audio.errorMessage,
            state.hvac.errorMessage,
        ).firstOrNull()?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
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
                .height(250.dp)
                .alpha(if (enabled) 1f else 0.42f)
                .clip(RoundedCornerShape(42.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(42.dp))
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
                    tint = if (normalized > 0.78f) activeContentColor else Color.White,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(R.string.control_percentage, (normalized * 100).roundToInt()),
                    color = if (normalized > 0.22f) activeContentColor else Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp)
    }
}

@Composable
private fun HvacCard(
    state: HvacState,
    onTemperatureDecrease: (ClimateZone) -> Unit,
    onTemperatureIncrease: (ClimateZone) -> Unit,
    onAcChanged: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = Color(0xFF62B0FF))
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
                        color = Color.White.copy(alpha = 0.70f),
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
                Spacer(Modifier.height(20.dp))
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
    Surface(modifier = modifier, color = Color.Black.copy(alpha = 0.18f), shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = Color.White.copy(alpha = 0.68f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(formatTemperature(zone?.temperature), fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Spacer(Modifier.height(10.dp))
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
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.10f),
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, stringResource(description), modifier = Modifier.size(26.dp))
        }
    }
}

private fun formatTemperature(value: Float?): String =
    value?.let { String.format(Locale.getDefault(), "%.1f°C", it) } ?: "--°C"
