package com.miniivi.car.service.control

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.miniivi.car.api.CarServiceError
import com.miniivi.car.api.ClimateCapability
import com.miniivi.car.api.ClimateControlState
import com.miniivi.car.api.ClimateFanDirection
import com.miniivi.car.api.ClimateWindow
import com.miniivi.car.api.ClimateZoneControlState
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.HvacZone
import com.miniivi.car.api.HvacZoneState
import com.miniivi.car.api.TemperatureUnit
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

    fun syncedPassengerTemperature(syncEnabled: Boolean, driver: Float, passenger: Float): Float =
        if (syncEnabled) driver else passenger

    fun isFanDirectionAvailable(direction: Int, availableDirections: IntArray): Boolean =
        direction in availableDirections

    fun selectFrontAreas(areaIds: IntArray, driverSeat: Int?): Pair<Int?, Int?> {
        if (areaIds.isEmpty()) return null to null
        val driver = driverSeat?.takeIf { it in areaIds } ?: areaIds.first()
        val expectedPassenger = when (driver) {
            SEAT_ROW_1_LEFT -> SEAT_ROW_1_RIGHT
            SEAT_ROW_1_RIGHT -> SEAT_ROW_1_LEFT
            else -> null
        }
        val passenger = expectedPassenger?.takeIf { it in areaIds }
            ?: areaIds.firstOrNull { it != driver }
        return driver to passenger
    }

    private const val SEAT_ROW_1_LEFT = 0x1
    private const val SEAT_ROW_1_RIGHT = 0x4
}

@SuppressLint("PrivateApi")
class HvacController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private data class ZoneBinding(val areaId: Int, val state: HvacZoneState)

    private val mutableState = MutableStateFlow(HvacState())
    val state: StateFlow<HvacState> = mutableState.asStateFlow()
    private val preferences = context.createDeviceProtectedStorageContext()
        .getSharedPreferences(CLIMATE_PREFERENCES, Context.MODE_PRIVATE)
    private val mutableClimateState = MutableStateFlow(restoredClimateState())
    val climateState: StateFlow<ClimateControlState> = mutableClimateState.asStateFlow()

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
    private var driverArea: Int? = null
    private var passengerArea: Int? = null
    private val extendedConfigs = mutableMapOf<Int, Any>()

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
        mutableClimateState.value = mutableClimateState.value.copy(
            status = FeatureStatus.UNAVAILABLE,
            available = true,
            diagnosticMessage = "HVAC controller stopped",
        )
    }

    fun refresh() {
        scope.launch {
            if (propertyManager == null) return@launch
            runCatching { readCurrentState() }
                .onSuccess {
                    mutableState.value = it
                    publishClimateState()
                }
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
                val key = if (zone == HvacZone.LEFT) KEY_DRIVER_TEMPERATURE else KEY_PASSENGER_TEMPERATURE
                preferences.edit().putFloat(key, celsius.coerceIn(DEFAULT_MINIMUM, DEFAULT_MAXIMUM)).apply()
                publishClimateState()
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
                if (zone == HvacZone.LEFT && mutableClimateState.value.syncOn) {
                    rightBinding?.let { passenger ->
                        manager.javaClass.getMethod(
                            "setFloatProperty",
                            Int::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                            Float::class.javaPrimitiveType,
                        ).invoke(manager, HVAC_TEMPERATURE_SET, passenger.areaId, requested)
                        updateZoneTemperature(passenger.areaId, requested)
                    }
                }
                publishClimateState()
            }.onFailure { error -> handlePlatformFailure("Unable to set HVAC temperature", error) }
        }
    }

    fun setAcEnabled(enabled: Boolean) {
        scope.launch {
            val area = acArea
            val manager = propertyManager
            if (area == null || manager == null) {
                updateMockBoolean(KEY_AC, enabled)
                publishClimateState()
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
                updateMockBoolean(KEY_AC, enabled)
                publishClimateState()
            }.onFailure { error -> handlePlatformFailure("Unable to set A/C state", error) }
        }
    }

    fun setPowerEnabled(enabled: Boolean) = setBooleanControl(
        HVAC_POWER_ON,
        ClimateCapability.POWER,
        KEY_POWER,
        enabled,
    )

    fun setAutoEnabled(enabled: Boolean) = setBooleanControl(
        HVAC_AUTO_ON,
        ClimateCapability.AUTO,
        KEY_AUTO,
        enabled,
    )

    fun setSyncEnabled(enabled: Boolean) {
        setBooleanControl(HVAC_DUAL_ON, ClimateCapability.SYNC, KEY_SYNC, enabled)
        if (enabled) {
            val driverTemperature = mutableClimateState.value.driverZone.temperatureCelsius
            setTemperature(HvacZone.RIGHT, driverTemperature)
        }
    }

    fun setRecirculationEnabled(enabled: Boolean) = setBooleanControl(
        HVAC_RECIRC_ON,
        ClimateCapability.RECIRCULATION,
        KEY_RECIRCULATION,
        enabled,
    )

    fun setFanSpeed(zone: Int, speed: Int) = setZoneIntControl(
        propertyId = HVAC_FAN_SPEED,
        capability = ClimateCapability.FAN_SPEED,
        key = if (zone == HvacZone.LEFT) KEY_DRIVER_FAN else KEY_PASSENGER_FAN,
        zone = zone,
        value = speed,
    )

    fun setFanDirection(zone: Int, direction: Int) {
        val target = if (zone == HvacZone.LEFT) mutableClimateState.value.driverZone
        else mutableClimateState.value.passengerZone
        if (!HvacPolicy.isFanDirectionAvailable(direction, target.availableFanDirections)) {
            publishArgumentError("Requested fan direction is unavailable")
            return
        }
        setZoneIntControl(
            propertyId = HVAC_FAN_DIRECTION,
            capability = ClimateCapability.FAN_DIRECTION,
            key = if (zone == HvacZone.LEFT) KEY_DRIVER_DIRECTION else KEY_PASSENGER_DIRECTION,
            zone = zone,
            value = direction,
        )
    }

    fun setDefrosterEnabled(window: Int, enabled: Boolean) {
        val propertyId = if (window == ClimateWindow.FRONT) HVAC_DEFROSTER
        else HVAC_ELECTRIC_DEFROSTER_ON
        val capability = if (window == ClimateWindow.FRONT) ClimateCapability.FRONT_DEFROST
        else ClimateCapability.REAR_DEFROST
        val key = if (window == ClimateWindow.FRONT) KEY_FRONT_DEFROST else KEY_REAR_DEFROST
        setBooleanControl(propertyId, capability, key, enabled, useFirstPropertyArea = true)
    }

    fun setSeatHeatingLevel(zone: Int, level: Int) = setZoneIntControl(
        HVAC_SEAT_TEMPERATURE,
        ClimateCapability.SEAT_HEATING,
        if (zone == HvacZone.LEFT) KEY_DRIVER_HEAT else KEY_PASSENGER_HEAT,
        zone,
        level,
    )

    fun setSeatVentilationLevel(zone: Int, level: Int) = setZoneIntControl(
        HVAC_SEAT_VENTILATION,
        ClimateCapability.SEAT_VENTILATION,
        if (zone == HvacZone.LEFT) KEY_DRIVER_VENT else KEY_PASSENGER_VENT,
        zone,
        level,
    )

    fun setMaxAcEnabled(enabled: Boolean) = setBooleanControl(
        HVAC_MAX_AC_ON,
        ClimateCapability.MAX_AC,
        KEY_MAX_AC,
        enabled,
    )

    fun setMaxDefrostEnabled(enabled: Boolean) = setBooleanControl(
        HVAC_MAX_DEFROST_ON,
        ClimateCapability.MAX_DEFROST,
        KEY_MAX_DEFROST,
        enabled,
    )

    fun setAutoRecirculationEnabled(enabled: Boolean) = setBooleanControl(
        HVAC_AUTO_RECIRC_ON,
        ClimateCapability.AUTO_RECIRCULATION,
        KEY_AUTO_RECIRCULATION,
        enabled,
    )

    fun setSteeringWheelHeatLevel(level: Int) = setGlobalIntControl(
        HVAC_STEERING_WHEEL_HEAT,
        ClimateCapability.STEERING_WHEEL_HEAT,
        KEY_STEERING_HEAT,
        level,
    )

    fun setTemperatureUnit(unit: Int) {
        if (unit != TemperatureUnit.CELSIUS && unit != TemperatureUnit.FAHRENHEIT) {
            publishArgumentError("Unsupported temperature unit")
            return
        }
        setGlobalIntControl(
            HVAC_TEMPERATURE_DISPLAY_UNITS,
            ClimateCapability.TEMPERATURE_UNIT,
            KEY_TEMPERATURE_UNIT,
            unit,
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
        val driverSeat = getConfig(INFO_DRIVER_SEAT)?.let {
            runCatching { readInt(INFO_DRIVER_SEAT, getAreaIds(it).firstOrNull() ?: 0) }.getOrNull()
        }
        val (leftArea, rightArea) = setConfig?.let {
            HvacPolicy.selectFrontAreas(getAreaIds(it), driverSeat)
        } ?: (null to null)
        driverArea = leftArea ?: driverSeat
        passengerArea = rightArea
        leftBinding = if (setConfig != null && leftArea != null) {
            createZoneBinding(HvacZone.LEFT, leftArea, setConfig)
        } else null
        rightBinding = if (setConfig != null && rightArea != null) {
            createZoneBinding(HvacZone.RIGHT, rightArea, setConfig)
        } else null
        val currentConfig = getConfig(HVAC_TEMPERATURE_CURRENT)
        val acConfig = getConfig(HVAC_AC_ON)
        cabinArea = currentConfig?.let(::getAreaIds)?.firstOrNull()
        acArea = acConfig?.let(::getAreaIds)?.firstOrNull()
        extendedConfigs.clear()
        EXTENDED_PROPERTIES.forEach { propertyId ->
            getConfig(propertyId)?.let { extendedConfigs[propertyId] = it }
        }

        mutableState.value = readCurrentState()
        publishClimateState()
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
            available = true,
            hasCabinTemperature = cabin != null,
            cabinTemperatureCelsius = cabin ?: 0f,
            leftZone = leftBinding?.state,
            rightZone = rightBinding?.state,
            acAvailable = true,
            acOn = acArea?.let { runCatching { readBoolean(HVAC_AC_ON, it) }.getOrNull() }
                ?: preferences.getBoolean(KEY_AC, true),
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
        if (leftBinding != null) registerProperty(HVAC_TEMPERATURE_SET)
        if (hasAc) registerProperty(HVAC_AC_ON)
        extendedConfigs.keys.filterNot {
            it == HVAC_TEMPERATURE_CURRENT || it == HVAC_TEMPERATURE_SET || it == HVAC_AC_ON
        }.forEach(::registerProperty)
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
            publishClimateState()
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
        publishClimateState()
    }

    private fun readFloat(propertyId: Int, areaId: Int): Float =
        (propertyManager?.javaClass
            ?.getMethod("getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            ?.invoke(propertyManager, propertyId, areaId) as Number).toFloat()

    private fun readBoolean(propertyId: Int, areaId: Int): Boolean =
        propertyManager?.javaClass
            ?.getMethod("getBooleanProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            ?.invoke(propertyManager, propertyId, areaId) as Boolean

    private fun readInt(propertyId: Int, areaId: Int): Int =
        propertyManager?.javaClass
            ?.getMethod("getIntProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            ?.invoke(propertyManager, propertyId, areaId) as Int

    private fun setBooleanControl(
        propertyId: Int,
        capability: Long,
        key: String,
        enabled: Boolean,
        useFirstPropertyArea: Boolean = false,
    ) {
        scope.launch {
            val config = extendedConfigs[propertyId]
            val manager = propertyManager
            if (config == null || manager == null) {
                updateMockBoolean(key, enabled)
                publishClimateState()
                return@launch
            }
            val area = if (useFirstPropertyArea) getAreaIds(config).firstOrNull()
            else preferredArea(config)
            if (area == null) {
                updateMockBoolean(key, enabled)
                publishClimateState()
                return@launch
            }
            runCatching {
                manager.javaClass.getMethod(
                    "setBooleanProperty",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                ).invoke(manager, propertyId, area, enabled)
            }.onSuccess { publishClimateState() }
                .onFailure { handleExtendedFailure("Unable to change climate control", it, capability) }
        }
    }

    private fun setZoneIntControl(
        propertyId: Int,
        capability: Long,
        key: String,
        zone: Int,
        value: Int,
    ) {
        if (zone != HvacZone.LEFT && zone != HvacZone.RIGHT) {
            publishArgumentError("Invalid climate zone")
            return
        }
        scope.launch {
            val config = extendedConfigs[propertyId]
            val manager = propertyManager
            val requested = if (config != null) {
                value.coerceIn(
                    getBound(config, "getMinValue", zoneArea(zone) ?: 0, 0f).toInt(),
                    getBound(config, "getMaxValue", zoneArea(zone) ?: 0, 7f).toInt(),
                )
            } else value.coerceIn(0, 7)
            val area = config?.let { zoneArea(zone)?.takeIf { target -> target in getAreaIds(it) } }
            if (config == null || manager == null || area == null) {
                preferences.edit().putInt(key, requested).apply()
                publishClimateState()
                return@launch
            }
            runCatching {
                manager.javaClass.getMethod(
                    "setIntProperty",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                ).invoke(manager, propertyId, area, requested)
            }.onSuccess { publishClimateState() }
                .onFailure { handleExtendedFailure("Unable to change zoned climate control", it, capability) }
        }
    }

    private fun setGlobalIntControl(propertyId: Int, capability: Long, key: String, value: Int) {
        scope.launch {
            val config = extendedConfigs[propertyId]
            val manager = propertyManager
            val area = config?.let(::getAreaIds)?.firstOrNull() ?: 0
            if (config == null || manager == null) {
                preferences.edit().putInt(key, value).apply()
                publishClimateState()
                return@launch
            }
            runCatching {
                manager.javaClass.getMethod(
                    "setIntProperty",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                ).invoke(manager, propertyId, area, value)
            }.onSuccess { publishClimateState() }
                .onFailure { handleExtendedFailure("Unable to change global climate control", it, capability) }
        }
    }

    private fun publishClimateState() {
        val old = mutableClimateState.value
        val capabilities = realCapabilities()
        val left = leftBinding?.state
        val right = rightBinding?.state
        mutableClimateState.value = ClimateControlState(
            status = FeatureStatus.READY,
            available = true,
            powerOn = readBooleanOrMock(HVAC_POWER_ON, KEY_POWER, true),
            acOn = acArea?.let { runCatching { readBoolean(HVAC_AC_ON, it) }.getOrNull() }
                ?: preferences.getBoolean(KEY_AC, true),
            autoOn = readBooleanOrMock(HVAC_AUTO_ON, KEY_AUTO, true),
            syncOn = readBooleanOrMock(HVAC_DUAL_ON, KEY_SYNC, false),
            recirculationOn = readBooleanOrMock(HVAC_RECIRC_ON, KEY_RECIRCULATION, false),
            hasCabinTemperature = mutableState.value.hasCabinTemperature,
            cabinTemperatureCelsius = mutableState.value.cabinTemperatureCelsius.takeIf {
                mutableState.value.hasCabinTemperature
            } ?: 25f,
            driverZone = zoneControlState(HvacZone.LEFT, left, old.driverZone),
            passengerZone = zoneControlState(HvacZone.RIGHT, right, old.passengerZone),
            frontDefrostOn = readBooleanOrMock(HVAC_DEFROSTER, KEY_FRONT_DEFROST, false, true),
            rearDefrostOn = readBooleanOrMock(
                HVAC_ELECTRIC_DEFROSTER_ON,
                KEY_REAR_DEFROST,
                false,
                true,
            ),
            maxAcOn = readBooleanOrMock(HVAC_MAX_AC_ON, KEY_MAX_AC, false),
            maxDefrostOn = readBooleanOrMock(HVAC_MAX_DEFROST_ON, KEY_MAX_DEFROST, false),
            autoRecirculationOn = readBooleanOrMock(
                HVAC_AUTO_RECIRC_ON,
                KEY_AUTO_RECIRCULATION,
                false,
            ),
            steeringWheelHeatLevel = readIntOrMock(
                HVAC_STEERING_WHEEL_HEAT,
                KEY_STEERING_HEAT,
                0,
            ),
            maximumSteeringWheelHeatLevel = 3,
            temperatureUnit = readIntOrMock(
                HVAC_TEMPERATURE_DISPLAY_UNITS,
                KEY_TEMPERATURE_UNIT,
                TemperatureUnit.CELSIUS,
            ),
            realCapabilities = capabilities,
            errorCode = CarServiceError.NONE,
            diagnosticMessage = null,
        )
    }

    private fun zoneControlState(
        zone: Int,
        legacy: HvacZoneState?,
        previous: ClimateZoneControlState,
    ): ClimateZoneControlState {
        val area = zoneArea(zone)
        val fanConfig = extendedConfigs[HVAC_FAN_SPEED]
        val directionConfig = extendedConfigs[HVAC_FAN_DIRECTION]
        val heatConfig = extendedConfigs[HVAC_SEAT_TEMPERATURE]
        val ventConfig = extendedConfigs[HVAC_SEAT_VENTILATION]
        val fanDirections = availableDirections(area, previous.availableFanDirections)
        return previous.copy(
            zone = zone,
            temperatureCelsius = legacy?.temperatureCelsius ?: preferences.getFloat(
                if (zone == HvacZone.LEFT) KEY_DRIVER_TEMPERATURE else KEY_PASSENGER_TEMPERATURE,
                if (zone == HvacZone.LEFT) 22f else 22.5f,
            ),
            minimumCelsius = legacy?.minimumCelsius ?: DEFAULT_MINIMUM,
            maximumCelsius = legacy?.maximumCelsius ?: DEFAULT_MAXIMUM,
            fanSpeed = readZoneIntOrMock(
                HVAC_FAN_SPEED,
                area,
                if (zone == HvacZone.LEFT) KEY_DRIVER_FAN else KEY_PASSENGER_FAN,
                4,
            ),
            minimumFanSpeed = fanConfig?.let { getBound(it, "getMinValue", area ?: 0, 0f).toInt() } ?: 0,
            maximumFanSpeed = fanConfig?.let { getBound(it, "getMaxValue", area ?: 0, 7f).toInt() } ?: 7,
            fanDirection = readZoneIntOrMock(
                HVAC_FAN_DIRECTION,
                area,
                if (zone == HvacZone.LEFT) KEY_DRIVER_DIRECTION else KEY_PASSENGER_DIRECTION,
                ClimateFanDirection.FACE,
            ),
            availableFanDirections = fanDirections,
            seatHeatingLevel = readZoneIntOrMock(
                HVAC_SEAT_TEMPERATURE,
                area,
                if (zone == HvacZone.LEFT) KEY_DRIVER_HEAT else KEY_PASSENGER_HEAT,
                0,
            ),
            maximumSeatHeatingLevel = heatConfig?.let {
                getBound(it, "getMaxValue", area ?: 0, 3f).toInt().coerceAtLeast(0)
            } ?: 3,
            seatVentilationLevel = readZoneIntOrMock(
                HVAC_SEAT_VENTILATION,
                area,
                if (zone == HvacZone.LEFT) KEY_DRIVER_VENT else KEY_PASSENGER_VENT,
                0,
            ),
            maximumSeatVentilationLevel = ventConfig?.let {
                getBound(it, "getMaxValue", area ?: 0, 3f).toInt().coerceAtLeast(0)
            } ?: 3,
        )
    }

    private fun availableDirections(area: Int?, fallback: IntArray): IntArray {
        val config = extendedConfigs[HVAC_FAN_DIRECTION_AVAILABLE] ?: return fallback
        val target = area ?: getAreaIds(config).firstOrNull() ?: return fallback
        return runCatching {
            val value = propertyManager?.javaClass?.getMethod(
                "getProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )?.invoke(propertyManager, HVAC_FAN_DIRECTION_AVAILABLE, target)
            val data = value?.javaClass?.getMethod("getValue")?.invoke(value)
            when (data) {
                is IntArray -> data
                is List<*> -> data.mapNotNull { (it as? Number)?.toInt() }.toIntArray()
                else -> fallback
            }
        }.getOrDefault(fallback)
    }

    private fun readBooleanOrMock(
        propertyId: Int,
        key: String,
        default: Boolean,
        firstArea: Boolean = false,
    ): Boolean {
        val config = extendedConfigs[propertyId]
        val area = if (firstArea) config?.let(::getAreaIds)?.firstOrNull()
        else config?.let(::preferredArea)
        return if (config != null && area != null) {
            runCatching { readBoolean(propertyId, area) }.getOrElse { preferences.getBoolean(key, default) }
        } else preferences.getBoolean(key, default)
    }

    private fun readIntOrMock(propertyId: Int, key: String, default: Int): Int {
        val config = extendedConfigs[propertyId]
        val area = config?.let(::getAreaIds)?.firstOrNull() ?: 0
        return if (config != null) runCatching { readInt(propertyId, area) }
            .getOrElse { preferences.getInt(key, default) }
        else preferences.getInt(key, default)
    }

    private fun readZoneIntOrMock(propertyId: Int, area: Int?, key: String, default: Int): Int {
        val config = extendedConfigs[propertyId]
        return if (config != null && area != null && area in getAreaIds(config)) {
            runCatching { readInt(propertyId, area) }.getOrElse { preferences.getInt(key, default) }
        } else preferences.getInt(key, default)
    }

    private fun preferredArea(config: Any): Int? {
        val areas = getAreaIds(config)
        return driverArea?.takeIf { it in areas } ?: areas.firstOrNull()
    }

    private fun zoneArea(zone: Int): Int? = if (zone == HvacZone.LEFT) driverArea else passengerArea

    private fun realCapabilities(): Long {
        var result = 0L
        CAPABILITY_PROPERTIES.forEach { (capability, propertyId) ->
            if (extendedConfigs.containsKey(propertyId) || getConfig(propertyId) != null) {
                result = result or capability
            }
        }
        if (acArea != null) result = result or ClimateCapability.AC
        return result
    }

    private fun updateMockBoolean(key: String, enabled: Boolean) {
        preferences.edit().putBoolean(key, enabled).apply()
    }

    private fun handleExtendedFailure(message: String, error: Throwable, capability: Long) {
        Log.e(TAG, "$message for capability $capability", error)
        mutableClimateState.update {
            it.copy(
                status = FeatureStatus.ERROR,
                errorCode = CarServiceError.PLATFORM_OPERATION_FAILED,
                diagnosticMessage = "$message: ${error.message}",
            )
        }
    }

    private fun restoredClimateState() = ClimateControlState(
        status = FeatureStatus.READY,
        available = true,
        powerOn = preferences.getBoolean(KEY_POWER, true),
        acOn = preferences.getBoolean(KEY_AC, true),
        autoOn = preferences.getBoolean(KEY_AUTO, true),
        syncOn = preferences.getBoolean(KEY_SYNC, false),
        recirculationOn = preferences.getBoolean(KEY_RECIRCULATION, false),
        driverZone = ClimateZoneControlState(
            zone = HvacZone.LEFT,
            temperatureCelsius = preferences.getFloat(KEY_DRIVER_TEMPERATURE, 22f),
            fanSpeed = preferences.getInt(KEY_DRIVER_FAN, 4),
        ),
        passengerZone = ClimateZoneControlState(
            zone = HvacZone.RIGHT,
            temperatureCelsius = preferences.getFloat(KEY_PASSENGER_TEMPERATURE, 22.5f),
            fanSpeed = preferences.getInt(KEY_PASSENGER_FAN, 4),
        ),
        temperatureUnit = preferences.getInt(KEY_TEMPERATURE_UNIT, TemperatureUnit.CELSIUS),
    )

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
        driverArea = null
        passengerArea = null
        extendedConfigs.clear()
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
        const val HVAC_MAX_AC_ON = 0x15200506
        const val HVAC_MAX_DEFROST_ON = 0x15200507
        const val HVAC_RECIRC_ON = 0x15200508
        const val HVAC_DUAL_ON = 0x15200509
        const val HVAC_AUTO_ON = 0x1520050A
        const val HVAC_POWER_ON = 0x15200510
        const val HVAC_AUTO_RECIRC_ON = 0x15200512
        const val HVAC_FAN_SPEED = 0x15400500
        const val HVAC_FAN_DIRECTION = 0x15400501
        const val HVAC_DEFROSTER = 0x13200504
        const val HVAC_STEERING_WHEEL_HEAT = 0x1140050D
        const val HVAC_TEMPERATURE_DISPLAY_UNITS = 0x1140050E
        const val HVAC_SEAT_TEMPERATURE = 0x1540050B
        const val HVAC_ELECTRIC_DEFROSTER_ON = 0x13200514
        const val HVAC_SEAT_VENTILATION = 0x15400513
        const val HVAC_FAN_DIRECTION_AVAILABLE = 0x15410511
        const val INFO_DRIVER_SEAT = 0x1540010A
        const val DEFAULT_MINIMUM = 16f
        const val DEFAULT_MAXIMUM = 30f
        const val INITIAL_RETRY_MILLIS = 1_000L
        const val MAX_RETRY_MILLIS = 30_000L
        const val CLIMATE_PREFERENCES = "climate_controls"
        const val KEY_POWER = "power"
        const val KEY_AC = "ac"
        const val KEY_AUTO = "auto"
        const val KEY_SYNC = "sync"
        const val KEY_RECIRCULATION = "recirculation"
        const val KEY_DRIVER_TEMPERATURE = "driver_temperature"
        const val KEY_PASSENGER_TEMPERATURE = "passenger_temperature"
        const val KEY_DRIVER_FAN = "driver_fan"
        const val KEY_PASSENGER_FAN = "passenger_fan"
        const val KEY_DRIVER_DIRECTION = "driver_direction"
        const val KEY_PASSENGER_DIRECTION = "passenger_direction"
        const val KEY_FRONT_DEFROST = "front_defrost"
        const val KEY_REAR_DEFROST = "rear_defrost"
        const val KEY_DRIVER_HEAT = "driver_heat"
        const val KEY_PASSENGER_HEAT = "passenger_heat"
        const val KEY_DRIVER_VENT = "driver_vent"
        const val KEY_PASSENGER_VENT = "passenger_vent"
        const val KEY_MAX_AC = "max_ac"
        const val KEY_MAX_DEFROST = "max_defrost"
        const val KEY_AUTO_RECIRCULATION = "auto_recirculation"
        const val KEY_STEERING_HEAT = "steering_heat"
        const val KEY_TEMPERATURE_UNIT = "temperature_unit"
        val EXTENDED_PROPERTIES = intArrayOf(
            HVAC_POWER_ON,
            HVAC_AUTO_ON,
            HVAC_DUAL_ON,
            HVAC_RECIRC_ON,
            HVAC_FAN_SPEED,
            HVAC_FAN_DIRECTION,
            HVAC_FAN_DIRECTION_AVAILABLE,
            HVAC_DEFROSTER,
            HVAC_ELECTRIC_DEFROSTER_ON,
            HVAC_SEAT_TEMPERATURE,
            HVAC_SEAT_VENTILATION,
            HVAC_MAX_AC_ON,
            HVAC_MAX_DEFROST_ON,
            HVAC_AUTO_RECIRC_ON,
            HVAC_STEERING_WHEEL_HEAT,
            HVAC_TEMPERATURE_DISPLAY_UNITS,
        )
        val CAPABILITY_PROPERTIES = mapOf(
            ClimateCapability.POWER to HVAC_POWER_ON,
            ClimateCapability.AUTO to HVAC_AUTO_ON,
            ClimateCapability.SYNC to HVAC_DUAL_ON,
            ClimateCapability.RECIRCULATION to HVAC_RECIRC_ON,
            ClimateCapability.FAN_SPEED to HVAC_FAN_SPEED,
            ClimateCapability.FAN_DIRECTION to HVAC_FAN_DIRECTION,
            ClimateCapability.FRONT_DEFROST to HVAC_DEFROSTER,
            ClimateCapability.REAR_DEFROST to HVAC_ELECTRIC_DEFROSTER_ON,
            ClimateCapability.SEAT_HEATING to HVAC_SEAT_TEMPERATURE,
            ClimateCapability.SEAT_VENTILATION to HVAC_SEAT_VENTILATION,
            ClimateCapability.MAX_AC to HVAC_MAX_AC_ON,
            ClimateCapability.MAX_DEFROST to HVAC_MAX_DEFROST_ON,
            ClimateCapability.AUTO_RECIRCULATION to HVAC_AUTO_RECIRC_ON,
            ClimateCapability.STEERING_WHEEL_HEAT to HVAC_STEERING_WHEEL_HEAT,
            ClimateCapability.TEMPERATURE_UNIT to HVAC_TEMPERATURE_DISPLAY_UNITS,
        )
    }
}
