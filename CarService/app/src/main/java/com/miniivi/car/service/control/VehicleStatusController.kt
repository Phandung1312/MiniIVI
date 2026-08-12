package com.miniivi.car.service.control

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.miniivi.car.api.CarServiceError
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.VehicleStatusState
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.lang.reflect.Proxy
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

@SuppressLint("PrivateApi")
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
    private var propertyManager: Any? = null
    private var propertyCallback: Any? = null
    private var supportedProperties = emptySet<Int>()
    private var tireAreas = emptySet<Int>()
    private val tireValues = mutableMapOf<Int, Float>()
    private val tireMinimums = mutableMapOf<Int, Float>()
    private var batteryLevel: Float? = null
    private var batteryCapacity: Float? = null

    fun start() {
        if (started) return
        started = true
        startConnectionLoop()
    }

    fun stop() {
        started = false
        connectionJob?.cancel()
        connectionJob = null
        disconnectBlocking()
        mutableState.value = VehicleStatusState(
            status = FeatureStatus.UNAVAILABLE,
            errorCode = CarServiceError.PLATFORM_UNAVAILABLE,
            diagnosticMessage = "Vehicle status controller stopped",
        )
    }

    private fun startConnectionLoop() {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            var retryDelay = INITIAL_RETRY_MILLIS
            while (isActive && started && propertyManager == null && carConnection.car == null) {
                mutableState.value = mutableState.value.copy(
                    status = FeatureStatus.CONNECTING,
                    available = false,
                    diagnosticMessage = null,
                )
                val connected = runCatching { carConnection.connect() }
                    .onFailure { error ->
                        Log.e(TAG, "Unable to connect to AAOS vehicle properties", error)
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

    private fun onCarReady(lifecycleCar: Any) {
        if (!started || propertyManager != null) return
        runCatching { connectPropertyManager(lifecycleCar) }
            .onFailure { error ->
                Log.e(TAG, "Unable to initialize vehicle status properties", error)
                clearPropertyManager()
                publishUnavailable(error.message)
            }
    }

    private fun onCarUnavailable() {
        clearPropertyManager(unregisterCallback = false)
        publishUnavailable("AAOS car service is unavailable")
    }

    private fun connectPropertyManager(lifecycleCar: Any) {
        val carClass = Class.forName(CAR_CLASS)
        propertyManager = carClass.getMethod("getCarManager", String::class.java)
            .invoke(lifecycleCar, PROPERTY_SERVICE)
            ?: error("Car property service is unavailable")

        supportedProperties = PROPERTY_IDS.filter { getConfig(it) != null }.toSet()
        val tireConfig = getConfig(TIRE_PRESSURE)
        tireAreas = tireConfig?.let(::getAreaIds)?.toSet().orEmpty()
        tireMinimums.clear()
        tireAreas.forEach { area ->
            tireMinimums[area] = getBound(tireConfig, "getMinValue", area, DEFAULT_TIRE_MINIMUM)
        }
        batteryLevel = readFloatOrNull(EV_BATTERY_LEVEL)
        batteryCapacity = readFloatOrNull(EV_CURRENT_BATTERY_CAPACITY)
            ?: readFloatOrNull(INFO_EV_BATTERY_CAPACITY)
        refreshState()
        registerCallbacks()
    }

    private fun registerCallbacks() {
        val manager = checkNotNull(propertyManager)
        val callbackClass = Class.forName(CAR_PROPERTY_CALLBACK)
        propertyCallback = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass),
        ) { proxy, method, args ->
            when (method.name) {
                "onChangeEvent" -> args?.firstOrNull()?.let { value ->
                    scope.launch { handlePropertyValue(value) }
                }
                "onErrorEvent" -> Log.w(TAG, "A vehicle property reported an error")
                "toString" -> "MiniIVI vehicle status callback"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        }
        val register = manager.javaClass.getMethod(
            "registerCallback",
            callbackClass,
            Int::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
        )
        supportedProperties.forEach { propertyId ->
            runCatching { register.invoke(manager, propertyCallback, propertyId, CALLBACK_RATE_HZ) }
                .onFailure { error -> Log.w(TAG, "Unable to register property 0x${propertyId.toString(16)}", error) }
        }
    }

    private fun handlePropertyValue(value: Any) {
        runCatching {
            val status = (value.javaClass.getMethod("getStatus").invoke(value) as? Int)
                ?: STATUS_AVAILABLE
            if (status != STATUS_AVAILABLE) return
            val propertyId = value.javaClass.getMethod("getPropertyId").invoke(value) as Int
            val areaId = value.javaClass.getMethod("getAreaId").invoke(value) as Int
            val data = value.javaClass.getMethod("getValue").invoke(value)
            when (propertyId) {
                EV_BATTERY_LEVEL -> batteryLevel = (data as Number).toFloat()
                EV_CURRENT_BATTERY_CAPACITY -> batteryCapacity = (data as Number).toFloat()
                TIRE_PRESSURE -> tireValues[areaId] = (data as Number).toFloat()
            }
            refreshState()
        }.onFailure { error -> Log.w(TAG, "Ignoring invalid vehicle property event", error) }
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
            (propertyManager?.javaClass
                ?.getMethod("getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                ?.invoke(propertyManager, propertyId, areaId) as Number).toFloat()
        }.getOrNull()?.takeIf { it.isFinite() }

    private fun getConfig(propertyId: Int): Any? = runCatching {
        propertyManager?.javaClass
            ?.getMethod("getCarPropertyConfig", Int::class.javaPrimitiveType)
            ?.invoke(propertyManager, propertyId)
    }.getOrNull()

    private fun getAreaIds(config: Any): IntArray {
        val areas = requireNotNull(config.javaClass.getMethod("getAreaIds").invoke(config))
        return IntArray(Array.getLength(areas)) { index -> Array.get(areas, index) as Int }
    }

    private fun getBound(config: Any?, methodName: String, areaId: Int, fallback: Float): Float {
        if (config == null) return fallback
        val methods = config.javaClass.methods.filter { it.name == methodName }
        val value = runCatching {
            methods.firstOrNull { it.parameterTypes.size == 1 }?.invoke(config, areaId)
                ?: methods.firstOrNull { it.parameterTypes.isEmpty() }?.invoke(config)
        }.getOrNull()
        return (value as? Number)?.toFloat() ?: fallback
    }

    private fun publishUnavailable(message: String?) {
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
                val unregister: Method? = manager.javaClass.methods.firstOrNull {
                    it.name == "unregisterCallback" && it.parameterTypes.size == 1
                }
                unregister?.invoke(manager, callback)
            }.onFailure { Log.w(TAG, "Unable to unregister vehicle status callback", it) }
        }
        propertyCallback = null
        propertyManager = null
        supportedProperties = emptySet()
        tireAreas = emptySet()
        tireValues.clear()
        tireMinimums.clear()
        batteryLevel = null
        batteryCapacity = null
    }

    private companion object {
        const val TAG = "MiniIviVehicleStatus"
        const val CAR_CLASS = "android.car.Car"
        const val PROPERTY_SERVICE = "property"
        const val CAR_PROPERTY_CALLBACK =
            "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback"
        const val GLOBAL_AREA = 0
        const val STATUS_AVAILABLE = 0
        const val CALLBACK_RATE_HZ = 1f
        const val METERS_PER_KILOMETER = 1_000f
        const val DEFAULT_TIRE_MINIMUM = 193f
        const val EV_BATTERY_LEVEL = 0x11600309
        const val EV_CURRENT_BATTERY_CAPACITY = 0x1160030D
        const val INFO_EV_BATTERY_CAPACITY = 0x11600106
        const val RANGE_REMAINING = 0x11600308
        const val ENV_OUTSIDE_TEMPERATURE = 0x11600703
        const val TIRE_PRESSURE = 0x17600309
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
