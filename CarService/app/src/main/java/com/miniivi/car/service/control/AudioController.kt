package com.miniivi.car.service.control

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.miniivi.car.api.AudioState
import com.miniivi.car.api.CarServiceError
import com.miniivi.car.api.FeatureStatus
import java.lang.reflect.Proxy
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal object AudioPolicy {
    fun clamp(volume: Int, minimum: Int, maximum: Int): Int =
        volume.coerceIn(minimum.coerceAtMost(maximum), maximum.coerceAtLeast(minimum))
}

class AudioController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mutableState = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = mutableState.asStateFlow()

    private var started = false
    private var pollingJob: Job? = null
    private val carConnection = LifecycleAwareCarConnection(
        context = context,
        onReady = { lifecycleCar -> scope.launch { onCarReady(lifecycleCar) } },
        onUnavailable = { scope.launch { onCarUnavailable() } },
    )
    private var carAudioManager: Any? = null
    private var carVolumeCallback: Any? = null
    private var mediaGroupId: Int? = null

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stream = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC)
            if (stream == AudioManager.STREAM_MUSIC || carAudioManager != null) refresh()
        }
    }

    fun start() {
        if (started) return
        started = true
        Log.i(TAG, "event=controller_started feature=audio")
        ContextCompat.registerReceiver(
            context,
            volumeReceiver,
            IntentFilter(ACTION_VOLUME_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch {
            val hasCallback = runCatching { connectCarAudio() }
                .onFailure {
                    Log.w(TAG, "event=backend_fallback feature=audio backend=media_stream", it)
                    disconnectCarAudio()
                }
                .getOrDefault(false)
            if (!hasCallback) startPolling()
            refreshBlocking()
        }
    }

    fun stop() {
        if (!started) return
        started = false
        Log.i(TAG, "event=controller_stopping feature=audio")
        pollingJob?.cancel()
        pollingJob = null
        runCatching { context.unregisterReceiver(volumeReceiver) }
        disconnectCarAudio()
        Log.i(TAG, "event=controller_stopped feature=audio")
    }

    fun refresh() {
        scope.launch { refreshBlocking() }
    }

    fun setVolume(volume: Int) {
        scope.launch {
            val current = mutableState.value
            if (!current.available) return@launch
            val clamped = AudioPolicy.clamp(volume, current.minimum, current.maximum)
            val manager = carAudioManager
            val groupId = mediaGroupId
            runCatching {
                if (manager != null && groupId != null) {
                    manager.javaClass.getMethod(
                        "setGroupVolume",
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                    ).invoke(manager, groupId, clamped, 0)
                } else {
                    checkNotNull(audioManager) { "Audio service is unavailable" }
                        .setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
                }
                mutableState.value = current.copy(
                    status = FeatureStatus.READY,
                    volume = clamped,
                    errorCode = CarServiceError.NONE,
                    diagnosticMessage = null,
                )
            }.onFailure { error -> publishError("Unable to set media volume", error) }
        }
    }

    private fun connectCarAudio(): Boolean {
        if (carAudioManager != null) return carVolumeCallback != null
        if (carConnection.car != null) return false
        carConnection.connect()
        return false
    }

    private fun onCarReady(lifecycleCar: Any) {
        if (!started || carAudioManager != null) return
        runCatching { connectCarAudioManager(lifecycleCar) }
            .onSuccess { hasCallback ->
                Log.i(
                    TAG,
                    "event=backend_ready feature=audio backend=car_audio callback=$hasCallback " +
                        "group_id=${mediaGroupId ?: -1}",
                )
                if (hasCallback) {
                    pollingJob?.cancel()
                    pollingJob = null
                } else {
                    startPolling()
                }
                refreshBlocking()
            }
            .onFailure { error ->
                Log.w(TAG, "event=backend_fallback feature=audio backend=media_stream", error)
                clearCarAudioManager()
                refreshStreamVolume()
                startPolling()
            }
    }

    private fun onCarUnavailable() {
        Log.w(TAG, "event=backend_unavailable feature=audio backend=car_audio")
        clearCarAudioManager(unregisterCallback = false)
        refreshStreamVolume()
    }

    private fun connectCarAudioManager(lifecycleCar: Any): Boolean {
        val carClass = Class.forName(CAR_CLASS)
        val manager = carClass.getMethod("getCarManager", String::class.java)
            .invoke(lifecycleCar, AUDIO_SERVICE)
            ?: error("Car audio service is unavailable")
        carAudioManager = manager
        mediaGroupId = runCatching {
            manager.javaClass.getMethod(
                "getVolumeGroupIdForUsage",
                Int::class.javaPrimitiveType,
            ).invoke(manager, AudioAttributes.USAGE_MEDIA) as Int
        }.getOrElse {
            val count = manager.javaClass.getMethod("getVolumeGroupCount").invoke(manager) as Int
            check(count > 0) { "No car audio volume groups are available" }
            DEFAULT_MEDIA_GROUP
        }
        return registerCarVolumeCallback(manager)
    }

    private fun registerCarVolumeCallback(manager: Any): Boolean = runCatching {
        val callbackClass = Class.forName(CAR_VOLUME_GROUP_EVENT_CALLBACK)
        val callback = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass),
        ) { proxy, method, args ->
            when (method.name) {
                "onVolumeGroupEvent" -> refresh()
                "toString" -> "MiniIVI car audio callback"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        }
        val registered = manager.javaClass.getMethod(
            "registerCarVolumeGroupEventCallback",
            Executor::class.java,
            callbackClass,
        ).invoke(manager, Executor { command -> command.run() }, callback) as Boolean
        if (registered) carVolumeCallback = callback
        registered
    }.getOrDefault(false)

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            var reconnectCountdown = CAR_RECONNECT_INTERVAL_TICKS
            while (isActive && started && carVolumeCallback == null) {
                delay(POLL_INTERVAL_MILLIS)
                refreshBlocking()
                if (carAudioManager == null && --reconnectCountdown <= 0) {
                    reconnectCountdown = CAR_RECONNECT_INTERVAL_TICKS
                    runCatching { connectCarAudio() }
                        .onFailure { disconnectCarAudio() }
                }
            }
        }
    }

    private fun refreshBlocking() {
        val manager = carAudioManager
        val groupId = mediaGroupId
        if (manager != null && groupId != null) {
            runCatching {
                AudioState(
                    status = FeatureStatus.READY,
                    available = true,
                    volume = invokeGroupInt(manager, "getGroupVolume", groupId),
                    minimum = invokeGroupInt(manager, "getGroupMinVolume", groupId),
                    maximum = invokeGroupInt(manager, "getGroupMaxVolume", groupId),
                )
            }.onSuccess { mutableState.value = it }
                .onFailure { error ->
                    Log.w(
                        TAG,
                        "event=backend_fallback feature=audio backend=media_stream " +
                            "reason=car_audio_read_failed",
                        error,
                    )
                    disconnectCarAudio()
                    refreshStreamVolume()
                    startPolling()
                }
            return
        }
        refreshStreamVolume()
    }

    private fun invokeGroupInt(manager: Any, methodName: String, groupId: Int): Int =
        manager.javaClass.getMethod(methodName, Int::class.javaPrimitiveType)
            .invoke(manager, groupId) as Int

    private fun refreshStreamVolume() {
        val manager = audioManager
        if (manager == null) {
            mutableState.value = AudioState(
                status = FeatureStatus.UNAVAILABLE,
                errorCode = CarServiceError.PLATFORM_UNAVAILABLE,
                diagnosticMessage = "Audio service is unavailable",
            )
            return
        }
        runCatching {
            AudioState(
                status = FeatureStatus.READY,
                available = true,
                volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC),
                minimum = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    manager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                } else {
                    0
                },
                maximum = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            )
        }.onSuccess { mutableState.value = it }
            .onFailure { error -> publishError("Unable to read media volume", error) }
    }

    private fun disconnectCarAudio() {
        clearCarAudioManager()
        carConnection.disconnect()
    }

    private fun clearCarAudioManager(unregisterCallback: Boolean = true) {
        val manager = carAudioManager
        val callback = carVolumeCallback
        if (unregisterCallback && manager != null && callback != null) {
            runCatching {
                manager.javaClass.getMethod(
                    "unregisterCarVolumeGroupEventCallback",
                    callback.javaClass.interfaces.first(),
                ).invoke(manager, callback)
            }.onFailure {
                Log.w(TAG, "event=callback_unregister_failed feature=audio", it)
            }
        }
        carVolumeCallback = null
        carAudioManager = null
        mediaGroupId = null
    }

    private fun publishError(message: String, error: Throwable) {
        Log.e(TAG, "event=operation_failed feature=audio operation=${message.toEventKey()}", error)
        mutableState.value = mutableState.value.copy(
            status = FeatureStatus.ERROR,
            available = false,
            errorCode = CarServiceError.PLATFORM_OPERATION_FAILED,
            diagnosticMessage = "$message: ${error.message}",
        )
    }

    private companion object {
        const val TAG = "MiniIviCarAudio"
        const val CAR_CLASS = "android.car.Car"
        const val AUDIO_SERVICE = "audio"
        const val CAR_VOLUME_GROUP_EVENT_CALLBACK =
            "android.car.media.CarVolumeGroupEventCallback"
        const val DEFAULT_MEDIA_GROUP = 0
        const val POLL_INTERVAL_MILLIS = 1_000L
        const val CAR_RECONNECT_INTERVAL_TICKS = 30
        const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    }
}
