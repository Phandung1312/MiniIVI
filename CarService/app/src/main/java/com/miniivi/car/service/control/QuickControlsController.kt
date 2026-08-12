package com.miniivi.car.service.control

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TetheringManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.miniivi.car.api.CarServiceError
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.QuickControl
import com.miniivi.car.api.QuickControlsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class QuickControlBackend { REAL, MOCK }

internal object QuickControlsPolicy {
    fun backend(realAvailable: Boolean): QuickControlBackend =
        if (realAvailable) QuickControlBackend.REAL else QuickControlBackend.MOCK

    fun shouldUseMockAfterRealFailure(realAvailable: Boolean): Boolean = !realAvailable
}

class QuickControlsController(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val wifiManager = applicationContext.getSystemService(WifiManager::class.java)
    private val bluetoothAdapter = applicationContext
        .getSystemService(BluetoothManager::class.java)?.adapter
    private val tetheringManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        applicationContext.getSystemService(TetheringManager::class.java)
    } else {
        null
    }
    private val powerManager = applicationContext.getSystemService(PowerManager::class.java)
    private val goToSleepMethod = powerManager?.javaClass?.methods?.firstOrNull {
        it.name == "goToSleep" && it.parameterTypes.contentEquals(arrayOf(Long::class.javaPrimitiveType))
    }
    private var receiverRegistered = false

    private val mutableState = MutableStateFlow(restoredState())
    val state: StateFlow<QuickControlsState> = mutableState.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    fun start() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                applicationContext.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        }
        refresh()
    }

    fun stop() {
        if (receiverRegistered) runCatching { applicationContext.unregisterReceiver(receiver) }
        receiverRegistered = false
    }

    fun refresh() {
        val capabilities = realCapabilities()
        mutableState.update { current ->
            current.copy(
                status = FeatureStatus.READY,
                available = true,
                wifiEnabled = if (capabilities and QuickControl.WIFI_CAPABILITY != 0L) {
                    runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(current.wifiEnabled)
                } else current.wifiEnabled,
                bluetoothEnabled = if (capabilities and QuickControl.BLUETOOTH_CAPABILITY != 0L) {
                    runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(current.bluetoothEnabled)
                } else current.bluetoothEnabled,
                realCapabilities = capabilities,
                errorCode = CarServiceError.NONE,
                diagnosticMessage = null,
            )
        }
    }

    fun setEnabled(control: Int, enabled: Boolean) {
        when (control) {
            QuickControl.WIFI -> setWifiEnabled(enabled)
            QuickControl.BLUETOOTH -> setBluetoothEnabled(enabled)
            QuickControl.HOTSPOT -> setHotspotEnabled(enabled)
            QuickControl.VALET_MODE -> updatePersisted(control, enabled)
            else -> publishArgumentError("Unknown quick control $control")
        }
    }

    fun requestScreenOff() {
        val manager = powerManager
        val method = goToSleepMethod
        if (manager == null || method == null) return
        runCatching { method.invoke(manager, SystemClock.uptimeMillis()) }
            .onFailure { publishFailure("Unable to turn the display off", it) }
    }

    private fun setWifiEnabled(enabled: Boolean) {
        if (QuickControlsPolicy.backend(wifiManager != null) == QuickControlBackend.MOCK) {
            updatePersisted(QuickControl.WIFI, enabled)
            return
        }
        scope.launch {
            runCatching {
                @Suppress("DEPRECATION")
                check(wifiManager.setWifiEnabled(enabled)) { "Wi-Fi request was rejected" }
            }.onSuccess { updateState(QuickControl.WIFI, enabled, persist = false) }
                .onFailure { publishFailure("Unable to change Wi-Fi state", it) }
        }
    }

    private fun setBluetoothEnabled(enabled: Boolean) {
        val adapter = bluetoothAdapter
        if (QuickControlsPolicy.backend(adapter != null) == QuickControlBackend.MOCK) {
            updatePersisted(QuickControl.BLUETOOTH, enabled)
            return
        }
        val realAdapter = checkNotNull(adapter)
        scope.launch {
            runCatching {
                @Suppress("DEPRECATION")
                val accepted = if (enabled) realAdapter.enable() else realAdapter.disable()
                check(accepted) { "Bluetooth request was rejected" }
            }.onSuccess { updateState(QuickControl.BLUETOOTH, enabled, persist = false) }
                .onFailure { publishFailure("Unable to change Bluetooth state", it) }
        }
    }

    private fun setHotspotEnabled(enabled: Boolean) {
        val manager = tetheringManager
        if (QuickControlsPolicy.backend(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && manager != null) == QuickControlBackend.MOCK) {
            updatePersisted(QuickControl.HOTSPOT, enabled)
            return
        }
        val realManager = checkNotNull(manager)
        scope.launch {
            runCatching {
                if (enabled) {
                    val request = TetheringManager.TetheringRequest.Builder(
                        TetheringManager.TETHERING_WIFI,
                    ).build()
                    realManager.startTethering(
                        request,
                        applicationContext.mainExecutor,
                        object : TetheringManager.StartTetheringCallback {
                            override fun onTetheringFailed(error: Int) {
                                publishFailure(
                                    "Unable to enable hotspot",
                                    IllegalStateException("Tethering error $error"),
                                )
                            }
                        },
                    )
                } else {
                    val request = TetheringManager.TetheringRequest.Builder(
                        TetheringManager.TETHERING_WIFI,
                    ).build()
                    realManager.stopTethering(
                        request,
                        applicationContext.mainExecutor,
                        object : TetheringManager.StopTetheringCallback {},
                    )
                }
            }.onSuccess { updateState(QuickControl.HOTSPOT, enabled, persist = false) }
                .onFailure { publishFailure("Unable to change hotspot state", it) }
        }
    }

    private fun updatePersisted(control: Int, enabled: Boolean) {
        preferences.edit().putBoolean(key(control), enabled).apply()
        updateState(control, enabled, persist = false)
    }

    private fun updateState(control: Int, enabled: Boolean, persist: Boolean) {
        if (persist) preferences.edit().putBoolean(key(control), enabled).apply()
        mutableState.update {
            when (control) {
                QuickControl.WIFI -> it.copy(wifiEnabled = enabled)
                QuickControl.BLUETOOTH -> it.copy(bluetoothEnabled = enabled)
                QuickControl.HOTSPOT -> it.copy(hotspotEnabled = enabled)
                QuickControl.VALET_MODE -> it.copy(valetModeEnabled = enabled)
                else -> it
            }.copy(
                status = FeatureStatus.READY,
                available = true,
                errorCode = CarServiceError.NONE,
                diagnosticMessage = null,
            )
        }
    }

    private fun restoredState() = QuickControlsState(
        status = FeatureStatus.READY,
        available = true,
        wifiEnabled = preferences.getBoolean(key(QuickControl.WIFI), false),
        bluetoothEnabled = preferences.getBoolean(key(QuickControl.BLUETOOTH), false),
        hotspotEnabled = preferences.getBoolean(key(QuickControl.HOTSPOT), false),
        valetModeEnabled = preferences.getBoolean(key(QuickControl.VALET_MODE), false),
        realCapabilities = realCapabilities(),
    )

    private fun realCapabilities(): Long {
        var result = 0L
        if (wifiManager != null) result = result or QuickControl.WIFI_CAPABILITY
        if (bluetoothAdapter != null) result = result or QuickControl.BLUETOOTH_CAPABILITY
        if (tetheringManager != null) result = result or QuickControl.HOTSPOT_CAPABILITY
        if (goToSleepMethod != null) result = result or QuickControl.SCREEN_OFF_CAPABILITY
        return result
    }

    private fun publishArgumentError(message: String) {
        mutableState.update {
            it.copy(
                status = FeatureStatus.ERROR,
                errorCode = CarServiceError.INVALID_ARGUMENT,
                diagnosticMessage = message,
            )
        }
    }

    private fun publishFailure(message: String, error: Throwable) {
        Log.e(TAG, message, error)
        mutableState.update {
            it.copy(
                status = FeatureStatus.ERROR,
                errorCode = CarServiceError.PLATFORM_OPERATION_FAILED,
                diagnosticMessage = "$message: ${error.message}",
            )
        }
    }

    private fun key(control: Int) = "control_$control"

    private companion object {
        const val TAG = "MiniIviQuickControls"
        const val PREFERENCES = "quick_controls"
    }
}
