package com.android.car.systemui.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.android.car.systemui.data.model.AudioState
import java.lang.reflect.Proxy
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal object AudioLevelMapper {
    fun toVolume(progress: Float, minimum: Int, maximum: Int): Int =
        (minimum + progress.coerceIn(0f, 1f) * (maximum - minimum))
            .roundToInt()
            .coerceIn(minimum, maximum)
}

/**
 * Uses the AAOS media volume group as the source of truth. ROMs without CarAudioManager fall
 * back to Android's music stream so the SystemUI remains usable outside an automotive image.
 */
class AndroidAudioRepository(private val context: Context) : AudioRepository {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(AudioState())
    override val state = mutableState.asStateFlow()

    private var started = false
    private var fallbackPollingJob: Job? = null
    private var car: Any? = null
    private var carAudioManager: Any? = null
    private var carVolumeCallback: Any? = null
    private var mediaGroupId: Int? = null

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stream = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC)
            if (stream == AudioManager.STREAM_MUSIC || carAudioManager != null) refresh()
        }
    }

    override fun start() {
        if (started) return
        started = true
        ContextCompat.registerReceiver(
            context,
            volumeReceiver,
            IntentFilter(ACTION_VOLUME_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val hasCarSignal = runCatching { connectCarAudio() }
            .onFailure { Log.w(TAG, "Car audio unavailable; using stream volume", it) }
            .getOrDefault(false)
        if (!hasCarSignal) startFallbackPolling()
        refresh()
    }

    override fun stop() {
        if (!started) return
        started = false
        fallbackPollingJob?.cancel()
        fallbackPollingJob = null
        runCatching { context.unregisterReceiver(volumeReceiver) }
        disconnectCarAudio()
    }

    private fun connectCarAudio(): Boolean {
        val carClass = Class.forName(CAR_CLASS)
        car = carClass.getMethod("createCar", Context::class.java).invoke(null, context)
        val manager = carClass.getMethod("getCarManager", String::class.java)
            .invoke(car, AUDIO_SERVICE)
            ?: error("No car audio service")
        carAudioManager = manager
        mediaGroupId = runCatching {
            manager.javaClass.getMethod(
                "getVolumeGroupIdForUsage",
                Int::class.javaPrimitiveType,
            ).invoke(manager, AudioAttributes.USAGE_MEDIA) as Int
        }.getOrElse {
            val count = manager.javaClass.getMethod("getVolumeGroupCount").invoke(manager) as Int
            check(count > 0) { "No car audio volume groups" }
            DEFAULT_MEDIA_GROUP
        }
        return registerCarVolumeGroupCallback(manager)
    }

    /** Android 14+ signal callback. Older car stacks are refreshed by the lightweight fallback. */
    private fun registerCarVolumeGroupCallback(manager: Any): Boolean = runCatching {
        val callbackClass = Class.forName(CAR_VOLUME_GROUP_EVENT_CALLBACK)
        val callback = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass),
        ) { proxy, method, args ->
            when (method.name) {
                "onVolumeGroupEvent" -> refresh()
                "toString" -> "CarSystemUI volume group callback"
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

    private fun startFallbackPolling() {
        fallbackPollingJob?.cancel()
        fallbackPollingJob = scope.launch {
            while (isActive) {
                delay(FALLBACK_REFRESH_INTERVAL_MS)
                refresh()
            }
        }
    }

    override fun refresh() {
        val manager = carAudioManager
        val groupId = mediaGroupId
        if (manager != null && groupId != null) {
            runCatching { readCarAudioState(manager, groupId) }
                .onSuccess { mutableState.value = it }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        available = false,
                        errorMessage = error.message,
                    )
                }
            return
        }
        refreshStreamVolume()
    }

    private fun readCarAudioState(manager: Any, groupId: Int): AudioState = AudioState(
        volume = invokeGroupInt(manager, "getGroupVolume", groupId),
        minimum = invokeGroupInt(manager, "getGroupMinVolume", groupId),
        maximum = invokeGroupInt(manager, "getGroupMaxVolume", groupId),
        available = true,
    )

    private fun invokeGroupInt(manager: Any, methodName: String, groupId: Int): Int =
        manager.javaClass.getMethod(methodName, Int::class.javaPrimitiveType)
            .invoke(manager, groupId) as Int

    private fun refreshStreamVolume() {
        val manager = audioManager
        if (manager == null) {
            mutableState.value = AudioState(errorMessage = "Audio service unavailable")
            return
        }
        runCatching {
            val minimum = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
            } else {
                0
            }
            AudioState(
                volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC),
                minimum = minimum,
                maximum = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                available = true,
            )
        }.onSuccess { mutableState.value = it }
            .onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    available = false,
                    errorMessage = error.message,
                )
            }
    }

    override fun setVolume(volume: Int) {
        val current = mutableState.value
        if (!current.available) return
        val clamped = volume.coerceIn(current.minimum, current.maximum)
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
                checkNotNull(audioManager) { "Audio service unavailable" }
                    .setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
            }
            mutableState.value = current.copy(volume = clamped, errorMessage = null)
        }.onFailure { error ->
            mutableState.value = current.copy(errorMessage = error.message)
        }
    }

    private fun disconnectCarAudio() {
        val manager = carAudioManager
        val callback = carVolumeCallback
        if (manager != null && callback != null) {
            runCatching {
                manager.javaClass.getMethod(
                    "unregisterCarVolumeGroupEventCallback",
                    callback.javaClass.interfaces.first(),
                ).invoke(manager, callback)
            }.onFailure { Log.w(TAG, "Cannot unregister car volume callback", it) }
        }
        runCatching { car?.javaClass?.getMethod("disconnect")?.invoke(car) }
        carVolumeCallback = null
        carAudioManager = null
        mediaGroupId = null
        car = null
    }

    private companion object {
        const val TAG = "CarSystemUI-Audio"
        const val CAR_CLASS = "android.car.Car"
        const val AUDIO_SERVICE = "audio"
        const val CAR_VOLUME_GROUP_EVENT_CALLBACK =
            "android.car.media.CarVolumeGroupEventCallback"
        const val DEFAULT_MEDIA_GROUP = 0
        const val FALLBACK_REFRESH_INTERVAL_MS = 1_000L
        const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    }
}
