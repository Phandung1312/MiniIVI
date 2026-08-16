package com.android.car.launcher.feature.dashboard.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.viewinterop.AndroidView
import com.android.car.launcher.R
import com.android.car.launcher.core.ui.MiniIviCard
import com.android.car.launcher.core.ui.MiniIviColors
import com.android.car.launcher.core.ui.MiniIviScaffold
import com.android.car.launcher.feature.dashboard.domain.model.FeatureStatus
import com.android.car.launcher.feature.dashboard.domain.model.HvacState
import com.android.car.launcher.feature.dashboard.domain.model.HomeDestination
import com.android.car.launcher.feature.dashboard.domain.model.VehicleStatusState
import com.android.car.launcher.feature.dashboard.presentation.model.DashboardUiState
import com.android.car.launcher.feature.dashboard.presentation.model.HomeApp
import com.android.car.launcher.feature.dashboard.presentation.model.HomeAppIcon
import com.android.car.launcher.feature.maps.presentation.ui.MapPreviewHostView
import com.android.car.launcher.feature.maps.presentation.model.MapPreviewUiState
import com.android.car.launcher.feature.media.domain.model.MediaState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun HomeScreen(
    state: DashboardUiState,
    onBackToHome: () -> Unit,
    onAppClick: (HomeApp) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    mapPreviewContent: @Composable () -> Unit = { MapPreviewPane() },
) {
    MiniIviScaffold {
        when (state.destination) {
            HomeDestination.Home -> Dashboard(
                state = state,
                apps = state.apps,
                onAppClick = onAppClick,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                mapPreviewContent = mapPreviewContent,
            )
            HomeDestination.Apps -> AppDrawer(
                apps = state.apps,
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
    mapPreviewContent: @Composable () -> Unit,
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
                        mapPreviewContent = mapPreviewContent,
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
private fun MapCard(
    modifier: Modifier,
    onClick: () -> Unit,
    mapPreviewContent: @Composable () -> Unit,
) {
    MiniIviCard(modifier = modifier, onClick = onClick) {
        Box(Modifier.fillMaxSize()) { mapPreviewContent() }
    }
}

@Composable
private fun MapPreviewPane() {
    var state by remember { mutableStateOf(MapPreviewUiState.CONNECTING) }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                MapPreviewHostView(context).apply { onStateChanged = { state = it } }
            },
            update = { host -> host.onStateChanged = { state = it } },
            modifier = Modifier.fillMaxSize().testTag("map_preview_surface"),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_preview_overlay")
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66201A29),
                            Color.Transparent,
                            Color(0x2615121C),
                        ),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .testTag("map_preview_badge"),
            color = MiniIviColors.SurfaceRaised.copy(alpha = 0.76f),
            border = BorderStroke(1.dp, MiniIviColors.GlassHighlight),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(R.string.navigation_preview),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                mapPreviewStatus(state)?.let { status ->
                    Text(
                        text = status,
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun mapPreviewStatus(state: MapPreviewUiState): String? = when (state) {
    MapPreviewUiState.CONNECTING -> stringResource(R.string.map_preview_connecting)
    MapPreviewUiState.LOCATING -> stringResource(R.string.map_preview_locating)
    MapPreviewUiState.READY -> null
    MapPreviewUiState.LAST_KNOWN -> stringResource(R.string.map_preview_last_known)
    MapPreviewUiState.LOCATION_UNAVAILABLE ->
        stringResource(R.string.map_preview_location_unavailable)
    MapPreviewUiState.TILE_UNAVAILABLE -> stringResource(R.string.map_preview_tiles_unavailable)
    MapPreviewUiState.UNAVAILABLE -> stringResource(R.string.map_preview_unavailable)
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
    val cardShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier.testTag("vehicle_column"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 204.dp else 248.dp)
                .testTag("vehicle_background")
                .semantics {
                    contentDescription = "$status, $battery, $cabinTemperature, $outside, $range, $tires, $tireHealth, $acStatus"
                }
                .shadow(
                    elevation = 8.dp,
                    shape = cardShape,
                    ambientColor = MiniIviColors.GlassShadow,
                    spotColor = MiniIviColors.GlassShadow,
                )
                .background(VehicleCeramicCardBrush, cardShape)
                .border(1.dp, VehicleCeramicBorder, cardShape)
                .clip(cardShape),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (compact) 12.dp else 16.dp,
                        vertical = if (compact) 10.dp else 16.dp,
                    ),
            ) {
                Row(
                    modifier = Modifier.height(if (compact) 40.dp else 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_vehicle_status_car),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(if (compact) 38.dp else 50.dp)
                            .testTag("vehicle_header_icon"),
                    )
                    Spacer(Modifier.width(if (compact) 9.dp else 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vehicle_status),
                            color = MiniIviColors.TextPrimary,
                            fontSize = if (compact) 15.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = status,
                            color = MiniIviColors.TextSecondary,
                            fontSize = if (compact) 10.sp else 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("vehicle_metrics_panel"),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                    ) {
                        VehicleMetricTile(
                            icon = R.drawable.ic_vehicle_status_battery,
                            label = stringResource(R.string.vehicle_battery),
                            value = battery,
                            compact = compact,
                            tag = "battery",
                        )
                        VehicleMetricTile(
                            icon = R.drawable.ic_vehicle_status_cabin,
                            label = stringResource(R.string.vehicle_cabin),
                            value = cabinTemperature,
                            compact = compact,
                            tag = "cabin",
                        )
                        VehicleMetricTile(
                            icon = R.drawable.ic_vehicle_status_outside,
                            label = stringResource(R.string.vehicle_outside),
                            value = outside,
                            compact = compact,
                            tag = "outside",
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                    ) {
                        VehicleMetricTile(
                            icon = R.drawable.ic_vehicle_status_range,
                            label = stringResource(R.string.vehicle_range),
                            value = range,
                            compact = compact,
                            tag = "range",
                        )
                        VehicleMetricTile(
                            icon = R.drawable.ic_vehicle_status_tires,
                            label = stringResource(R.string.vehicle_tires),
                            value = tires,
                            supporting = tireHealth,
                            warning = !vehicleStatus.tiresHealthy,
                            compact = compact,
                            tag = "tires",
                        )
                        VehicleMetricTile(
                            icon = R.drawable.ic_vehicle_status_climate,
                            label = stringResource(R.string.vehicle_climate),
                            value = acStatus,
                            compact = compact,
                            tag = "climate",
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.76f)
                    .height(if (compact) 42.dp else 64.dp)
                    .background(VehicleGroundGlowBrush, CircleShape),
            )
            Image(
                painter = painterResource(R.drawable.home_vehicle_premium),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomEnd)
                    .testTag("vehicle_image"),
            )
        }
    }
}

@Composable
private fun RowScope.VehicleMetricTile(
    icon: Int,
    label: String,
    value: String,
    compact: Boolean,
    tag: String,
    supporting: String? = null,
    warning: Boolean = false,
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .shadow(3.dp, shape, ambientColor = VehicleTileShadow, spotColor = VehicleTileShadow)
            .background(VehicleCeramicTileBrush, shape)
            .border(
                1.dp,
                if (warning) VehicleWarning.copy(alpha = 0.82f) else VehicleCeramicTileBorder,
                shape,
            )
            .clip(shape)
            .padding(horizontal = if (compact) 6.dp else 9.dp)
            .testTag("vehicle_metric_$tag"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(if (compact) 30.dp else 36.dp)
                .testTag("vehicle_metric_icon_$tag"),
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
private val VehicleCeramicBorder = Color.White.copy(alpha = 0.28f)
private val VehicleCeramicTileBorder = Color.White.copy(alpha = 0.15f)
private val VehicleTileShadow = Color.Black.copy(alpha = 0.38f)
private val VehicleCeramicCardBrush = Brush.linearGradient(
    listOf(
        MiniIviColors.SurfaceRaised,
        MiniIviColors.Surface,
        Color(0xFF3A3046),
    ),
)
private val VehicleCeramicTileBrush = Brush.verticalGradient(
    listOf(
        MiniIviColors.SurfaceRaised.copy(alpha = 0.72f),
        Color(0xFF4A3E59),
        Color(0xFF2A2435),
    ),
)
private val VehicleGroundGlowBrush = Brush.radialGradient(
    listOf(
        MiniIviColors.Primary.copy(alpha = 0.30f),
        MiniIviColors.Secondary.copy(alpha = 0.10f),
        Color.Transparent,
    ),
)

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
