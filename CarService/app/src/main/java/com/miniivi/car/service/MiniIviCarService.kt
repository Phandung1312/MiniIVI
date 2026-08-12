package com.miniivi.car.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import com.miniivi.car.api.AudioState
import com.miniivi.car.api.BrightnessState
import com.miniivi.car.api.CarServiceContract
import com.miniivi.car.api.ClimateControlState
import com.miniivi.car.api.HvacState
import com.miniivi.car.api.IAudioStateListener
import com.miniivi.car.api.IBrightnessStateListener
import com.miniivi.car.api.IHvacStateListener
import com.miniivi.car.api.IClimateControlStateListener
import com.miniivi.car.api.IMiniIviCarService
import com.miniivi.car.api.IVehicleStatusListener
import com.miniivi.car.api.IQuickControlsStateListener
import com.miniivi.car.api.QuickControlsState
import com.miniivi.car.api.VehicleStatusState
import com.miniivi.car.service.control.AudioController
import com.miniivi.car.service.control.BrightnessController
import com.miniivi.car.service.control.CurrentUserProvider
import com.miniivi.car.service.control.HvacController
import com.miniivi.car.service.control.QuickControlsController
import com.miniivi.car.service.control.VehicleStatusController
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class MiniIviCarService : Service() {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MiniIviCarControl")
    }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val brightnessListeners = RemoteCallbackList<IBrightnessStateListener>()
    private val audioListeners = RemoteCallbackList<IAudioStateListener>()
    private val hvacListeners = RemoteCallbackList<IHvacStateListener>()
    private val vehicleStatusListeners = RemoteCallbackList<IVehicleStatusListener>()
    private val climateControlListeners = RemoteCallbackList<IClimateControlStateListener>()
    private val quickControlsListeners = RemoteCallbackList<IQuickControlsStateListener>()

    private lateinit var brightnessController: BrightnessController
    private lateinit var audioController: AudioController
    private lateinit var hvacController: HvacController
    private lateinit var vehicleStatusController: VehicleStatusController
    private lateinit var quickControlsController: QuickControlsController

    private val binder = object : IMiniIviCarService.Stub() {
        override fun getApiVersion(): Int {
            enforceAccess()
            return CarServiceContract.API_VERSION
        }

        override fun getBrightnessState(): BrightnessState {
            enforceAccess()
            return brightnessController.state.value
        }

        override fun getAudioState(): AudioState {
            enforceAccess()
            return audioController.state.value
        }

        override fun getHvacState(): HvacState {
            enforceAccess()
            return hvacController.state.value
        }

        override fun registerBrightnessListener(listener: IBrightnessStateListener) {
            enforceAccess()
            if (brightnessListeners.register(listener)) {
                runCatching { listener.onBrightnessStateChanged(brightnessController.state.value) }
            }
        }

        override fun unregisterBrightnessListener(listener: IBrightnessStateListener) {
            enforceAccess()
            brightnessListeners.unregister(listener)
        }

        override fun registerAudioListener(listener: IAudioStateListener) {
            enforceAccess()
            if (audioListeners.register(listener)) {
                runCatching { listener.onAudioStateChanged(audioController.state.value) }
            }
        }

        override fun unregisterAudioListener(listener: IAudioStateListener) {
            enforceAccess()
            audioListeners.unregister(listener)
        }

        override fun registerHvacListener(listener: IHvacStateListener) {
            enforceAccess()
            if (hvacListeners.register(listener)) {
                runCatching { listener.onHvacStateChanged(hvacController.state.value) }
            }
        }

        override fun unregisterHvacListener(listener: IHvacStateListener) {
            enforceAccess()
            hvacListeners.unregister(listener)
        }

        override fun setBrightness(progress: Float) {
            enforceAccess()
            brightnessController.setProgress(progress)
        }

        override fun setMediaVolume(volume: Int) {
            enforceAccess()
            audioController.setVolume(volume)
        }

        override fun setHvacTemperature(zone: Int, celsius: Float) {
            enforceAccess()
            hvacController.setTemperature(zone, celsius)
        }

        override fun setAcEnabled(enabled: Boolean) {
            enforceAccess()
            hvacController.setAcEnabled(enabled)
        }

        override fun getVehicleStatusState(): VehicleStatusState {
            enforceAccess()
            return vehicleStatusController.state.value
        }

        override fun registerVehicleStatusListener(listener: IVehicleStatusListener) {
            enforceAccess()
            if (vehicleStatusListeners.register(listener)) {
                runCatching { listener.onVehicleStatusChanged(vehicleStatusController.state.value) }
            }
        }

        override fun unregisterVehicleStatusListener(listener: IVehicleStatusListener) {
            enforceAccess()
            vehicleStatusListeners.unregister(listener)
        }

        override fun getClimateControlState(): ClimateControlState {
            enforceAccess()
            return hvacController.climateState.value
        }

        override fun registerClimateControlStateListener(listener: IClimateControlStateListener) {
            enforceAccess()
            if (climateControlListeners.register(listener)) {
                runCatching { listener.onClimateControlStateChanged(hvacController.climateState.value) }
            }
        }

        override fun unregisterClimateControlStateListener(listener: IClimateControlStateListener) {
            enforceAccess()
            climateControlListeners.unregister(listener)
        }

        override fun setClimatePowerEnabled(enabled: Boolean) =
            hvacController.setPowerEnabled(enforceAndReturn(enabled))

        override fun setClimateAutoEnabled(enabled: Boolean) =
            hvacController.setAutoEnabled(enforceAndReturn(enabled))

        override fun setClimateSyncEnabled(enabled: Boolean) =
            hvacController.setSyncEnabled(enforceAndReturn(enabled))

        override fun setClimateRecirculationEnabled(enabled: Boolean) =
            hvacController.setRecirculationEnabled(enforceAndReturn(enabled))

        override fun setClimateFanSpeed(zone: Int, speed: Int) {
            enforceAccess()
            hvacController.setFanSpeed(zone, speed)
        }

        override fun setClimateFanDirection(zone: Int, direction: Int) {
            enforceAccess()
            hvacController.setFanDirection(zone, direction)
        }

        override fun setClimateDefrosterEnabled(window: Int, enabled: Boolean) {
            enforceAccess()
            hvacController.setDefrosterEnabled(window, enabled)
        }

        override fun setSeatHeatingLevel(zone: Int, level: Int) {
            enforceAccess()
            hvacController.setSeatHeatingLevel(zone, level)
        }

        override fun setSeatVentilationLevel(zone: Int, level: Int) {
            enforceAccess()
            hvacController.setSeatVentilationLevel(zone, level)
        }

        override fun setMaxAcEnabled(enabled: Boolean) =
            hvacController.setMaxAcEnabled(enforceAndReturn(enabled))

        override fun setMaxDefrostEnabled(enabled: Boolean) =
            hvacController.setMaxDefrostEnabled(enforceAndReturn(enabled))

        override fun setAutoRecirculationEnabled(enabled: Boolean) =
            hvacController.setAutoRecirculationEnabled(enforceAndReturn(enabled))

        override fun setSteeringWheelHeatLevel(level: Int) {
            enforceAccess()
            hvacController.setSteeringWheelHeatLevel(level)
        }

        override fun setTemperatureUnit(unit: Int) {
            enforceAccess()
            hvacController.setTemperatureUnit(unit)
        }

        override fun getQuickControlsState(): QuickControlsState {
            enforceAccess()
            return quickControlsController.state.value
        }

        override fun registerQuickControlsStateListener(listener: IQuickControlsStateListener) {
            enforceAccess()
            if (quickControlsListeners.register(listener)) {
                runCatching { listener.onQuickControlsStateChanged(quickControlsController.state.value) }
            }
        }

        override fun unregisterQuickControlsStateListener(listener: IQuickControlsStateListener) {
            enforceAccess()
            quickControlsListeners.unregister(listener)
        }

        override fun setQuickControlEnabled(control: Int, enabled: Boolean) {
            enforceAccess()
            quickControlsController.setEnabled(control, enabled)
        }

        override fun requestScreenOff() {
            enforceAccess()
            quickControlsController.requestScreenOff()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val currentUserProvider = CurrentUserProvider(applicationContext)
        brightnessController = BrightnessController(applicationContext, currentUserProvider, scope)
        audioController = AudioController(applicationContext, scope)
        hvacController = HvacController(applicationContext, scope)
        vehicleStatusController = VehicleStatusController(applicationContext, scope)
        quickControlsController = QuickControlsController(applicationContext, scope)

        scope.launch {
            brightnessController.state.drop(1).collect(::notifyBrightness)
        }
        scope.launch {
            audioController.state.drop(1).collect(::notifyAudio)
        }
        scope.launch {
            hvacController.state.drop(1).collect(::notifyHvac)
        }
        scope.launch {
            vehicleStatusController.state.drop(1).collect(::notifyVehicleStatus)
        }
        scope.launch {
            hvacController.climateState.drop(1).collect(::notifyClimateControl)
        }
        scope.launch {
            quickControlsController.state.drop(1).collect(::notifyQuickControls)
        }

        brightnessController.start()
        audioController.start()
        hvacController.start()
        vehicleStatusController.start()
        quickControlsController.start()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        brightnessController.stop()
        audioController.stop()
        hvacController.stop()
        vehicleStatusController.stop()
        quickControlsController.stop()
        brightnessListeners.kill()
        audioListeners.kill()
        hvacListeners.kill()
        vehicleStatusListeners.kill()
        climateControlListeners.kill()
        quickControlsListeners.kill()
        scope.cancel()
        dispatcher.close()
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun enforceAccess() {
        enforceCallingOrSelfPermission(
            CarServiceContract.CONTROL_PERMISSION,
            "Caller does not have MiniIVI car control permission",
        )
    }

    private fun enforceAndReturn(value: Boolean): Boolean {
        enforceAccess()
        return value
    }

    private fun notifyBrightness(state: BrightnessState) {
        broadcast(brightnessListeners) { it.onBrightnessStateChanged(state) }
    }

    private fun notifyAudio(state: AudioState) {
        broadcast(audioListeners) { it.onAudioStateChanged(state) }
    }

    private fun notifyHvac(state: HvacState) {
        broadcast(hvacListeners) { it.onHvacStateChanged(state) }
    }

    private fun notifyVehicleStatus(state: VehicleStatusState) {
        broadcast(vehicleStatusListeners) { it.onVehicleStatusChanged(state) }
    }

    private fun notifyClimateControl(state: ClimateControlState) {
        broadcast(climateControlListeners) { it.onClimateControlStateChanged(state) }
    }

    private fun notifyQuickControls(state: QuickControlsState) {
        broadcast(quickControlsListeners) { it.onQuickControlsStateChanged(state) }
    }

    private inline fun <T : android.os.IInterface> broadcast(
        callbacks: RemoteCallbackList<T>,
        action: (T) -> Unit,
    ) {
        val count = callbacks.beginBroadcast()
        try {
            for (index in 0 until count) runCatching { action(callbacks.getBroadcastItem(index)) }
        } finally {
            callbacks.finishBroadcast()
        }
    }
}
