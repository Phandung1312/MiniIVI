package com.android.car.launcher.feature.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.TireRepair
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.car.launcher.R
import com.android.car.launcher.core.ui.MiniIviCard
import com.android.car.launcher.core.ui.MiniIviColors
import com.android.car.launcher.core.ui.MiniIviScaffold
import com.android.car.launcher.feature.dashboard.DashboardUiState
import com.android.car.launcher.feature.dashboard.HomeApp
import com.android.car.launcher.feature.dashboard.HomeAppIcon
import com.android.car.launcher.feature.dashboard.HomeDestination
import com.android.car.launcher.feature.media.MediaState
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.VehicleStatusState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun HomeScreen(
    destination: HomeDestination,
    state: DashboardUiState,
    apps: List<HomeApp>,
    onBackToHome: () -> Unit,
    onAppClick: (HomeApp) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    MiniIviScaffold {
        when (destination) {
            HomeDestination.Home -> Dashboard(
                state = state,
                apps = apps,
                onAppClick = onAppClick,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
            )
            HomeDestination.Apps -> AppDrawer(
                apps = apps,
                onBack = onBackToHome,
                onAppClick = onAppClick,
            )
        }
    }
}

@Composable
private fun Dashboard(
    state: DashboardUiState,
    apps: List<HomeApp>,
    onAppClick: (HomeApp) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val clock = rememberHomeClock()
    val weatherApp = apps.first { it.id == "weather" }
    val mapsApp = apps.first { it.id == "maps" }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 22.dp),
    ) {
        DashboardHeader(clock)
        Spacer(Modifier.height(18.dp))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < 1_400.dp
            val gap = if (compact) 14.dp else 18.dp
            val topRowHeight = when {
                maxHeight < 520.dp -> 164.dp
                compact -> 184.dp
                else -> 220.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topRowHeight)
                        .testTag("dashboard_top_row"),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    WeatherCard(
                        modifier = Modifier.weight(0.78f).fillMaxHeight(),
                        onClick = { onAppClick(weatherApp) },
                    )
                    MediaCard(
                        state = state.media,
                        modifier = Modifier.weight(1.22f).fillMaxHeight(),
                        onClick = { onAppClick(apps.first { it.id == "media" }) },
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("dashboard_bottom_row"),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    MapCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("map_card"),
                        onClick = { onAppClick(mapsApp) },
                    )
                    DashboardVehicleStatus(
                        hvac = state.hvac,
                        vehicleStatus = state.vehicleStatus,
                        compact = compact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(clock: HomeClock) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_greeting),
                color = MiniIviColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                color = MiniIviColors.TextSecondary,
                fontSize = 15.sp,
            )
        }
        Text(
            text = clock.date,
            color = MiniIviColors.TextSecondary,
            fontSize = 16.sp,
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = clock.time,
            color = MiniIviColors.TextPrimary,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WeatherCard(modifier: Modifier, onClick: () -> Unit) {
    MiniIviCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = MiniIviColors.Primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = MiniIviColors.Primary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    text = stringResource(R.string.weather),
                    color = MiniIviColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.weather_unavailable),
                    color = MiniIviColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun MediaCard(
    state: MediaState,
    modifier: Modifier,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    MiniIviCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                color = MiniIviColors.Secondary.copy(alpha = 0.42f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = MiniIviColors.TextPrimary,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.now_playing),
                    color = MiniIviColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = state.currentTrack?.title ?: stringResource(R.string.no_media_loaded),
                    color = MiniIviColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.currentTrack?.artist ?: stringResource(R.string.media_unavailable),
                    color = MiniIviColors.TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PlaybackIcon(Icons.Rounded.SkipPrevious, R.string.previous, state.tracks.isNotEmpty(), onPrevious)
            PlaybackIcon(
                if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (state.isPlaying) R.string.pause else R.string.play,
                state.tracks.isNotEmpty() && !state.isPreparing,
                onPlayPause,
                primary = true,
            )
            PlaybackIcon(Icons.Rounded.SkipNext, R.string.next, state.tracks.isNotEmpty(), onNext)
        }
    }
}

@Composable
private fun PlaybackIcon(
    icon: ImageVector,
    description: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp),
    ) {
        Surface(
            modifier = Modifier.size(if (primary) 48.dp else 40.dp),
            shape = CircleShape,
            color = if (primary) MiniIviColors.Primary else Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = stringResource(description),
                    tint = if (primary) Color.White else MiniIviColors.TextPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun MapCard(modifier: Modifier, onClick: () -> Unit) {
    MiniIviCard(modifier = modifier, onClick = onClick) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            Column(Modifier.align(Alignment.TopStart)) {
                Text(
                    text = stringResource(R.string.navigation_preview),
                    color = MiniIviColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.open_maps),
                    color = MiniIviColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
            Canvas(Modifier.fillMaxSize().padding(top = 46.dp, start = 24.dp, end = 18.dp)) {
                val route = Path().apply {
                    moveTo(size.width * 0.06f, size.height * 0.76f)
                    cubicTo(
                        size.width * 0.30f,
                        size.height * 0.18f,
                        size.width * 0.48f,
                        size.height * 0.88f,
                        size.width * 0.70f,
                        size.height * 0.40f,
                    )
                    cubicTo(
                        size.width * 0.80f,
                        size.height * 0.18f,
                        size.width * 0.88f,
                        size.height * 0.32f,
                        size.width * 0.94f,
                        size.height * 0.10f,
                    )
                }
                drawPath(
                    route,
                    brush = Brush.linearGradient(
                        listOf(MiniIviColors.Primary, MiniIviColors.Secondary),
                    ),
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                )
                drawCircle(
                    color = MiniIviColors.Primary,
                    radius = 10.dp.toPx(),
                    center = Offset(size.width * 0.06f, size.height * 0.76f),
                )
                drawCircle(
                    color = MiniIviColors.Secondary,
                    radius = 12.dp.toPx(),
                    center = Offset(size.width * 0.94f, size.height * 0.10f),
                )
            }
            Icon(
                Icons.Rounded.Map,
                contentDescription = null,
                tint = MiniIviColors.Primary,
                modifier = Modifier.align(Alignment.TopEnd).size(34.dp),
            )
        }
    }
}

@Composable
private fun DashboardVehicleStatus(
    hvac: HvacState,
    vehicleStatus: VehicleStatusState,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val status = vehicleStatus(hvac)
    val cabinTemperature = cabinTemperature(hvac)
    val acStatus = when {
        hvac.status == FeatureStatus.CONNECTING -> stringResource(R.string.vehicle_climate_connecting)
        hvac.available && hvac.acAvailable && hvac.acOn -> stringResource(R.string.ac_on)
        hvac.available && hvac.acAvailable -> stringResource(R.string.ac_off)
        else -> stringResource(R.string.hvac_unavailable)
    }
    val battery = if (vehicleStatus.available && vehicleStatus.hasBatteryPercentage) {
        String.format(Locale.ENGLISH, "%.0f%%", vehicleStatus.batteryPercentage)
    } else "--"
    val outside = if (vehicleStatus.available && vehicleStatus.hasOutsideTemperature) {
        String.format(Locale.ENGLISH, "%.1f°C", vehicleStatus.outsideTemperatureCelsius)
    } else "--°C"
    val range = if (vehicleStatus.available && vehicleStatus.hasRange) {
        String.format(Locale.ENGLISH, "%.0f km", vehicleStatus.rangeKilometers)
    } else "-- km"
    val tires = if (vehicleStatus.available && vehicleStatus.hasTirePressure) {
        String.format(Locale.ENGLISH, "%.0f kPa", vehicleStatus.minimumTirePressureKpa)
    } else "-- kPa"
    val tireHealth = if (vehicleStatus.tiresHealthy) {
        stringResource(R.string.tires_normal)
    } else {
        stringResource(R.string.tires_low)
    }
    Box(
        modifier = modifier
            .testTag("vehicle_background")
            .semantics {
                contentDescription = "$status, $battery, $cabinTemperature, $outside, $range, $tires, $tireHealth, $acStatus"
            },
    ) {
        DashboardRouteDecoration(
            Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 12.dp else 24.dp,
                    top = if (compact) 12.dp else 24.dp,
                    end = if (compact) 12.dp else 18.dp,
                    bottom = if (compact) 8.dp else 12.dp,
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 16.dp else 24.dp,
                    vertical = if (compact) 16.dp else 22.dp,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.DirectionsCar,
                    contentDescription = null,
                    tint = MiniIviColors.Primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.vehicle_status),
                    color = MiniIviColors.TextPrimary,
                    fontSize = if (compact) 15.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            Text(
                text = status,
                color = MiniIviColors.TextSecondary,
                fontSize = if (compact) 11.sp else 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vehicle_metrics_panel"),
                shape = RoundedCornerShape(if (compact) 20.dp else 26.dp),
                color = MiniIviColors.Surface.copy(alpha = 0.58f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MiniIviColors.Border.copy(alpha = 0.7f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (compact) 10.dp else 16.dp,
                        vertical = if (compact) 8.dp else 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
                        VehicleMetricTile(
                            icon = Icons.Rounded.BatteryFull,
                            label = stringResource(R.string.vehicle_battery),
                            value = battery,
                            compact = compact,
                            tag = "battery",
                        )
                        VehicleMetricTile(
                            icon = Icons.Rounded.DeviceThermostat,
                            label = stringResource(R.string.vehicle_cabin),
                            value = cabinTemperature,
                            compact = compact,
                            tag = "cabin",
                        )
                        VehicleMetricTile(
                            icon = Icons.Rounded.WbSunny,
                            label = stringResource(R.string.vehicle_outside),
                            value = outside,
                            compact = compact,
                            tag = "outside",
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
                        VehicleMetricTile(
                            icon = Icons.Rounded.Route,
                            label = stringResource(R.string.vehicle_range),
                            value = range,
                            compact = compact,
                            tag = "range",
                        )
                        VehicleMetricTile(
                            icon = Icons.Rounded.TireRepair,
                            label = stringResource(R.string.vehicle_tires),
                            value = tires,
                            supporting = tireHealth,
                            warning = !vehicleStatus.tiresHealthy,
                            compact = compact,
                            tag = "tires",
                        )
                        VehicleMetricTile(
                            icon = Icons.Rounded.AcUnit,
                            label = stringResource(R.string.vehicle_climate),
                            value = acStatus,
                            compact = compact,
                            tag = "climate",
                        )
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.home_vehicle_premium),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(if (compact) 0.92f else 0.90f)
                    .weight(1.18f)
                    .align(Alignment.End)
                    .testTag("vehicle_image"),
            )
        }
    }
}

@Composable
private fun RowScope.VehicleMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    compact: Boolean,
    tag: String,
    supporting: String? = null,
    warning: Boolean = false,
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .height(if (compact) 54.dp else 68.dp)
            .testTag("vehicle_metric_$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (warning) VehicleWarning else MiniIviColors.Primary,
            modifier = Modifier.size(if (compact) 18.dp else 22.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                color = MiniIviColors.TextSecondary,
                fontSize = if (compact) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = MiniIviColors.TextPrimary,
                fontSize = if (compact) 13.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supporting?.let {
                Text(
                    text = it,
                    color = if (warning) VehicleWarning else MiniIviColors.TextSecondary,
                    fontSize = if (compact) 8.sp else 9.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

private val VehicleWarning = Color(0xFFC35B6E)

@Composable
private fun DashboardRouteDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val route = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.88f)
            cubicTo(
                size.width * 0.62f,
                size.height * 0.64f,
                size.width * 0.70f,
                size.height * 0.98f,
                size.width * 0.84f,
                size.height * 0.62f,
            )
            cubicTo(
                size.width * 0.90f,
                size.height * 0.45f,
                size.width * 0.94f,
                size.height * 0.60f,
                size.width * 0.98f,
                size.height * 0.30f,
            )
        }
        drawPath(
            route,
            brush = Brush.linearGradient(
                listOf(
                    MiniIviColors.Primary.copy(alpha = 0.14f),
                    MiniIviColors.Secondary.copy(alpha = 0.16f),
                ),
            ),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = MiniIviColors.Primary.copy(alpha = 0.25f),
            radius = 8.dp.toPx(),
            center = Offset(size.width * 0.48f, size.height * 0.88f),
        )
        drawCircle(
            color = MiniIviColors.Secondary.copy(alpha = 0.30f),
            radius = 9.dp.toPx(),
            center = Offset(size.width * 0.98f, size.height * 0.30f),
        )
    }
}

@Composable
private fun AppDrawer(
    apps: List<HomeApp>,
    onBack: () -> Unit,
    onAppClick: (HomeApp) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MiniIviColors.SurfaceRaised,
                onClick = onBack,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MiniIviColors.TextPrimary,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .testTag("app_drawer_grid"),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(apps, key = HomeApp::id) { app ->
                MiniIviCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .testTag("app_item_${app.id}"),
                    onClick = { onAppClick(app) },
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        E01AppIcon(
                            icon = app.icon,
                            modifier = Modifier.size(120.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(app.titleRes),
                            color = MiniIviColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun vehicleStatus(hvac: HvacState): String = when {
    hvac.status == FeatureStatus.CONNECTING -> stringResource(R.string.vehicle_climate_connecting)
    hvac.available -> stringResource(R.string.vehicle_climate_connected)
    else -> stringResource(R.string.vehicle_data_unavailable)
}

private fun cabinTemperature(hvac: HvacState): String =
    if (hvac.available && hvac.hasCabinTemperature) {
        String.format(Locale.ENGLISH, "%.1f°C", hvac.cabinTemperatureCelsius)
    } else {
        "--°C"
    }

private data class HomeClock(val time: String, val date: String)

@Composable
private fun rememberHomeClock(): HomeClock {
    val locale = remember { Locale.ENGLISH }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, dd MMMM", locale) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            val millisecondsToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(millisecondsToNextMinute.coerceAtLeast(1_000L))
            now = LocalDateTime.now()
        }
    }

    return HomeClock(now.format(timeFormatter), now.format(dateFormatter))
}
