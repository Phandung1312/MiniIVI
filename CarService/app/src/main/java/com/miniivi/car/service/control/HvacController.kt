package com.miniivi.car.service.control

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.miniivi.car.api.CarServiceError
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.HvacZone
import com.miniivi.car.api.HvacZoneState
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal object HvacPolicy {
    fun selectLogicalAreas(areaIds: IntArray): Pair<Int?, Int?> {
        val left = areaIds.firstOrNull()
        val right = areaIds.lastOrNull()?.takeIf { it != left }
        return left to right
    }

    fun clampTemperature(value: Float, minimum: Float, maximum: Float): Float =
        value.coerceIn(minimum.coerceAtMost(maximum), maximum.coerceAtLeast(minimum))
}

@SuppressLint("PrivateApi")
class HvacController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private data class ZoneBinding(val areaId: Int, val state: HvacZoneState)

    private val mutableState = MutableStateFlow(HvacState())
    val state: StateFlow<HvacState> = mutableState.asStateFlow()

    private var started = false
    private var connectionJob: Job? = null
    private val carConnection = LifecycleAwareCarConnection(
        context = context,
        onReady = { lifecycleCar -> scope.launch { onCarReady(lifecycleCar) } },
        onUnavailable = { scope.launch { onCarUnavailable() } },
    )
    private var propertyManager: Any? = null
    private var propertyCallback: Any? = null
    private var leftBinding: ZoneBinding? = null
    private var rightBinding: ZoneBinding? = null
    private var cabinArea: Int? = null
    private var acArea: Int? = null

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
        mutableState.value = HvacState(
            status = FeatureStatus.UNAVAILABLE,
            diagnosticMessage = "HVAC controller stopped",
        )
    }

    fun refresh() {
        scope.launch {
            if (propertyManager == null) return@launch
            runCatching { readCurrentState() }
                .onSuccess { mutableState.value = it }
                .onFailure { error -> handlePlatformFailure("Unable to refresh HVAC state", error) }
        }
    }

    fun setTemperature(zone: Int, celsius: Float) {
        if (zone != HvacZone.LEFT && zone != HvacZone.RIGHT || !celsius.isFinite()) {
            publishArgumentError("Invalid HVAC zone or temperature")
            return
        }
        scope.launch {
            val target = when (zone) {
                HvacZone.LEFT -> leftBinding
                HvacZone.RIGHT -> rightBinding
                else -> null
            }
            if (target == null) {
                publishArgumentError("Requested HVAC zone is unavailable")
                return@launch
            }
            val manager = propertyManager
            if (manager == null) {
                publishUnavailable("HVAC property service is unavailable")
                return@launch
            }
            val requested = HvacPolicy.clampTemperature(
                celsius,
                target.state.minimumCelsius,
                target.state.maximumCelsius,
            )
            runCatching {
                manager.javaClass.getMethod(
                    "setFloatProperty",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType,
                ).invoke(manager, HVAC_TEMPERATURE_SET, target.areaId, requested)
                updateZoneTemperature(target.areaId, requested)
            }.onFailure { error -> handlePlatformFailure("Unable to set HVAC temperature", error) }
        }
    }

    fun setAcEnabled(enabled: Boolean) {
        scope.launch {
            val area = acArea
            val manager = propertyManager
            if (area == null || manager == null) {
                publishUnavailable("A/C control is unavailable")
                return@launch
            }
            runCatching {
                manager.javaClass.getMethod(
                    "setBooleanProperty",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                ).invoke(manager, HVAC_AC_ON, area, enabled)
                mutableState.update {
                    it.copy(
                        status = FeatureStatus.READY,
                        acOn = enabled,
                        errorCode = CarServiceError.NONE,
                        diagnosticMessage = null,
                    )
                }
            }.onFailure { error -> handlePlatformFailure("Unable to set A/C state", error) }
        }
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
                        Log.e(TAG, "Unable to connect to AAOS climate properties", error)
                        disconnectBlocking()
                        mutableState.value = HvacState(
                            status = FeatureStatus.UNAVAILABLE,
                            errorCode = CarServiceError.PLATFORM_UNAVAILABLE,
                            diagnosticMessage = error.message,
                        )
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
                Log.e(TAG, "Unable to connect to AAOS climate properties", error)
                clearPropertyManager()
                mutableState.value = HvacState(
                    status = FeatureStatus.UNAVAILABLE,
                    errorCode = CarServiceError.PLATFORM_UNAVAILABLE,
                    diagnosticMessage = error.message,
                )
            }
    }

    private fun onCarUnavailable() {
        clearPropertyManager(unregisterCallback = false)
        mutableState.value = HvacState(
            status = FeatureStatus.UNAVAILABLE,
            errorCode = CarServiceError.PLATFORM_UNAVAILABLE,
            diagnosticMessage = "AAOS car service is unavailable",
        )
    }

    private fun connectPropertyManager(lifecycleCar: Any) {
        val carClass = Class.forName(CAR_CLASS)
        propertyManager = carClass.getMethod("getCarManager", String::class.java)
            .invoke(lifecycleCar, PROPERTY_SERVICE)
            ?: error("Car property service is unavailable")

        val setConfig = getConfig(HVAC_TEMPERATURE_SET)
            ?: error("HVAC temperature control is unavailable")
        val (leftArea, rightArea) = HvacPolicy.selectLogicalAreas(getAreaIds(setConfig))
        checkNotNull(leftArea) { "No HVAC temperature zones are available" }

        leftBinding = createZoneBinding(HvacZone.LEFT, leftArea, setConfig)
        rightBinding = rightArea?.let { createZoneBinding(HvacZone.RIGHT, it, setConfig) }
        val currentConfig = getConfig(HVAC_TEMPERATURE_CURRENT)
        val acConfig = getConfig(HVAC_AC_ON)
        cabinArea = currentConfig?.let(::getAreaIds)?.firstOrNull()
        acArea = acConfig?.let(::getAreaIds)?.firstOrNull()

        mutableState.value = readCurrentState()
        createAndRegisterCallback(currentConfig != null, acConfig != null)
    }

    private fun createZoneBinding(zone: Int, areaId: Int, config: Any): ZoneBinding =
        ZoneBinding(
            areaId = areaId,
            state = HvacZoneState(
                zone = zone,
                available = true,
                hasTemperature = true,
                temperatureCelsius = readFloat(HVAC_TEMPERATURE_SET, areaId),
                minimumCelsius = getBound(config, "getMinValue", areaId, DEFAULT_MINIMUM),
                maximumCelsius = getBound(config, "getMaxValue", areaId, DEFAULT_MAXIMUM),
            ),
        )

    private fun readCurrentState(): HvacState {
        leftBinding = leftBinding?.refreshTemperature()
        rightBinding = rightBinding?.refreshTemperature()
        val cabin = cabinArea?.let { readFloat(HVAC_TEMPERATURE_CURRENT, it) }
        return HvacState(
            status = FeatureStatus.READY,
            available = leftBinding != null,
            hasCabinTemperature = cabin != null,
            cabinTemperatureCelsius = cabin ?: 0f,
            leftZone = leftBinding?.state,
            rightZone = rightBinding?.state,
            acAvailable = acArea != null,
            acOn = acArea?.let { readBoolean(HVAC_AC_ON, it) } ?: false,
        )
    }

    private fun ZoneBinding.refreshTemperature(): ZoneBinding = copy(
        state = state.copy(
            hasTemperature = true,
            temperatureCelsius = readFloat(HVAC_TEMPERATURE_SET, areaId),
        ),
    )

    private fun getConfig(propertyId: Int): Any? = propertyManager?.javaClass
        ?.getMethod("getCarPropertyConfig", Int::class.javaPrimitiveType)
        ?.invoke(propertyManager, propertyId)

    private fun getAreaIds(config: Any): IntArray {
        val areas = requireNotNull(config.javaClass.getMethod("getAreaIds").invoke(config))
        return IntArray(Array.getLength(areas)) { index -> Array.get(areas, index) as Int }
    }

    private fun getBound(config: Any, methodName: String, areaId: Int, fallback: Float): Float {
        val methods = config.javaClass.methods.filter { it.name == methodName }
        val value = runCatching {
            val areaMethod = methods.firstOrNull { it.parameterTypes.size == 1 }
            val noArgMethod = methods.firstOrNull { it.parameterTypes.isEmpty() }
            when {
                areaMethod != null -> areaMethod.invoke(config, areaId)
                noArgMethod != null -> noArgMethod.invoke(config)
                else -> null
            }
        }.getOrNull()
        return (value as? Number)?.toFloat() ?: fallback
    }

    private fun createAndRegisterCallback(hasCabin: Boolean, hasAc: Boolean) {
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
                "onErrorEvent" -> scope.launch {
                    publishUnavailable("An HVAC property reported an error")
                }
                "toString" -> "MiniIVI HVAC callback"
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
        fun registerProperty(propertyId: Int) {
            val registered = register.invoke(manager, propertyCallback, propertyId, RATE_ONCHANGE)
                as? Boolean ?: true
            check(registered) { "Unable to register HVAC property 0x${propertyId.toString(16)}" }
        }
        if (hasCabin) registerProperty(HVAC_TEMPERATURE_CURRENT)
        registerProperty(HVAC_TEMPERATURE_SET)
        if (hasAc) registerProperty(HVAC_AC_ON)
    }

    private fun handlePropertyValue(value: Any) {
        runCatching {
            val status = runCatching {
                value.javaClass.getMethod("getStatus").invoke(value) as Int
            }.getOrDefault(STATUS_AVAILABLE)
            if (status != STATUS_AVAILABLE) return
            val propertyId = value.javaClass.getMethod("getPropertyId").invoke(value) as Int
            val areaId = value.javaClass.getMethod("getAreaId").invoke(value) as Int
            val data = value.javaClass.getMethod("getValue").invoke(value)
            when (propertyId) {
                HVAC_TEMPERATURE_CURRENT -> if (areaId == cabinArea) {
                    mutableState.update {
                        it.copy(
                            hasCabinTemperature = true,
                            cabinTemperatureCelsius = (data as Number).toFloat(),
                        )
                    }
                }
                HVAC_TEMPERATURE_SET -> updateZoneTemperature(areaId, (data as Number).toFloat())
                HVAC_AC_ON -> if (areaId == acArea) {
                    mutableState.update { it.copy(acOn = data as Boolean) }
                }
            }
        }.onFailure { error -> Log.w(TAG, "Ignoring an invalid HVAC property event", error) }
    }

    private fun updateZoneTemperature(areaId: Int, temperature: Float) {
        fun ZoneBinding?.updated(): ZoneBinding? = this?.let {
            if (it.areaId == areaId) {
                it.copy(state = it.state.copy(hasTemperature = true, temperatureCelsius = temperature))
            } else {
                it
            }
        }
        leftBinding = leftBinding.updated()
        rightBinding = rightBinding.updated()
        mutableState.update {
            it.copy(
                status = FeatureStatus.READY,
                available = true,
                leftZone = leftBinding?.state,
                rightZone = rightBinding?.state,
                errorCode = CarServiceError.NONE,
                diagnosticMessage = null,
            )
        }
    }

    private fun readFloat(propertyId: Int, areaId: Int): Float =
        (propertyManager?.javaClass
            ?.getMethod("getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            ?.invoke(propertyManager, propertyId, areaId) as Number).toFloat()

    private fun readBoolean(propertyId: Int, areaId: Int): Boolean =
        propertyManager?.javaClass
            ?.getMethod("getBooleanProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            ?.invoke(propertyManager, propertyId, areaId) as Boolean

    private fun handlePlatformFailure(message: String, error: Throwable) {
        Log.e(TAG, message, error)
        mutableState.value = mutableState.value.copy(
            status = FeatureStatus.ERROR,
            available = false,
            errorCode = CarServiceError.PLATFORM_OPERATION_FAILED,
            diagnosticMessage = "$message: ${error.message}",
        )
        disconnectBlocking()
        if (started) startConnectionLoop()
    }

    private fun publishArgumentError(message: String) {
        mutableState.value = mutableState.value.copy(
            status = FeatureStatus.ERROR,
            errorCode = CarServiceError.INVALID_ARGUMENT,
            diagnosticMessage = message,
        )
    }

    private fun publishUnavailable(message: String) {
        mutableState.value = mutableState.value.copy(
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
            }.onFailure { Log.w(TAG, "Unable to unregister the HVAC callback", it) }
        }
        propertyCallback = null
        propertyManager = null
        leftBinding = null
        rightBinding = null
        cabinArea = null
        acArea = null
    }

    private companion object {
        const val TAG = "MiniIviCarHvac"
        const val CAR_CLASS = "android.car.Car"
        const val PROPERTY_SERVICE = "property"
        const val CAR_PROPERTY_CALLBACK =
            "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback"
        const val RATE_ONCHANGE = 0f
        const val STATUS_AVAILABLE = 0
        const val HVAC_TEMPERATURE_CURRENT = 0x15600502
        const val HVAC_TEMPERATURE_SET = 0x15600503
        const val HVAC_AC_ON = 0x15200505
        const val DEFAULT_MINIMUM = 16f
        const val DEFAULT_MAXIMUM = 30f
        const val INITIAL_RETRY_MILLIS = 1_000L
        const val MAX_RETRY_MILLIS = 30_000L
    }
}
