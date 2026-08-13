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
    private val retryBackoff = RetryBackoff(INITIAL_RETRY_MILLIS, MAX_RETRY_MILLIS)

    private val brightnessListener = object : IBrightnessStateListener.Stub() {
        override fun onBrightnessStateChanged(state: BrightnessState) {
            mutableBrightnessState.value = state
        }
    }

    private val audioListener = object : IAudioStateListener.Stub() {
        override fun onAudioStateChanged(state: AudioState) {
            mutableAudioState.value = state
        }
    }

    private val hvacListener = object : IHvacStateListener.Stub() {
        override fun onHvacStateChanged(state: HvacState) {
            mutableHvacState.value = state
        }
    }

    private val vehicleStatusListener = object : IVehicleStatusListener.Stub() {
        override fun onVehicleStatusChanged(state: VehicleStatusState) {
            mutableVehicleStatusState.value = state
        }
    }

    private val climateControlListener = object : IClimateControlStateListener.Stub() {
        override fun onClimateControlStateChanged(state: ClimateControlState) {
            mutableClimateControlState.value = state
        }
    }

    private val quickControlsListener = object : IQuickControlsStateListener.Stub() {
        override fun onQuickControlsStateChanged(state: QuickControlsState) {
            mutableQuickControlsState.value = state
        }
    }

    private val bluetoothListener = object : IBluetoothFeatureStateListener.Stub() {
        override fun onBluetoothFeatureStateChanged(state: BluetoothFeatureState) {
            mutableBluetoothState.value = state
        }
    }

    private val deathRecipient = IBinder.DeathRecipient {
        mainHandler.post { resetBindingAndRetry("Car control service binder died") }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
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
            }.onFailure { error ->
                Log.e(TAG, "Unable to initialize the car control client", error)
                resetBindingAndRetry(error.message ?: "Car control service initialization failed")
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            remote = null
            remoteApiVersion = 0
            publishDisconnected("Car control service disconnected")
        }

        override fun onBindingDied(name: ComponentName) {
            resetBindingAndRetry("Car control service binding died")
        }

        override fun onNullBinding(name: ComponentName) {
            resetBindingAndRetry("Car control service returned a null binding")
        }
    }

    fun start() {
        if (started) return
        started = true
        bindNow()
    }

    fun close() {
        if (!started) return
        started = false
        mainHandler.removeCallbacksAndMessages(RETRY_TOKEN)
        remote?.let(::unregisterListeners)
        remote = null
        remoteApiVersion = 0
        unbindSafely()
        publishDisconnected("Car control client stopped")
    }

    fun setBrightness(progress: Float): Boolean =
        sendCommand { it.setBrightness(progress) }

    fun setMediaVolume(volume: Int): Boolean =
        sendCommand { it.setMediaVolume(volume) }

    fun setHvacTemperature(zone: Int, celsius: Float): Boolean =
        sendCommand { it.setHvacTemperature(zone, celsius) }

    fun setAcEnabled(enabled: Boolean): Boolean =
        sendCommand { it.setAcEnabled(enabled) }

    fun setClimatePowerEnabled(enabled: Boolean): Boolean =
        sendCommand { it.setClimatePowerEnabled(enabled) }

    fun setClimateAutoEnabled(enabled: Boolean): Boolean =
        sendCommand { it.setClimateAutoEnabled(enabled) }

    fun setClimateSyncEnabled(enabled: Boolean): Boolean =
        sendCommand { it.setClimateSyncEnabled(enabled) }

    fun setClimateRecirculationEnabled(enabled: Boolean): Boolean =
        sendCommand { it.setClimateRecirculationEnabled(enabled) }

    fun setClimateFanSpeed(zone: Int, speed: Int): Boolean =
        sendCommand { it.setClimateFanSpeed(zone, speed) }

    fun setClimateFanDirection(zone: Int, direction: Int): Boolean =
        sendCommand { it.setClimateFanDirection(zone, direction) }

    fun setClimateDefrosterEnabled(window: Int, enabled: Boolean): Boolean =
        sendCommand { it.setClimateDefrosterEnabled(window, enabled) }

    fun setSeatHeatingLevel(zone: Int, level: Int): Boolean =
        sendCommand { it.setSeatHeatingLevel(zone, level) }

    fun setSeatVentilationLevel(zone: Int, level: Int): Boolean =
        sendCommand { it.setSeatVentilationLevel(zone, level) }

    fun setMaxAcEnabled(enabled: Boolean): Boolean =
        sendCommand { it.setMaxAcEnabled(enabled) }

    fun setMaxDefrostEnabled(enabled: Boolean): Boolean =
        sendCommand { it.setMaxDefrostEnabled(enabled) }

    fun setAutoRecirculationEnabled(enabled: Boolean): Boolean =
        sendCommand { it.setAutoRecirculationEnabled(enabled) }

    fun setSteeringWheelHeatLevel(level: Int): Boolean =
        sendCommand { it.setSteeringWheelHeatLevel(level) }

    fun setTemperatureUnit(unit: Int): Boolean =
        sendCommand { it.setTemperatureUnit(unit) }

    fun setQuickControlEnabled(control: Int, enabled: Boolean): Boolean =
        sendCommand { it.setQuickControlEnabled(control, enabled) }

    fun requestScreenOff(): Boolean = sendCommand { it.requestScreenOff() }

    fun refreshBrightness(): Boolean = requestStateRefresh(CarFeature.BRIGHTNESS)

    fun refreshAudio(): Boolean = requestStateRefresh(CarFeature.AUDIO)

    fun refreshHvac(): Boolean = requestStateRefresh(CarFeature.HVAC)

    fun refreshVehicleStatus(): Boolean = requestStateRefresh(CarFeature.VEHICLE_STATUS)

    fun refreshQuickControls(): Boolean = requestStateRefresh(CarFeature.QUICK_CONTROLS)

    fun refreshBluetooth(): Boolean = requestStateRefresh(CarFeature.BLUETOOTH)

    fun requestBluetoothDiscovery(): Boolean = callVersion4 { it.requestBluetoothDiscovery() }

    fun renameLocalBluetoothDevice(name: String): Boolean =
        callVersion4 { it.renameLocalBluetoothDevice(name) }

    private fun requestStateRefresh(featureMask: Int): Boolean =
        if (CarServiceCompatibility.supportsRefresh(remoteApiVersion)) {
            sendCommand { it.requestStateRefresh(featureMask) }
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
        }.isSuccess
    }

    private fun callVersion4(command: (IMiniIviCarService) -> Boolean): Boolean {
        val service = remote ?: return false
        if (!CarServiceCompatibility.supportsBluetooth(remoteApiVersion)) return false
        return runCatching { command(service) }
            .onFailure { error ->
                Log.w(TAG, "Unable to send a Bluetooth command", error)
                mainHandler.post {
                    resetBindingAndRetry(error.message ?: "Car control Bluetooth command failed")
                }
            }
            .getOrDefault(false)
    }

    private fun sendCommand(command: (IMiniIviCarService) -> Unit): Boolean {
        val service = remote ?: return false
        return runCatching { command(service) }
            .onFailure { error ->
                Log.w(TAG, "Unable to send a car control command", error)
                mainHandler.post {
                    resetBindingAndRetry(error.message ?: "Car control command failed")
                }
            }
            .isSuccess
    }

    private fun bindNow() {
        if (!started || binding || bound) return
        binding = true
        val intent = Intent().setComponent(
            ComponentName(CarServiceContract.SERVICE_PACKAGE, CarServiceContract.SERVICE_CLASS),
        )
        val accepted = runCatching {
            applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.onFailure { Log.e(TAG, "Unable to bind the car control service", it) }
            .getOrDefault(false)
        if (!accepted) {
            binding = false
            publishDisconnected("Car control service is unavailable")
            scheduleRetry()
        }
    }

    private fun resetBindingAndRetry(message: String) {
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
    }

    private fun unbindSafely() {
        if (bound || binding) runCatching { applicationContext.unbindService(connection) }
        bound = false
        binding = false
    }

    private fun scheduleRetry() {
        if (!started) return
        val delay = retryBackoff.nextDelay()
        mainHandler.removeCallbacksAndMessages(RETRY_TOKEN)
        mainHandler.postAtTime({ bindNow() }, RETRY_TOKEN, SystemClock.uptimeMillis() + delay)
    }

    private fun publishDisconnected(message: String) {
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
