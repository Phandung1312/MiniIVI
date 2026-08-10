package com.miniivi.car.service.control

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import androidx.core.content.ContextCompat
import com.miniivi.car.api.BrightnessState
import com.miniivi.car.api.CarServiceError
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.service.framework.FrameworkPlatformApi
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal object BrightnessPolicy {
    fun linearToProgress(value: Float, minimum: Float, maximum: Float): Float {
        if (maximum <= minimum) return 0f
        val normalized = ((value - minimum) / (maximum - minimum)).coerceIn(0f, 1f) * 12f
        return if (normalized <= 1f) {
            sqrt(normalized) * R
        } else {
            A * ln(normalized - B) + C
        }.coerceIn(0f, 1f)
    }

    fun progressToLinear(progress: Float, minimum: Float, maximum: Float): Float {
        if (maximum <= minimum) return minimum
        val gamma = progress.coerceIn(0f, 1f)
        val normalized = if (gamma <= R) {
            (gamma / R) * (gamma / R)
        } else {
            exp((gamma - C) / A) + B
        } / 12f
        return minimum + normalized.coerceIn(0f, 1f) * (maximum - minimum)
    }

    fun settingToProgress(value: Int, minimum: Int, maximum: Int): Float =
        linearToProgress(value.toFloat(), minimum.toFloat(), maximum.toFloat())

    fun progressToSetting(progress: Float, minimum: Int, maximum: Int): Int =
        progressToLinear(progress, minimum.toFloat(), maximum.toFloat()).toInt()
            .coerceIn(minimum, maximum)

    private const val R = 0.5f
    private const val A = 0.17883277f
    private const val B = 0.28466892f
    private const val C = 0.55991073f
}

class BrightnessController(
    private val applicationContext: Context,
    private val currentUserProvider: CurrentUserProvider,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(BrightnessState())
    val state: StateFlow<BrightnessState> = mutableState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val displayManager = applicationContext.getSystemService(DisplayManager::class.java)
    private val powerManager = applicationContext.getSystemService(PowerManager::class.java)
    private var started = false
    private var observedResolver: ContentResolver? = null

    private val observer = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) refresh()
        }
    }

    private val userReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            registerObserverForCurrentUser()
            refresh()
        }
    }

    fun start() {
        if (started) return
        started = true
        ContextCompat.registerReceiver(
            applicationContext,
            userReceiver,
            IntentFilter(ACTION_USER_SWITCHED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registerObserverForCurrentUser()
        registerDisplayListener()
        refresh()
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { applicationContext.unregisterReceiver(userReceiver) }
        runCatching { displayManager?.unregisterDisplayListener(displayListener) }
        observedResolver?.let { resolver -> runCatching { resolver.unregisterContentObserver(observer) } }
        observedResolver = null
    }

    fun refresh() {
        scope.launch {
            runCatching {
                val resolver = currentUserProvider.context().contentResolver
                val mode = Settings.System.getInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
                BrightnessState(
                    status = FeatureStatus.READY,
                    available = true,
                    progress = readDisplayBrightnessProgress() ?: readSettingProgress(resolver),
                    automatic = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                )
            }.onSuccess { mutableState.value = it }
                .onFailure { error -> publishError("Unable to read display brightness", error) }
        }
    }

    fun setProgress(progress: Float) {
        if (!progress.isFinite()) {
            mutableState.value = mutableState.value.copy(
                status = FeatureStatus.ERROR,
                errorCode = CarServiceError.INVALID_ARGUMENT,
                diagnosticMessage = "Brightness must be finite",
            )
            return
        }
        val normalized = progress.coerceIn(0f, 1f)
        scope.launch {
            val resolver = runCatching { currentUserProvider.context().contentResolver }
                .getOrElse { error ->
                    publishError("Unable to resolve the foreground user", error)
                    return@launch
                }
            runCatching {
                check(
                    Settings.System.putInt(
                        resolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    ),
                ) { "Unable to switch brightness to manual mode" }
                if (!setDisplayBrightness(normalized)) {
                    val (minimum, maximum) = settingRange()
                    check(
                        Settings.System.putInt(
                            resolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            BrightnessPolicy.progressToSetting(normalized, minimum, maximum),
                        ),
                    ) { "Unable to write screen brightness" }
                }
                mutableState.value = mutableState.value.copy(
                    status = FeatureStatus.READY,
                    available = true,
                    progress = normalized,
                    automatic = false,
                    errorCode = CarServiceError.NONE,
                    diagnosticMessage = null,
                )
            }.onFailure { error -> publishError("Unable to set display brightness", error) }
        }
    }

    private fun registerObserverForCurrentUser() {
        observedResolver?.let { resolver -> runCatching { resolver.unregisterContentObserver(observer) } }
        observedResolver = runCatching {
            currentUserProvider.context().contentResolver.also { resolver ->
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
        }.onFailure { error -> publishError("Unable to observe display brightness", error) }
            .getOrNull()
    }

    private fun registerDisplayListener() {
        val manager = displayManager ?: return
        val registeredWithBrightnessSignal = runCatching {
            val method = DisplayManager::class.java.getMethod(
                "registerDisplayListener",
                DisplayManager.DisplayListener::class.java,
                Handler::class.java,
                Long::class.javaPrimitiveType,
            )
            method.invoke(manager, displayListener, mainHandler, EVENT_TYPE_DISPLAY_BRIGHTNESS)
        }.isSuccess
        if (!registeredWithBrightnessSignal) {
            runCatching { manager.registerDisplayListener(displayListener, mainHandler) }
        }
    }

    private fun readDisplayBrightnessProgress(): Float? {
        val brightness = runCatching {
            displayManager?.let { FrameworkPlatformApi.getBrightness(it, Display.DEFAULT_DISPLAY) }
        }.getOrNull() ?: return null
        return brightness.takeIf(Float::isFinite)?.let {
            BrightnessPolicy.linearToProgress(it, DISPLAY_MINIMUM, DISPLAY_MAXIMUM)
        }
    }

    private fun readSettingProgress(resolver: ContentResolver): Float {
        val (minimum, maximum) = settingRange()
        val value = Settings.System.getInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            (minimum + maximum) / 2,
        )
        return BrightnessPolicy.settingToProgress(value, minimum, maximum)
    }

    private fun setDisplayBrightness(progress: Float): Boolean {
        val manager = displayManager ?: return false
        val linear = BrightnessPolicy.progressToLinear(
            progress,
            DISPLAY_MINIMUM,
            DISPLAY_MAXIMUM,
        )
        return runCatching {
            FrameworkPlatformApi.setBrightness(manager, Display.DEFAULT_DISPLAY, linear)
        }.isSuccess
    }

    private fun settingRange(): Pair<Int, Int> {
        fun invoke(name: String, fallback: Int): Int = runCatching {
            PowerManager::class.java.getMethod(name).invoke(powerManager) as Int
        }.getOrDefault(fallback)
        val minimum = invoke("getMinimumScreenBrightnessSetting", FALLBACK_MINIMUM)
        val maximum = invoke("getMaximumScreenBrightnessSetting", FALLBACK_MAXIMUM)
        return minimum.coerceAtMost(maximum) to maximum.coerceAtLeast(minimum)
    }

    private fun publishError(message: String, error: Throwable) {
        mutableState.value = mutableState.value.copy(
            status = FeatureStatus.ERROR,
            available = false,
            errorCode = CarServiceError.PLATFORM_OPERATION_FAILED,
            diagnosticMessage = "$message: ${error.message}",
        )
    }

    private companion object {
        const val FALLBACK_MINIMUM = 1
        const val FALLBACK_MAXIMUM = 255
        const val DISPLAY_MINIMUM = 0f
        const val DISPLAY_MAXIMUM = 1f
        const val EVENT_TYPE_DISPLAY_BRIGHTNESS = 1L shl 5
        const val ACTION_USER_SWITCHED = "android.intent.action.USER_SWITCHED"
    }
}
