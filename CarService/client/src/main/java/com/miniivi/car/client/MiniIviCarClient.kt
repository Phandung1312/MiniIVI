package com.miniivi.car.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.miniivi.car.api.AudioState
import com.miniivi.car.api.BluetoothFeatureState
import com.miniivi.car.api.BrightnessState
import com.miniivi.car.api.CarFeature
import com.miniivi.car.api.CarServiceContract
import com.miniivi.car.api.ClimateControlState
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.IAudioStateListener
import com.miniivi.car.api.IBluetoothFeatureStateListener
import com.miniivi.car.api.IBrightnessStateListener
import com.miniivi.car.api.IHvacStateListener
import com.miniivi.car.api.IClimateControlStateListener
import com.miniivi.car.api.IMiniIviCarService
import com.miniivi.car.api.IQuickControlsStateListener
import com.miniivi.car.api.IVehicleStatusListener
import com.miniivi.car.api.VehicleStatusState
import com.miniivi.car.api.QuickControlsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MiniIviCarClient(context: Context) {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val mutableBrightnessState = MutableStateFlow(BrightnessState())
    val brightnessState: StateFlow<BrightnessState> = mutableBrightnessState.asStateFlow()

    private val mutableAudioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = mutableAudioState.asStateFlow()

    private val mutableHvacState = MutableStateFlow(HvacState())
    val hvacState: StateFlow<HvacState> = mutableHvacState.asStateFlow()

    private val mutableVehicleStatusState = MutableStateFlow(VehicleStatusState())
    val vehicleStatusState: StateFlow<VehicleStatusState> = mutableVehicleStatusState.asStateFlow()

    private val mutableClimateControlState = MutableStateFlow(ClimateControlState())
    val climateControlState: StateFlow<ClimateControlState> = mutableClimateControlState.asStateFlow()

    private val mutableQuickControlsState = MutableStateFlow(QuickControlsState())
    val quickControlsState: StateFlow<QuickControlsState> = mutableQuickControlsState.asStateFlow()

    private val mutableBluetoothState = MutableStateFlow(BluetoothFeatureState())
    val bluetoothState: StateFlow<BluetoothFeatureState> = mutableBluetoothState.asStateFlow()

    @Volatile private var remote: IMiniIviCarService? = null
    @Volatile private var remoteApiVersion = 0
    private var started = false
    private var binding = false
    private var bound = false
    private var lastLoggedRetryDelay = -1L
    private val retryBackoff = RetryBackoff(INITIAL_RETRY_MILLIS, MAX_RETRY_MILLIS)

    private val brightnessListener = object : IBrightnessStateListener.Stub() {
        override fun onBrightnessStateChanged(state: BrightnessState) {
            logStateTransition(
                "brightness",
                mutableBrightnessState.value.status,
                mutableBrightnessState.value.available,
                state.status,
                state.available,
            )
            mutableBrightnessState.value = state
        }
    }

    private val audioListener = object : IAudioStateListener.Stub() {
        override fun onAudioStateChanged(state: AudioState) {
            logStateTransition(
                "audio",
                mutableAudioState.value.status,
                mutableAudioState.value.available,
                state.status,
                state.available,
            )
            mutableAudioState.value = state
        }
    }

    private val hvacListener = object : IHvacStateListener.Stub() {
        override fun onHvacStateChanged(state: HvacState) {
            logStateTransition(
                "hvac",
                mutableHvacState.value.status,
                mutableHvacState.value.available,
                state.status,
                state.available,
            )
            mutableHvacState.value = state
        }
    }

    private val vehicleStatusListener = object : IVehicleStatusListener.Stub() {
        override fun onVehicleStatusChanged(state: VehicleStatusState) {
            logStateTransition(
                "vehicle_status",
                mutableVehicleStatusState.value.status,
                mutableVehicleStatusState.value.available,
                state.status,
                state.available,
            )
            mutableVehicleStatusState.value = state
        }
    }

    private val climateControlListener = object : IClimateControlStateListener.Stub() {
        override fun onClimateControlStateChanged(state: ClimateControlState) {
            logStateTransition(
                "climate_control",
                mutableClimateControlState.value.status,
                mutableClimateControlState.value.available,
                state.status,
                state.available,
            )
            mutableClimateControlState.value = state
        }
    }

    private val quickControlsListener = object : IQuickControlsStateListener.Stub() {
        override fun onQuickControlsStateChanged(state: QuickControlsState) {
            logStateTransition(
                "quick_controls",
                mutableQuickControlsState.value.status,
                mutableQuickControlsState.value.available,
                state.status,
                state.available,
            )
            mutableQuickControlsState.value = state
        }
    }

    private val bluetoothListener = object : IBluetoothFeatureStateListener.Stub() {
        override fun onBluetoothFeatureStateChanged(state: BluetoothFeatureState) {
            logStateTransition(
                "bluetooth",
                mutableBluetoothState.value.status,
                mutableBluetoothState.value.available,
                state.status,
                state.available,
            )
            mutableBluetoothState.value = state
        }
    }

    private val deathRecipient = IBinder.DeathRecipient {
        Log.w(TAG, "event=binder_died service=car_control")
        mainHandler.post { resetBindingAndRetry("Car control service binder died") }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            Log.i(TAG, "event=service_connected component=${name.flattenToShortString()}")
            binding = false
            bound = true
            val service = IMiniIviCarService.Stub.asInterface(binder)
            runCatching {
                val apiVersion = service.apiVersion
                check(apiVersion >= CarServiceContract.MIN_COMPATIBLE_API_VERSION) {
                    "Unsupported car service API version ${service.apiVersion}"
                }
                binder.linkToDeath(deathRecipient, 0)
                mutableBrightnessState.value = service.brightnessState
                mutableAudioState.value = service.audioState
                mutableHvacState.value = service.hvacState
                mutableVehicleStatusState.value = service.vehicleStatusState
                mutableClimateControlState.value = service.climateControlState
                mutableQuickControlsState.value = service.quickControlsState
                if (CarServiceCompatibility.supportsBluetooth(apiVersion)) {
                    mutableBluetoothState.value = service.bluetoothFeatureState
                } else {
                    mutableBluetoothState.value = unavailableBluetoothState(
                        "Bluetooth feature requires car service API $BLUETOOTH_API_VERSION",
                    )
                }
                service.registerBrightnessListener(brightnessListener)
                service.registerAudioListener(audioListener)
                service.registerHvacListener(hvacListener)
                service.registerVehicleStatusListener(vehicleStatusListener)
                service.registerClimateControlStateListener(climateControlListener)
                service.registerQuickControlsStateListener(quickControlsListener)
                if (CarServiceCompatibility.supportsBluetooth(apiVersion)) {
                    service.registerBluetoothFeatureStateListener(bluetoothListener)
                }
                remoteApiVersion = apiVersion
            }.onSuccess {
                remote = service
                retryBackoff.reset()
                lastLoggedRetryDelay = -1L
                Log.i(
                    TAG,
                    "event=client_ready api_version=$remoteApiVersion bluetooth=" +
                        CarServiceCompatibility.supportsBluetooth(remoteApiVersion),
                )
            }.onFailure { error ->
                Log.e(TAG, "event=client_initialization_failed", error)
                resetBindingAndRetry(error.message ?: "Car control service initialization failed")
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.w(TAG, "event=service_disconnected component=${name.flattenToShortString()}")
            remote = null
            remoteApiVersion = 0
            publishDisconnected("Car control service disconnected")
        }

        override fun onBindingDied(name: ComponentName) {
            Log.w(TAG, "event=binding_died component=${name.flattenToShortString()}")
            resetBindingAndRetry("Car control service binding died")
        }

        override fun onNullBinding(name: ComponentName) {
            Log.w(TAG, "event=null_binding component=${name.flattenToShortString()}")
            resetBindingAndRetry("Car control service returned a null binding")
        }
    }

    fun start() {
        if (started) return
        started = true
        Log.i(TAG, "event=client_started")
        bindNow()
    }

    fun close() {
        if (!started) return
        started = false
        Log.i(TAG, "event=client_stopping")
        mainHandler.removeCallbacksAndMessages(RETRY_TOKEN)
        remote?.let(::unregisterListeners)
        remote = null
        remoteApiVersion = 0
        unbindSafely()
        publishDisconnected("Car control client stopped")
        Log.i(TAG, "event=client_stopped")
    }

    fun setBrightness(progress: Float): Boolean =
        sendCommand("set_brightness", logSuccess = false) { it.setBrightness(progress) }

    fun setMediaVolume(volume: Int): Boolean =
        sendCommand("set_media_volume", logSuccess = false) { it.setMediaVolume(volume) }

    fun setHvacTemperature(zone: Int, celsius: Float): Boolean =
        sendCommand("set_hvac_temperature") { it.setHvacTemperature(zone, celsius) }

    fun setAcEnabled(enabled: Boolean): Boolean =
        sendCommand("set_ac") { it.setAcEnabled(enabled) }

    fun setClimatePowerEnabled(enabled: Boolean): Boolean =
        sendCommand("set_climate_power") { it.setClimatePowerEnabled(enabled) }

    fun setClimateAutoEnabled(enabled: Boolean): Boolean =
        sendCommand("set_climate_auto") { it.setClimateAutoEnabled(enabled) }

    fun setClimateSyncEnabled(enabled: Boolean): Boolean =
        sendCommand("set_climate_sync") { it.setClimateSyncEnabled(enabled) }

    fun setClimateRecirculationEnabled(enabled: Boolean): Boolean =
        sendCommand("set_climate_recirculation") { it.setClimateRecirculationEnabled(enabled) }

    fun setClimateFanSpeed(zone: Int, speed: Int): Boolean =
        sendCommand("set_climate_fan_speed") { it.setClimateFanSpeed(zone, speed) }

    fun setClimateFanDirection(zone: Int, direction: Int): Boolean =
        sendCommand("set_climate_fan_direction") { it.setClimateFanDirection(zone, direction) }

    fun setClimateDefrosterEnabled(window: Int, enabled: Boolean): Boolean =
        sendCommand("set_climate_defroster") { it.setClimateDefrosterEnabled(window, enabled) }

    fun setSeatHeatingLevel(zone: Int, level: Int): Boolean =
        sendCommand("set_seat_heating") { it.setSeatHeatingLevel(zone, level) }

    fun setSeatVentilationLevel(zone: Int, level: Int): Boolean =
        sendCommand("set_seat_ventilation") { it.setSeatVentilationLevel(zone, level) }

    fun setMaxAcEnabled(enabled: Boolean): Boolean =
        sendCommand("set_max_ac") { it.setMaxAcEnabled(enabled) }

    fun setMaxDefrostEnabled(enabled: Boolean): Boolean =
        sendCommand("set_max_defrost") { it.setMaxDefrostEnabled(enabled) }

    fun setAutoRecirculationEnabled(enabled: Boolean): Boolean =
        sendCommand("set_auto_recirculation") { it.setAutoRecirculationEnabled(enabled) }

    fun setSteeringWheelHeatLevel(level: Int): Boolean =
        sendCommand("set_steering_wheel_heat") { it.setSteeringWheelHeatLevel(level) }

    fun setTemperatureUnit(unit: Int): Boolean =
        sendCommand("set_temperature_unit") { it.setTemperatureUnit(unit) }

    fun setQuickControlEnabled(control: Int, enabled: Boolean): Boolean =
        sendCommand("set_quick_control") { it.setQuickControlEnabled(control, enabled) }

    fun requestScreenOff(): Boolean = sendCommand("request_screen_off") { it.requestScreenOff() }

    fun refreshBrightness(): Boolean = requestStateRefresh(CarFeature.BRIGHTNESS)

    fun refreshAudio(): Boolean = requestStateRefresh(CarFeature.AUDIO)

    fun refreshHvac(): Boolean = requestStateRefresh(CarFeature.HVAC)

    fun refreshVehicleStatus(): Boolean = requestStateRefresh(CarFeature.VEHICLE_STATUS)

    fun refreshQuickControls(): Boolean = requestStateRefresh(CarFeature.QUICK_CONTROLS)

    fun refreshBluetooth(): Boolean = requestStateRefresh(CarFeature.BLUETOOTH)

    fun requestBluetoothDiscovery(): Boolean =
        callVersion4("bluetooth_discovery") { it.requestBluetoothDiscovery() }

    fun renameLocalBluetoothDevice(name: String): Boolean =
        callVersion4("bluetooth_rename") { it.renameLocalBluetoothDevice(name) }

    private fun requestStateRefresh(featureMask: Int): Boolean =
        if (CarServiceCompatibility.supportsRefresh(remoteApiVersion)) {
            sendCommand("refresh_state") { it.requestStateRefresh(featureMask) }
        } else {
            resyncCachedState(featureMask)
        }

    private fun resyncCachedState(featureMask: Int): Boolean {
        val service = remote ?: return false
        return runCatching {
            if (featureMask and CarFeature.BRIGHTNESS != 0) mutableBrightnessState.value = service.brightnessState
            if (featureMask and CarFeature.AUDIO != 0) mutableAudioState.value = service.audioState
            if (featureMask and CarFeature.HVAC != 0) {
                mutableHvacState.value = service.hvacState
                mutableClimateControlState.value = service.climateControlState
            }
            if (featureMask and CarFeature.VEHICLE_STATUS != 0) {
                mutableVehicleStatusState.value = service.vehicleStatusState
            }
            if (featureMask and CarFeature.QUICK_CONTROLS != 0) {
                mutableQuickControlsState.value = service.quickControlsState
            }
        }.onSuccess {
            logDebug(
                "event=state_resynced mode=legacy feature_mask=0x${featureMask.toString(16)}",
            )
        }.onFailure { error ->
            Log.w(
                TAG,
                "event=state_resync_failed mode=legacy feature_mask=0x${featureMask.toString(16)}",
                error,
            )
        }.isSuccess
    }

    private fun callVersion4(
        operation: String,
        command: (IMiniIviCarService) -> Boolean,
    ): Boolean {
        val service = remote ?: return commandUnavailable(operation)
        if (!CarServiceCompatibility.supportsBluetooth(remoteApiVersion)) {
            Log.w(TAG, "event=command_rejected command=$operation reason=unsupported_api")
            return false
        }
        return runCatching { command(service) }
            .onSuccess { accepted ->
                logDebug("event=command_result command=$operation accepted=$accepted")
            }
            .onFailure { error ->
                Log.w(TAG, "event=command_failed command=$operation", error)
                mainHandler.post {
                    resetBindingAndRetry(error.message ?: "Car control Bluetooth command failed")
                }
            }
            .getOrDefault(false)
    }

    private fun sendCommand(
        operation: String,
        logSuccess: Boolean = true,
        command: (IMiniIviCarService) -> Unit,
    ): Boolean {
        val service = remote ?: return commandUnavailable(operation, logSuccess)
        return runCatching { command(service) }
            .onSuccess {
                if (logSuccess) logDebug("event=command_sent command=$operation")
            }
            .onFailure { error ->
                Log.w(TAG, "event=command_failed command=$operation", error)
                mainHandler.post {
                    resetBindingAndRetry(error.message ?: "Car control command failed")
                }
            }
            .isSuccess
    }

    private fun commandUnavailable(operation: String, logResult: Boolean = true): Boolean {
        if (logResult) logDebug("event=command_rejected command=$operation reason=not_connected")
        return false
    }

    private fun bindNow() {
        if (!started || binding || bound) return
        binding = true
        logDebug("event=bind_requested service=car_control")
        val intent = Intent().setComponent(
            ComponentName(CarServiceContract.SERVICE_PACKAGE, CarServiceContract.SERVICE_CLASS),
        )
        val accepted = runCatching {
            applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.onFailure { Log.e(TAG, "event=bind_failed service=car_control", it) }
            .getOrDefault(false)
        if (!accepted) {
            Log.w(TAG, "event=bind_rejected service=car_control")
            binding = false
            publishDisconnected("Car control service is unavailable")
            scheduleRetry()
        }
    }

    private fun resetBindingAndRetry(message: String) {
        Log.w(TAG, "event=connection_reset reason=${message.toEventKey()}")
        remote?.asBinder()?.unlinkToDeath(deathRecipient, 0)
        remote?.let(::unregisterListeners)
        remote = null
        remoteApiVersion = 0
        unbindSafely()
        publishDisconnected(message)
        scheduleRetry()
    }

    private fun unregisterListeners(service: IMiniIviCarService) {
        runCatching { service.unregisterBrightnessListener(brightnessListener) }
        runCatching { service.unregisterAudioListener(audioListener) }
        runCatching { service.unregisterHvacListener(hvacListener) }
        runCatching { service.unregisterVehicleStatusListener(vehicleStatusListener) }
        runCatching { service.unregisterClimateControlStateListener(climateControlListener) }
        runCatching { service.unregisterQuickControlsStateListener(quickControlsListener) }
        if (CarServiceCompatibility.supportsBluetooth(remoteApiVersion)) {
            runCatching { service.unregisterBluetoothFeatureStateListener(bluetoothListener) }
        }
        logDebug("event=listeners_unregistered service=car_control")
    }

    private fun unbindSafely() {
        if (bound || binding) {
            runCatching { applicationContext.unbindService(connection) }
                .onSuccess { logDebug("event=service_unbound service=car_control") }
                .onFailure { error -> Log.w(TAG, "event=unbind_failed service=car_control", error) }
        }
        bound = false
        binding = false
    }

    private fun scheduleRetry() {
        if (!started) return
        val delay = retryBackoff.nextDelay()
        if (delay != lastLoggedRetryDelay) {
            Log.w(TAG, "event=bind_retry_scheduled service=car_control delay_ms=$delay")
            lastLoggedRetryDelay = delay
        }
        mainHandler.removeCallbacksAndMessages(RETRY_TOKEN)
        mainHandler.postAtTime({ bindNow() }, RETRY_TOKEN, SystemClock.uptimeMillis() + delay)
    }

    private fun publishDisconnected(message: String) {
        val wasDisconnected = mutableBrightnessState.value.status == FeatureStatus.UNAVAILABLE &&
            !mutableBrightnessState.value.available
        if (!wasDisconnected) {
            Log.w(TAG, "event=client_unavailable reason=${message.toEventKey()}")
        }
        mutableBrightnessState.value = mutableBrightnessState.value.copy(
            status = FeatureStatus.UNAVAILABLE,
            available = false,
            diagnosticMessage = message,
        )
        mutableAudioState.value = mutableAudioState.value.copy(
            status = FeatureStatus.UNAVAILABLE,
            available = false,
            diagnosticMessage = message,
        )
        mutableHvacState.value = mutableHvacState.value.copy(
            status = FeatureStatus.UNAVAILABLE,
            available = false,
            diagnosticMessage = message,
        )
        mutableVehicleStatusState.value = mutableVehicleStatusState.value.copy(
            status = FeatureStatus.UNAVAILABLE,
            available = false,
            diagnosticMessage = message,
        )
        mutableClimateControlState.value = mutableClimateControlState.value.copy(
            status = FeatureStatus.UNAVAILABLE,
            available = false,
            diagnosticMessage = message,
        )
        mutableQuickControlsState.value = mutableQuickControlsState.value.copy(
            status = FeatureStatus.UNAVAILABLE,
            available = false,
            diagnosticMessage = message,
        )
        mutableBluetoothState.value = unavailableBluetoothState(message)
    }

    private fun unavailableBluetoothState(message: String) = BluetoothFeatureState(
        status = FeatureStatus.UNAVAILABLE,
        available = false,
        supported = false,
        diagnosticMessage = message,
    )

    private fun logStateTransition(
        feature: String,
        previousStatus: Int,
        previousAvailable: Boolean,
        status: Int,
        available: Boolean,
    ) {
        if (previousStatus == status && previousAvailable == available) return
        Log.i(
            TAG,
            "event=feature_state_changed feature=$feature status=$status available=$available",
        )
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "MiniIviCarClient"
        const val INITIAL_RETRY_MILLIS = 1_000L
        const val MAX_RETRY_MILLIS = 30_000L
        const val BLUETOOTH_API_VERSION = 4
        val RETRY_TOKEN = Any()
    }
}

internal object CarServiceCompatibility {
    private const val VERSION_4 = 4
    fun supportsRefresh(apiVersion: Int): Boolean = apiVersion >= VERSION_4
    fun supportsBluetooth(apiVersion: Int): Boolean = apiVersion >= VERSION_4
}

internal class RetryBackoff(
    private val initialDelayMillis: Long,
    private val maximumDelayMillis: Long,
) {
    private var currentDelayMillis = initialDelayMillis

    fun nextDelay(): Long = currentDelayMillis.also {
        currentDelayMillis = (currentDelayMillis * 2).coerceAtMost(maximumDelayMillis)
    }

    fun reset() {
        currentDelayMillis = initialDelayMillis
    }
}

private fun String.toEventKey(): String = lowercase().replace(' ', '_')
