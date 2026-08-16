package com.miniivi.car.service.control

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import com.miniivi.car.service.framework.FrameworkPlatformApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
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
    private val bluetoothController: BluetoothController,
) {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.createDeviceProtectedStorageContext()
        .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val wifiManager = applicationContext.getSystemService(WifiManager::class.java)
    private val connectivityManager = applicationContext.getSystemService(ConnectivityManager::class.java)
    private val tetheringManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        applicationContext.getSystemService(TetheringManager::class.java)
    } else {
        null
    }
    private val powerManager = applicationContext.getSystemService(PowerManager::class.java)
    private var receiverRegistered = false
    private var networkCallbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshWifiConnection()

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            updateWifiConnection(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
        }

        override fun onLost(network: Network) = refreshWifiConnection()
    }

    private val mutableState = MutableStateFlow(restoredState())
    val state: StateFlow<QuickControlsState> = mutableState.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    init {
        scope.launch {
            bluetoothController.state.collect { bluetooth ->
                mutableState.update {
                    it.copy(
                        bluetoothEnabled = bluetooth.enabled,
                        bluetoothConnected = bluetooth.enabled && bluetooth.connectedDevices.isNotEmpty(),
                        realCapabilities = realCapabilities(),
                    )
                }
            }
        }
    }

    fun start() {
        if (receiverRegistered) return
        Log.i(TAG, "event=controller_started feature=quick_controls")
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                applicationContext.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        }
        if (!networkCallbackRegistered) {
            runCatching {
                connectivityManager?.registerDefaultNetworkCallback(networkCallback)
                networkCallbackRegistered = connectivityManager != null
            }
        }
        refresh()
    }

    fun stop() {
        if (!receiverRegistered) return
        Log.i(TAG, "event=controller_stopping feature=quick_controls")
        if (receiverRegistered) runCatching { applicationContext.unregisterReceiver(receiver) }
        receiverRegistered = false
        if (networkCallbackRegistered) {
            runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
            networkCallbackRegistered = false
        }
        Log.i(TAG, "event=controller_stopped feature=quick_controls")
    }

    fun refresh() {
        bluetoothController.refresh()
        val capabilities = realCapabilities()
        val previousCapabilities = mutableState.value.realCapabilities
        mutableState.update { current ->
            current.copy(
                status = FeatureStatus.READY,
                available = true,
                wifiEnabled = if (capabilities and QuickControl.WIFI_CAPABILITY != 0L) {
                    runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(current.wifiEnabled)
                } else current.wifiEnabled,
                wifiConnected = if (capabilities and QuickControl.WIFI_CAPABILITY != 0L) {
                    isWifiConnected()
                } else false,
                bluetoothEnabled = bluetoothController.state.value.enabled,
                bluetoothConnected = bluetoothController.state.value.enabled &&
                    bluetoothController.state.value.connectedDevices.isNotEmpty(),
                realCapabilities = capabilities,
                errorCode = CarServiceError.NONE,
                diagnosticMessage = null,
            )
        }
        if (capabilities != previousCapabilities) {
            Log.i(
                TAG,
                "event=capabilities_changed feature=quick_controls capabilities=0x" +
                    capabilities.toString(16),
            )
        }
    }

    fun setEnabled(control: Int, enabled: Boolean) {
        logDebug("event=command_received feature=quick_controls control=$control enabled=$enabled")
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
        if (manager == null) {
            Log.w(TAG, "event=command_rejected command=request_screen_off reason=unsupported")
            return
        }
        runCatching { FrameworkPlatformApi.goToSleep(manager, SystemClock.uptimeMillis()) }
            .onSuccess { logDebug("event=command_applied command=request_screen_off") }
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
        if (!bluetoothController.state.value.supported) {
            publishFailure(
                "Unable to change Bluetooth state",
                IllegalStateException("Bluetooth is not supported"),
            )
            return
        }
        scope.launch {
            if (!bluetoothController.setEnabled(enabled)) {
                publishFailure(
                    "Unable to change Bluetooth state",
                    IllegalStateException("Bluetooth request was rejected"),
                )
            }
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
                QuickControl.WIFI -> it.copy(
                    wifiEnabled = enabled,
                    wifiConnected = enabled && isWifiConnected(),
                )
                QuickControl.BLUETOOTH -> it.copy(
                    bluetoothEnabled = enabled,
                    bluetoothConnected = enabled && bluetoothController.state.value.connectedDevices.isNotEmpty(),
                )
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
        wifiConnected = false,
        bluetoothEnabled = bluetoothController.state.value.enabled,
        bluetoothConnected = bluetoothController.state.value.enabled &&
            bluetoothController.state.value.connectedDevices.isNotEmpty(),
        hotspotEnabled = preferences.getBoolean(key(QuickControl.HOTSPOT), false),
        valetModeEnabled = preferences.getBoolean(key(QuickControl.VALET_MODE), false),
        realCapabilities = realCapabilities(),
    )

    private fun realCapabilities(): Long {
        var result = 0L
        if (wifiManager != null) result = result or QuickControl.WIFI_CAPABILITY
        if (bluetoothController.state.value.supported) result = result or QuickControl.BLUETOOTH_CAPABILITY
        if (tetheringManager != null) result = result or QuickControl.HOTSPOT_CAPABILITY
        if (powerManager != null) result = result or QuickControl.SCREEN_OFF_CAPABILITY
        return result
    }

    private fun isWifiConnected(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun refreshWifiConnection() = updateWifiConnection(isWifiConnected())

    private fun updateWifiConnection(connected: Boolean) {
        mutableState.update { current ->
            current.copy(wifiConnected = current.wifiEnabled && connected)
        }
    }

    private fun publishArgumentError(message: String) {
        Log.w(TAG, "event=command_rejected feature=quick_controls reason=${message.toEventKey()}")
        mutableState.update {
            it.copy(
                status = FeatureStatus.ERROR,
                errorCode = CarServiceError.INVALID_ARGUMENT,
                diagnosticMessage = message,
            )
        }
    }

    private fun publishFailure(message: String, error: Throwable) {
        Log.e(
            TAG,
            "event=operation_failed feature=quick_controls operation=${message.toEventKey()}",
            error,
        )
        mutableState.update {
            it.copy(
                status = FeatureStatus.ERROR,
                errorCode = CarServiceError.PLATFORM_OPERATION_FAILED,
                diagnosticMessage = "$message: ${error.message}",
            )
        }
    }

    private fun key(control: Int) = "control_$control"

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "MiniIviQuickControls"
        const val PREFERENCES = "quick_controls"
    }
}
