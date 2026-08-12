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
import com.miniivi.car.api.BrightnessState
import com.miniivi.car.api.CarServiceContract
import com.miniivi.car.api.ClimateControlState
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.IAudioStateListener
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

    @Volatile private var remote: IMiniIviCarService? = null
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

    private val deathRecipient = IBinder.DeathRecipient {
        mainHandler.post { resetBindingAndRetry("Car control service binder died") }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            binding = false
            bound = true
            val service = IMiniIviCarService.Stub.asInterface(binder)
            runCatching {
                check(service.apiVersion >= CarServiceContract.API_VERSION) {
                    "Unsupported car service API version ${service.apiVersion}"
                }
                binder.linkToDeath(deathRecipient, 0)
                mutableBrightnessState.value = service.brightnessState
                mutableAudioState.value = service.audioState
                mutableHvacState.value = service.hvacState
                mutableVehicleStatusState.value = service.vehicleStatusState
                mutableClimateControlState.value = service.climateControlState
                mutableQuickControlsState.value = service.quickControlsState
                service.registerBrightnessListener(brightnessListener)
                service.registerAudioListener(audioListener)
                service.registerHvacListener(hvacListener)
                service.registerVehicleStatusListener(vehicleStatusListener)
                service.registerClimateControlStateListener(climateControlListener)
                service.registerQuickControlsStateListener(quickControlsListener)
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
    }

    private companion object {
        const val TAG = "MiniIviCarClient"
        const val INITIAL_RETRY_MILLIS = 1_000L
        const val MAX_RETRY_MILLIS = 30_000L
        val RETRY_TOKEN = Any()
    }
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
