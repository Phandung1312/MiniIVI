package com.android.car.systemui.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.model.TemperatureZone
import java.lang.reflect.Array
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object HvacTemperaturePolicy {
    fun adjust(zone: TemperatureZone, delta: Float): Float =
        ((zone.temperature ?: return zone.minimum) + delta).coerceIn(zone.minimum, zone.maximum)
}

@SuppressLint("PrivateApi")
class AndroidHvacRepository(private val context: Context) : HvacRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(HvacState())
    override val state = mutableState.asStateFlow()

    private var connectionJob: Job? = null
    private var car: Any? = null
    private var propertyManager: Any? = null
    private var propertyCallback: Any? = null
    private var cabinArea: Int? = null
    private var acArea: Int? = null

    override fun start() {
        if (connectionJob?.isActive == true || propertyManager != null) return
        mutableState.value = HvacState(connecting = true)
        connectionJob = scope.launch { connectBlocking() }
    }

    override fun stop() {
        connectionJob?.cancel()
        connectionJob = null
        disconnectBlocking()
        mutableState.value = HvacState(connecting = false)
    }

    private fun connectBlocking() {
        runCatching {
            val carClass = Class.forName("android.car.Car")
            car = carClass.getMethod("createCar", Context::class.java).invoke(null, context)
            propertyManager = carClass.getMethod("getCarManager", String::class.java)
                .invoke(car, PROPERTY_SERVICE)
                ?: error("No car property service")

            val setConfig = getConfig(HVAC_TEMPERATURE_SET)
                ?: error("No HVAC temperature property")
            val setAreas = getAreaIds(setConfig)
            check(setAreas.isNotEmpty()) { "No HVAC temperature zones" }

            val leftArea = setAreas.first()
            val rightArea = setAreas.last().takeIf { it != leftArea }
            val currentConfig = getConfig(HVAC_TEMPERATURE_CURRENT)
            val acConfig = getConfig(HVAC_AC_ON)
            cabinArea = currentConfig?.let(::getAreaIds)?.firstOrNull()
            acArea = acConfig?.let(::getAreaIds)?.firstOrNull()

            val leftZone = TemperatureZone(
                areaId = leftArea,
                temperature = readFloat(HVAC_TEMPERATURE_SET, leftArea),
                minimum = getBound(setConfig, "getMinValue", leftArea, DEFAULT_MINIMUM),
                maximum = getBound(setConfig, "getMaxValue", leftArea, DEFAULT_MAXIMUM),
            )
            val rightZone = rightArea?.let { area ->
                TemperatureZone(
                    areaId = area,
                    temperature = readFloat(HVAC_TEMPERATURE_SET, area),
                    minimum = getBound(setConfig, "getMinValue", area, DEFAULT_MINIMUM),
                    maximum = getBound(setConfig, "getMaxValue", area, DEFAULT_MAXIMUM),
                )
            }
            mutableState.value = HvacState(
                connecting = false,
                available = true,
                cabinTemperature = cabinArea?.let { readFloat(HVAC_TEMPERATURE_CURRENT, it) },
                leftZone = leftZone,
                rightZone = rightZone,
                acAvailable = acArea != null,
                acOn = acArea?.let { readBoolean(HVAC_AC_ON, it) } ?: false,
            )
            createAndRegisterCallback(currentConfig != null, acConfig != null)
        }.onFailure { error ->
            Log.e(TAG, "Cannot connect to AAOS climate properties", error)
            disconnectBlocking()
            mutableState.value = HvacState(
                connecting = false,
                available = false,
                errorMessage = error.message,
            )
        }
    }

    private fun getConfig(propertyId: Int): Any? = propertyManager?.javaClass
        ?.getMethod("getCarPropertyConfig", Int::class.javaPrimitiveType)
        ?.invoke(propertyManager, propertyId)

    private fun getAreaIds(config: Any): IntArray {
        val areas = requireNotNull(config.javaClass.getMethod("getAreaIds").invoke(config))
        return IntArray(Array.getLength(areas)) { index -> Array.get(areas, index) as Int }
    }

    private fun getBound(
        config: Any,
        methodName: String,
        areaId: Int,
        fallback: Float,
    ): Float {
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
        val manager = propertyManager ?: return
        val callbackClass = Class.forName(
            "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
        )
        propertyCallback = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass),
            java.lang.reflect.InvocationHandler { proxy, method, args ->
            when (method.name) {
                "onChangeEvent" -> args?.firstOrNull()?.let(::handlePropertyValue)
                "toString" -> "CarSystemUI HVAC callback"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        },
        )
        val register = manager.javaClass.getMethod(
            "registerCallback",
            callbackClass,
            Int::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
        )
        fun registerProperty(propertyId: Int) {
            val registered = register.invoke(
                manager,
                propertyCallback,
                propertyId,
                RATE_ONCHANGE,
            ) as? Boolean ?: true
            check(registered) { "Cannot register HVAC property 0x${propertyId.toString(16)}" }
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
            mutableState.update { current ->
                when (propertyId) {
                    HVAC_TEMPERATURE_CURRENT -> if (areaId == cabinArea) {
                        current.copy(cabinTemperature = (data as Number).toFloat())
                    } else current

                    HVAC_TEMPERATURE_SET -> current.copy(
                        leftZone = current.leftZone.updateTemperature(areaId, data),
                        rightZone = current.rightZone.updateTemperature(areaId, data),
                    )

                    HVAC_AC_ON -> if (areaId == acArea) current.copy(acOn = data as Boolean)
                    else current
                    else -> current
                }
            }
        }.onFailure { error -> Log.w(TAG, "Invalid climate property event", error) }
    }

    private fun TemperatureZone?.updateTemperature(areaId: Int, value: Any?): TemperatureZone? =
        this?.takeIf { it.areaId == areaId }?.copy(temperature = (value as Number).toFloat()) ?: this

    private fun readFloat(propertyId: Int, areaId: Int): Float =
        (propertyManager?.javaClass
            ?.getMethod("getFloatProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            ?.invoke(propertyManager, propertyId, areaId) as Number).toFloat()

    private fun readBoolean(propertyId: Int, areaId: Int): Boolean =
        propertyManager?.javaClass
            ?.getMethod("getBooleanProperty", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            ?.invoke(propertyManager, propertyId, areaId) as Boolean

    override fun refresh() {
        scope.launch {
            val current = mutableState.value
            if (!current.available || propertyManager == null) return@launch
            runCatching {
                current.copy(
                    cabinTemperature = cabinArea?.let { readFloat(HVAC_TEMPERATURE_CURRENT, it) },
                    leftZone = current.leftZone?.let {
                        it.copy(temperature = readFloat(HVAC_TEMPERATURE_SET, it.areaId))
                    },
                    rightZone = current.rightZone?.let {
                        it.copy(temperature = readFloat(HVAC_TEMPERATURE_SET, it.areaId))
                    },
                    acOn = acArea?.let { readBoolean(HVAC_AC_ON, it) } ?: current.acOn,
                    errorMessage = null,
                )
            }.onSuccess { mutableState.value = it }
                .onFailure { error ->
                    Log.w(TAG, "Cannot refresh HVAC state", error)
                    mutableState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    override fun adjustTemperature(zone: ClimateZone, delta: Float) {
        val current = mutableState.value
        val target = when (zone) {
            ClimateZone.LEFT -> current.leftZone
            ClimateZone.RIGHT -> current.rightZone ?: current.leftZone
        } ?: return
        if (target.temperature == null) return
        val requested = HvacTemperaturePolicy.adjust(target, delta)
        scope.launch { setTemperature(target.areaId, requested) }
    }

    private fun setTemperature(areaId: Int, temperature: Float) {
        val manager = propertyManager ?: return
        runCatching {
            manager.javaClass.getMethod(
                "setFloatProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            ).invoke(manager, HVAC_TEMPERATURE_SET, areaId, temperature)
            mutableState.update { current ->
                current.copy(
                    leftZone = current.leftZone.optimisticTemperature(areaId, temperature),
                    rightZone = current.rightZone.optimisticTemperature(areaId, temperature),
                    errorMessage = null,
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Cannot set HVAC temperature", error)
            mutableState.update { it.copy(errorMessage = error.message) }
        }
    }

    private fun TemperatureZone?.optimisticTemperature(
        areaId: Int,
        temperature: Float,
    ): TemperatureZone? = if (this?.areaId == areaId) copy(temperature = temperature) else this

    override fun setAc(enabled: Boolean) {
        val area = acArea ?: return
        val manager = propertyManager ?: return
        scope.launch {
            runCatching {
                manager.javaClass.getMethod(
                    "setBooleanProperty",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                ).invoke(manager, HVAC_AC_ON, area, enabled)
                mutableState.update { it.copy(acOn = enabled, errorMessage = null) }
            }.onFailure { error ->
                Log.e(TAG, "Cannot set A/C state", error)
                mutableState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    private fun disconnectBlocking() {
        val manager = propertyManager
        val callback = propertyCallback
        if (manager != null && callback != null) {
            runCatching {
                val unregister: Method? = manager.javaClass.methods.firstOrNull {
                    it.name == "unregisterCallback" && it.parameterTypes.size == 1
                }
                unregister?.invoke(manager, callback)
            }.onFailure { Log.w(TAG, "Cannot unregister HVAC callback", it) }
        }
        runCatching { car?.javaClass?.getMethod("disconnect")?.invoke(car) }
        propertyCallback = null
        propertyManager = null
        car = null
        cabinArea = null
        acArea = null
    }

    private companion object {
        const val TAG = "CarSystemUI-HVAC"
        const val PROPERTY_SERVICE = "property"
        const val RATE_ONCHANGE = 0f
        const val STATUS_AVAILABLE = 0
        const val HVAC_TEMPERATURE_CURRENT = 0x15600502
        const val HVAC_TEMPERATURE_SET = 0x15600503
        const val HVAC_AC_ON = 0x15200505
        const val DEFAULT_MINIMUM = 16f
        const val DEFAULT_MAXIMUM = 30f
    }
}
