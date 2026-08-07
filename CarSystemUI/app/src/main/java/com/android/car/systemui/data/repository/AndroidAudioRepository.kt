package com.android.car.systemui.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.android.car.systemui.data.model.AudioState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

internal object AudioLevelMapper {
    fun toVolume(progress: Float, minimum: Int, maximum: Int): Int =
        (minimum + progress.coerceIn(0f, 1f) * (maximum - minimum))
            .roundToInt()
            .coerceIn(minimum, maximum)
}

class AndroidAudioRepository(private val context: Context) : AudioRepository {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mutableState = MutableStateFlow(AudioState())
    override val state = mutableState.asStateFlow()
    private var started = false

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_MUSIC) ==
                AudioManager.STREAM_MUSIC
            ) {
                refresh()
            }
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
        refresh()
    }

    override fun stop() {
        if (!started) return
        started = false
        runCatching { context.unregisterReceiver(volumeReceiver) }
    }

    override fun refresh() {
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
        val manager = audioManager ?: return
        val current = mutableState.value
        val clamped = volume.coerceIn(current.minimum, current.maximum)
        runCatching {
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
            mutableState.value = current.copy(volume = clamped, errorMessage = null)
        }.onFailure { error ->
            mutableState.value = current.copy(errorMessage = error.message)
        }
    }

    private companion object {
        const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    }
}
