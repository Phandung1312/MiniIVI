package com.miniivi.car.service.control

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import com.miniivi.car.api.CarServiceError
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.VehicleStatusState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal object VehicleStatusPolicy {
    const val MOCK_BATTERY_PERCENTAGE = 78f
    const val MOCK_OUTSIDE_TEMPERATURE_CELSIUS = 30f
    const val MOCK_RANGE_KILOMETERS = 320f
    const val MOCK_TIRE_PRESSURE_KPA = 230f

    fun batteryPercentage(level: Float, capacity: Float): Float? =
        if (level.isFinite() && capacity.isFinite() && capacity > 0f) {
            (level / capacity * 100f).coerceIn(0f, 100f)
        } else {
            null
        }

    fun minimumTirePressure(values: Collection<Float>): Float? =
        values.filter { it.isFinite() && it >= 0f }.minOrNull()
}

class VehicleStatusController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(VehicleStatusState())
    val state: StateFlow<VehicleStatusState> = mutableState.asStateFlow()

    private val carConnection = LifecycleAwareCarConnection(
        context = context,
        onReady = { lifecycleCar -> scope.launch { onCarReady(lifecycleCar) } },
        onUnavailable = { scope.launch { onCarUnavailable() } },
    )
    private var started = false
    private var connectionJob: Job? = null
    private var propertyManager: CarPropertyManager? = null
    private var propertyCallback: CarPropertyManager.CarPropertyEventCallback? = null
    private var supportedProperties = emptySet<Int>()
    private var tireAreas = emptySet<Int>()
    private val tireValues = mutableMapOf<Int, Float>()
    private val tireMinimums = mutableMapOf<Int, Float>()
    private var batteryLevel: Float? = null
    private var batteryCapacity: Float? = null
    private var propertyErrorReported = false

    fun start() {
        if (started) return
        started = true
        Log.i(TAG, "event=controller_started feature=vehicle_status")
        startConnectionLoop()
    }

    fun stop() {
        if (!started) return
        started = false
        Log.i(TAG, "event=controller_stopping feature=vehicle_status")
        connectionJob?.cancel()
        connectionJob = null
        disconnectBlocking()
        mutableState.value = VehicleStatusState(
            status = FeatureStatus.UNAVAILABLE,
            errorCode = CarServiceError.PLATFORM_UNAVAILABLE,
            diagnosticMessage = "Vehicle status controller stopped",
        )
        Log.i(TAG, "event=controller_stopped feature=vehicle_status")
    }

    fun refresh() {
        scope.launch {
            if (propertyManager == null) return@launch
            runCatching {
                batteryLevel = readFloatOrNull(EV_BATTERY_LEVEL)
                batteryCapacity = readFloatOrNull(EV_CURRENT_BATTERY_CAPACITY)
                    ?: readFloatOrNull(INFO_EV_BATTERY_CAPACITY)
                refreshState()
            }.onFailure { error ->
                Log.e(
                    TAG,
                    "event=operation_failed feature=vehicle_status operation=refresh",
                    error,
                )
                publishUnavailable(error.message)
            }
        }
    }

    private fun startConnectionLoop() {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            var retryDelay = INITIAL_RETRY_MILLIS
            var lastLoggedRetryDelay = -1L
            while (isActive && started && propertyManager == null && carConnection.car == null) {
                mutableState.value = mutableState.value.copy(
                    status = FeatureStatus.CONNECTING,
                    available = false,
                    diagnosticMessage = null,
                )
                val connected = runCatching { carConnection.connect() }
                    .onFailure { error ->
                        if (retryDelay != lastLoggedRetryDelay) {
                            Log.w(
                                TAG,
                                "event=connection_retry feature=vehicle_status delay_ms=$retryDelay",
                                error,
                            )
                            lastLoggedRetryDelay = retryDelay
                        }
                        disconnectBlocking()
                        publishUnavailable(error.message)
                    }
                    .isSuccess
                if (connected) return@launch
                delay(retryDelay)
                retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_MILLIS)
            }
        }
    }

    private fun onCarReady(lifecycleCar: Car) {
        if (!started || propertyManager != null) return
        runCatching { connectPropertyManager(lifecycleCar) }
            .onSuccess {
                Log.i(
                    TAG,
                    "event=backend_ready feature=vehicle_status backend=aaos " +
                        "properties=${supportedProperties.size} tire_areas=${tireAreas.size}",
                )
            }
            .onFailure { error ->
                Log.e(
                    TAG,
                    "event=backend_initialization_failed feature=vehicle_status backend=aaos",
                    error,
                )
                clearPropertyManager()
                publishUnavailable(error.message)
            }
    }

    private fun onCarUnavailable() {
        Log.w(TAG, "event=backend_unavailable feature=vehicle_status backend=aaos")
        clearPropertyManager(unregisterCallback = false)
        publishUnavailable("AAOS car service is unavailable")
    }

    private fun connectPropertyManager(lifecycleCar: Car) {
        propertyManager = lifecycleCar.getCarManager(CarPropertyManager::class.java)
            ?: error("Car property service is unavailable")

        supportedProperties = PROPERTY_IDS.filter { getConfig(it) != null }.toSet()
        val tireConfig = getConfig(TIRE_PRESSURE)
        tireAreas = tireConfig?.let(::getAreaIds)?.toSet().orEmpty()
        tireMinimums.clear()
        tireAreas.forEach { area ->
            tireMinimums[area] = getMinValue(tireConfig, area, DEFAULT_TIRE_MINIMUM)
        }
        batteryLevel = readFloatOrNull(EV_BATTERY_LEVEL)
        batteryCapacity = readFloatOrNull(EV_CURRENT_BATTERY_CAPACITY)
            ?: readFloatOrNull(INFO_EV_BATTERY_CAPACITY)
        refreshState()
        registerCallbacks()
    }

    private fun registerCallbacks() {
        val manager = checkNotNull(propertyManager)
        propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                scope.launch { handlePropertyValue(value) }
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                if (!propertyErrorReported) {
                    propertyErrorReported = true
                    Log.w(TAG, "event=property_error feature=vehicle_status")
                }
            }
        }
        supportedProperties.forEach { propertyId ->
            runCatching {
                manager.registerCallback(checkNotNull(propertyCallback), propertyId, CALLBACK_RATE_HZ)
            }
                .onFailure { error ->
                    Log.w(
                        TAG,
                        "event=property_registration_failed feature=vehicle_status " +
                            "property=0x${propertyId.toString(16)}",
                        error,
                    )
                }
        }
    }

    private fun handlePropertyValue(value: CarPropertyValue<*>) {
        runCatching {
            val status = value.status
            if (status != STATUS_AVAILABLE) return
            if (propertyErrorReported) {
                propertyErrorReported = false
                Log.i(TAG, "event=property_recovered feature=vehicle_status")
            }
            val propertyId = value.propertyId
            val areaId = value.areaId
            val data = value.value
            when (propertyId) {
                EV_BATTERY_LEVEL -> batteryLevel = (data as Number).toFloat()
                EV_CURRENT_BATTERY_CAPACITY -> batteryCapacity = (data as Number).toFloat()
                TIRE_PRESSURE -> tireValues[areaId] = (data as Number).toFloat()
            }
            refreshState()
        }.onFailure { error ->
            Log.w(TAG, "event=property_event_ignored feature=vehicle_status reason=invalid", error)
        }
    }

    private fun refreshState() {
        val battery = VehicleStatusPolicy.batteryPercentage(
            batteryLevel ?: Float.NaN,
            batteryCapacity ?: Float.NaN,
        ) ?: VehicleStatusPolicy.MOCK_BATTERY_PERCENTAGE
        val outside = readFloatOrNull(ENV_OUTSIDE_TEMPERATURE)
            ?: VehicleStatusPolicy.MOCK_OUTSIDE_TEMPERATURE_CELSIUS
        val range = readFloatOrNull(RANGE_REMAINING)?.div(METERS_PER_KILOMETER)
            ?: VehicleStatusPolicy.MOCK_RANGE_KILOMETERS
        if (tireAreas.isNotEmpty()) {
            tireAreas.forEach { area ->
                if (area !in tireValues) readFloatOrNull(TIRE_PRESSURE, area)?.let { tireValues[area] = it }
            }
        }
        val tirePressure = VehicleStatusPolicy.minimumTirePressure(tireValues.values)
            ?: VehicleStatusPolicy.MOCK_TIRE_PRESSURE_KPA
        val tiresHealthy = tireValues
            .filter { it.key in tireMinimums }
            .all { (area, value) -> value >= (tireMinimums[area] ?: DEFAULT_TIRE_MINIMUM) }
        mutableState.value = VehicleStatusState(
            status = FeatureStatus.READY,
            available = true,
            hasBatteryPercentage = true,
            batteryPercentage = battery,
            hasOutsideTemperature = true,
            outsideTemperatureCelsius = outside,
            hasRange = true,
            rangeKilometers = range,
            hasTirePressure = true,
            minimumTirePressureKpa = tirePressure,
            tiresHealthy = tireValues.isEmpty() || tiresHealthy,
            errorCode = CarServiceError.NONE,
        )
    }

    private fun readFloatOrNull(propertyId: Int, areaId: Int = GLOBAL_AREA): Float? =
        if (propertyId !in supportedProperties && propertyManager != null) null else runCatching {
            propertyManager?.getFloatProperty(propertyId, areaId)
        }.getOrNull()?.takeIf { it.isFinite() }

    private fun getConfig(propertyId: Int): CarPropertyConfig<*>? = runCatching {
        propertyManager?.getCarPropertyConfig(propertyId)
    }.getOrNull()

    private fun getAreaIds(config: CarPropertyConfig<*>): IntArray = config.areaIds

    private fun getMinValue(config: CarPropertyConfig<*>?, areaId: Int, fallback: Float): Float {
        if (config == null) return fallback
        val value = runCatching { config.getMinValue(areaId) }.getOrNull()
        return (value as? Number)?.toFloat() ?: fallback
    }

    private fun publishUnavailable(message: String?) {
        if (mutableState.value.status != FeatureStatus.UNAVAILABLE) {
            Log.w(TAG, "event=feature_unavailable feature=vehicle_status")
        }
        mutableState.value = VehicleStatusState(
            status = FeatureStatus.UNAVAILABLE,
            available = false,
            errorCode = CarServiceError.PLATFORM_UNAVAILABLE,
            diagnosticMessage = message,
        )
    }

    private fun disconnectBlocking() {
        clearPropertyManager()
        carConnection.disconnect()
    }

    private fun clearPropertyManager(unregisterCallback: Boolean = true) {
        val manager = propertyManager
        val callback = propertyCallback
        if (unregisterCallback && manager != null && callback != null) {
            runCatching {
                manager.unregisterCallback(callback)
            }.onFailure {
                Log.w(TAG, "event=callback_unregister_failed feature=vehicle_status", it)
            }
        }
        propertyCallback = null
        propertyManager = null
        supportedProperties = emptySet()
        tireAreas = emptySet()
        tireValues.clear()
        tireMinimums.clear()
        batteryLevel = null
        batteryCapacity = null
        propertyErrorReported = false
    }

    private companion object {
        const val TAG = "MiniIviVehicleStatus"
        const val GLOBAL_AREA = 0
        const val STATUS_AVAILABLE = 0
        const val CALLBACK_RATE_HZ = 1f
        const val METERS_PER_KILOMETER = 1_000f
        const val DEFAULT_TIRE_MINIMUM = 193f
        const val EV_BATTERY_LEVEL = VehiclePropertyIds.EV_BATTERY_LEVEL
        const val EV_CURRENT_BATTERY_CAPACITY = VehiclePropertyIds.EV_CURRENT_BATTERY_CAPACITY
        const val INFO_EV_BATTERY_CAPACITY = VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY
        const val RANGE_REMAINING = VehiclePropertyIds.RANGE_REMAINING
        const val ENV_OUTSIDE_TEMPERATURE = VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE
        const val TIRE_PRESSURE = VehiclePropertyIds.TIRE_PRESSURE
        val PROPERTY_IDS = setOf(
            EV_BATTERY_LEVEL,
            EV_CURRENT_BATTERY_CAPACITY,
            INFO_EV_BATTERY_CAPACITY,
            RANGE_REMAINING,
            ENV_OUTSIDE_TEMPERATURE,
            TIRE_PRESSURE,
        )
        const val INITIAL_RETRY_MILLIS = 1_000L
        const val MAX_RETRY_MILLIS = 30_000L
    }
}
