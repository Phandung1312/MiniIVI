package com.android.car.systemui.data.repository

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.android.car.systemui.data.model.BrightnessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal object BrightnessMapper {
    const val MINIMUM = 1
    const val MAXIMUM = 255

    fun toProgress(value: Int): Float =
        ((value.coerceIn(MINIMUM, MAXIMUM) - MINIMUM).toFloat() / (MAXIMUM - MINIMUM))

    fun toSetting(progress: Float): Int =
        (MINIMUM + progress.coerceIn(0f, 1f) * (MAXIMUM - MINIMUM)).toInt()
            .coerceIn(MINIMUM, MAXIMUM)
}

class AndroidBrightnessRepository(
    private val applicationContext: Context,
    private val currentUserProvider: CurrentUserProvider,
) : BrightnessRepository {
    private val mutableState = MutableStateFlow(BrightnessState())
    override val state = mutableState.asStateFlow()

    private var started = false
    private var observedResolver: ContentResolver? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    private val userReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            registerObserverForCurrentUser()
            refresh()
        }
    }

    override fun start() {
        if (started) return
        started = true
        ContextCompat.registerReceiver(
            applicationContext,
            userReceiver,
            IntentFilter(ACTION_USER_SWITCHED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registerObserverForCurrentUser()
        refresh()
    }

    override fun stop() {
        if (!started) return
        started = false
        runCatching { applicationContext.unregisterReceiver(userReceiver) }
        observedResolver?.let { resolver -> runCatching { resolver.unregisterContentObserver(observer) } }
        observedResolver = null
    }

    private fun registerObserverForCurrentUser() {
        observedResolver?.let { resolver -> runCatching { resolver.unregisterContentObserver(observer) } }
        observedResolver = currentUserProvider.context().contentResolver.also { resolver ->
            resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false,
                observer,
            )
            resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                false,
                observer,
            )
        }
    }

    override fun refresh() {
        val resolver = currentUserProvider.context().contentResolver
        runCatching {
            val value = Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS,
                DEFAULT_BRIGHTNESS,
            )
            val mode = Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            BrightnessState(
                progress = BrightnessMapper.toProgress(value),
                available = true,
                automatic = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
            )
        }.onSuccess { mutableState.value = it }
            .onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    available = false,
                    errorMessage = error.message,
                )
            }
    }

    override suspend fun setBrightness(progress: Float) {
        val normalized = progress.coerceIn(0f, 1f)
        mutableState.value = mutableState.value.copy(
            progress = normalized,
            automatic = false,
            available = true,
            errorMessage = null,
        )
        withContext(Dispatchers.IO) {
            val resolver = currentUserProvider.context().contentResolver
            runCatching {
                check(
                    Settings.System.putInt(
                        resolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    ),
                ) { "Unable to switch brightness to manual mode" }
                check(
                    Settings.System.putInt(
                        resolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        BrightnessMapper.toSetting(normalized),
                    ),
                ) { "Unable to write screen brightness" }
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(errorMessage = error.message)
            }
        }
    }

    private companion object {
        const val DEFAULT_BRIGHTNESS = 128
        const val ACTION_USER_SWITCHED = "android.intent.action.USER_SWITCHED"
    }
}
