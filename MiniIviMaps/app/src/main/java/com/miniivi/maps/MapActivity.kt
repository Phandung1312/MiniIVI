package com.miniivi.maps

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "event=activity_created component=MapActivity")
        val locationTracker = (application as MapsApplication).locationTracker
        setContent {
            MapsTheme {
                MapRoute(locationTracker = locationTracker, onFinish = ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "event=activity_resumed component=MapActivity")
        }
    }

    override fun onPause() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "event=activity_paused component=MapActivity")
        }
        super.onPause()
    }

    override fun onDestroy() {
        Log.i(TAG, "event=activity_destroyed component=MapActivity")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MiniIviMaps"
    }
}

@Composable
private fun MapRoute(
    locationTracker: MapLocationTracker,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationState by locationTracker.state.collectAsState()
    var mapView by remember { mutableStateOf<OpenStreetMapView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var following by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    val locationUnavailable = stringResource(R.string.location_unavailable)
    val permissionRequired = stringResource(R.string.location_permission_required)

    DisposableEffect(locationTracker) {
        locationTracker.start()
        onDispose { locationTracker.stop() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        locationTracker.refreshPermission()
        if (MapLocationPermission.isGranted(grants)) {
            following = true
        } else {
            scope.launch { snackbarHostState.showSnackbar(permissionRequired) }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    LaunchedEffect(locationState.position, following, mapView) {
        if (following) {
            locationState.position?.let { position ->
                mapView?.showLocation(position.latitude, position.longitude)
            }
        }
    }

    BackHandler(onBack = onFinish)
    MapScreen(
        loading = loading,
        locating = following && locationState.locating,
        loadFailed = loadFailed,
        snackbarHostState = snackbarHostState,
        onMapAvailable = { view ->
            view.onUserGesture = { following = false }
            mapView = view
        },
        onLoadingChanged = { loading = it },
        onLoadFailed = {
            loading = false
            loadFailed = true
        },
        onBack = onFinish,
        onMyLocation = {
            following = true
            if (hasLocationPermission(context)) {
                locationTracker.refreshPermission()
                val position = locationState.position
                if (position == null) {
                    scope.launch { snackbarHostState.showSnackbar(locationUnavailable) }
                } else {
                    mapView?.showLocation(position.latitude, position.longitude)
                }
            } else {
                permissionLauncher.launch(LOCATION_PERMISSIONS)
            }
        },
        onRetry = {
            loadFailed = false
            loading = true
            mapView?.retryFailedTiles()
        },
    )
}

@Composable
internal fun MapScreen(
    loading: Boolean,
    locating: Boolean,
    loadFailed: Boolean,
    snackbarHostState: SnackbarHostState,
    onMapAvailable: (OpenStreetMapView) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onLoadFailed: () -> Unit,
    onBack: () -> Unit,
    onMyLocation: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(start = NavigationRailClearance),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { contentPadding ->
            Column(Modifier.fillMaxSize().padding(contentPadding)) {
                MapControls(
                    loading = loading,
                    locating = locating,
                    loadFailed = loadFailed,
                    onBack = onBack,
                    onMyLocation = onMyLocation,
                    onRetry = onRetry,
                )
                AndroidView(
                    factory = { context ->
                        OpenStreetMapView(context).apply {
                            this.onLoadingChanged = onLoadingChanged
                            this.onInitialLoadFailed = onLoadFailed
                            onMapAvailable(this)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .testTag("map_view"),
                )
            }
        }
    }
}

@Composable
internal fun MapControls(
    loading: Boolean,
    locating: Boolean,
    loadFailed: Boolean,
    onBack: () -> Unit,
    onMyLocation: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(color = Background, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilledIconButton(
                onClick = onBack,
                modifier = Modifier.size(56.dp).testTag("map_back"),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            FilledIconButton(
                onClick = onMyLocation,
                enabled = !locating,
                modifier = Modifier.size(56.dp).testTag("map_my_location"),
            ) {
                if (locating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                } else {
                    Icon(
                        Icons.Rounded.MyLocation,
                        contentDescription = stringResource(R.string.my_location),
                    )
                }
            }
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            if (loading && !loadFailed) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp).testTag("map_loading"),
                    strokeWidth = 3.dp,
                )
            }
            if (loadFailed) {
                Text(text = stringResource(R.string.map_load_failed))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("map_retry"),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text(
                        text = stringResource(R.string.retry),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

internal object MapLocationPermission {
    fun isGranted(grants: Map<String, Boolean>): Boolean =
        grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
}

@Composable
private fun MapsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Primary,
            background = Background,
            surface = SurfaceRaised,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
)
private val Background = Color(0xFF15121C)
private val Primary = Color(0xFFA98CF5)
private val SurfaceRaised = Color(0xE06F5E88)
private val NavigationRailClearance = 135.2.dp
