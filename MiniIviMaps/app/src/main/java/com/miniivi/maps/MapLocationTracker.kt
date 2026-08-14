package com.miniivi.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class MapPosition(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
)

internal data class MapLocationState(
    val position: MapPosition? = null,
    val permissionGranted: Boolean = false,
    val locating: Boolean = false,
    val hasLiveFix: Boolean = false,
)

internal class MapLocationTracker(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(
        MapLocationState(position = readPersistedPosition()),
    )
    private val listeners = mutableMapOf<String, LocationListener>()
    private var consumerCount = 0

    val state: StateFlow<MapLocationState> = mutableState.asStateFlow()

    fun start() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "MapLocationTracker must be started on the main thread"
        }
        consumerCount += 1
        if (consumerCount == 1) {
            Log.i(TAG, "event=tracking_started consumer_count=$consumerCount")
            startTracking()
        } else {
            logDebug("event=consumer_added consumer_count=$consumerCount")
        }
    }

    fun stop() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "MapLocationTracker must be stopped on the main thread"
        }
        consumerCount = (consumerCount - 1).coerceAtLeast(0)
        if (consumerCount == 0) {
            stopTracking()
            Log.i(TAG, "event=tracking_stopped consumer_count=0")
        } else {
            logDebug("event=consumer_removed consumer_count=$consumerCount")
        }
    }

    fun refreshPermission() {
        if (consumerCount == 0) return
        logDebug("event=permission_refresh_requested")
        stopTracking()
        startTracking()
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        val permissionGranted = hasLocationPermission(appContext)
        if (!permissionGranted) {
            if (mutableState.value.permissionGranted || mutableState.value.locating) {
                Log.w(TAG, "event=permission_changed granted=false")
            } else {
                Log.i(TAG, "event=permission_unavailable")
            }
            mutableState.value = mutableState.value.copy(
                permissionGranted = false,
                locating = false,
                hasLiveFix = false,
            )
            return
        }

        val enabledProviders = runCatching { locationManager.getProviders(true) }
            .getOrDefault(emptyList())
            .filter { it == LocationManager.GPS_PROVIDER || it == LocationManager.NETWORK_PROVIDER }
        Log.i(
            TAG,
            "event=providers_selected enabled_count=${enabledProviders.size} " +
                "has_cached_position=${mutableState.value.position != null}",
        )
        val systemPosition = enabledProviders
            .asSequence()
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull(Location::getElapsedRealtimeNanos)
            ?.toMapPosition()
        val initialPosition = MapPositionPolicy.selectNewest(
            mutableState.value.position,
            systemPosition,
        )
        mutableState.value = MapLocationState(
            position = initialPosition,
            permissionGranted = true,
            locating = enabledProviders.isNotEmpty(),
            hasLiveFix = false,
        )

        enabledProviders.forEach { provider ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    acceptLocation(location)
                }

                @Deprecated("Deprecated in Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) {
                    Log.i(TAG, "event=provider_disabled provider=$provider")
                    unregisterProvider(provider)
                    if (listeners.isEmpty()) {
                        mutableState.value = mutableState.value.copy(locating = false)
                    }
                }
            }
            val minimumTime = if (provider == LocationManager.GPS_PROVIDER) {
                GPS_MIN_TIME_MILLIS
            } else {
                NETWORK_MIN_TIME_MILLIS
            }
            val minimumDistance = if (provider == LocationManager.GPS_PROVIDER) {
                GPS_MIN_DISTANCE_METERS
            } else {
                NETWORK_MIN_DISTANCE_METERS
            }
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    minimumTime,
                    minimumDistance,
                    listener,
                    Looper.getMainLooper(),
                )
                listeners[provider] = listener
            }.onFailure { error ->
                Log.w(TAG, "event=provider_registration_failed provider=$provider", error)
            }
        }

        handler.removeCallbacks(locationTimeout)
        if (listeners.isNotEmpty()) handler.postDelayed(locationTimeout, LOCATION_TIMEOUT_MILLIS)
    }

    private fun stopTracking() {
        handler.removeCallbacks(locationTimeout)
        listeners.values.forEach { listener ->
            runCatching { locationManager.removeUpdates(listener) }
        }
        listeners.clear()
        mutableState.value = mutableState.value.copy(locating = false, hasLiveFix = false)
    }

    private fun unregisterProvider(provider: String) {
        listeners.remove(provider)?.let { listener ->
            runCatching { locationManager.removeUpdates(listener) }
        }
    }

    private fun acceptLocation(location: Location) {
        val position = location.toMapPosition()
        if (!MapPositionPolicy.isValid(position)) {
            Log.w(TAG, "event=location_ignored reason=invalid")
            return
        }
        val firstLiveFix = !mutableState.value.hasLiveFix
        persist(position)
        handler.removeCallbacks(locationTimeout)
        mutableState.value = MapLocationState(
            position = position,
            permissionGranted = true,
            locating = false,
            hasLiveFix = true,
        )
        if (firstLiveFix) {
            Log.i(TAG, "event=live_fix_acquired provider=${location.provider ?: "unknown"}")
        }
    }

    private val locationTimeout = Runnable {
        mutableState.value = mutableState.value.copy(locating = false, hasLiveFix = false)
        Log.w(TAG, "event=location_timeout timeout_ms=$LOCATION_TIMEOUT_MILLIS")
    }

    private fun Location.toMapPosition(): MapPosition = MapPosition(
        latitude = latitude,
        longitude = longitude,
        timestampMillis = time.takeIf { it > 0L } ?: System.currentTimeMillis(),
    )

    private fun persist(position: MapPosition) {
        preferences.edit()
            .putString(LATITUDE, position.latitude.toString())
            .putString(LONGITUDE, position.longitude.toString())
            .putLong(TIMESTAMP, position.timestampMillis)
            .apply()
    }

    private fun readPersistedPosition(): MapPosition? {
        val latitude = preferences.getString(LATITUDE, null)?.toDoubleOrNull() ?: return null
        val longitude = preferences.getString(LONGITUDE, null)?.toDoubleOrNull() ?: return null
        val position = MapPosition(latitude, longitude, preferences.getLong(TIMESTAMP, 0L))
        return position.takeIf(MapPositionPolicy::isValid)
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "MiniIviLocation"
        const val PREFERENCES = "map_location"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val TIMESTAMP = "timestamp"
        const val GPS_MIN_TIME_MILLIS = 1_000L
        const val NETWORK_MIN_TIME_MILLIS = 5_000L
        const val GPS_MIN_DISTANCE_METERS = 1f
        const val NETWORK_MIN_DISTANCE_METERS = 10f
        const val LOCATION_TIMEOUT_MILLIS = 10_000L
    }
}

internal object MapPositionPolicy {
    fun selectNewest(first: MapPosition?, second: MapPosition?): MapPosition? =
        listOfNotNull(first, second).maxByOrNull(MapPosition::timestampMillis)

    fun isValid(position: MapPosition): Boolean =
        position.latitude.isFinite() &&
            position.longitude.isFinite() &&
            position.latitude in -90.0..90.0 &&
            position.longitude in -180.0..180.0
}

internal fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
