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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
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
fun NavigationRailScreen(
    controlCenterVisible: Boolean,
    onHome: () -> Unit,
    onAppList: () -> Unit,
    onControlCenter: () -> Unit,
    onSettings: () -> Unit,
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
                    MiniIviNavigationIcons.Home,
                    R.string.navigation_home,
                    buttonSize,
                    onClick = onHome,
                )
                NavigationButton(
                    MiniIviNavigationIcons.Apps,
                    R.string.navigation_app_list,
                    buttonSize,
                    onClick = onAppList,
                )
                Spacer(Modifier.weight(1f))
                NavigationButton(
                    MiniIviNavigationIcons.ControlCenter,
                    R.string.navigation_control_center,
                    buttonSize,
                    selected = controlCenterVisible,
                    onClick = onControlCenter,
                )
                NavigationButton(
                    Icons.Rounded.Settings,
                    R.string.navigation_settings,
                    buttonSize,
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
    icon: ImageVector,
    description: Int,
    size: Dp,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    else SystemUiGlassSurface.copy(alpha = 0.34f)
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(22.dp),
        color = background,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(description),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(size * 0.48f),
            )
        }
    }
}

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
                Text(
                    text = stringResource(R.string.control_percentage, (normalized * 100).roundToInt()),
                    color = if (normalized > 0.22f) activeContentColor
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
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
