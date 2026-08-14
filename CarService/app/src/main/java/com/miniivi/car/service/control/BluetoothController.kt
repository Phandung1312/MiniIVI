package com.miniivi.car.service.control

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.miniivi.car.api.BluetoothDeviceInfo
import com.miniivi.car.api.BluetoothFeatureState
import com.miniivi.car.api.CarServiceError
import com.miniivi.car.api.FeatureStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object BluetoothStateReducer {
    fun adapterChanged(state: BluetoothFeatureState, enabled: Boolean): BluetoothFeatureState =
        state.copy(
            status = FeatureStatus.READY,
            available = true,
            supported = true,
            enabled = enabled,
            discovering = if (enabled) state.discovering else false,
            nearbyDevices = if (enabled) state.nearbyDevices else emptyList(),
            connectedDevices = if (enabled) state.connectedDevices else emptyList(),
            errorCode = CarServiceError.NONE,
            diagnosticMessage = null,
        )

    fun discoveryStarted(state: BluetoothFeatureState): BluetoothFeatureState =
        state.copy(discovering = true, nearbyDevices = emptyList())

    fun discoveryFinished(state: BluetoothFeatureState): BluetoothFeatureState =
        state.copy(discovering = false)

    fun nearbyDevice(state: BluetoothFeatureState, device: BluetoothDeviceInfo): BluetoothFeatureState =
        state.copy(nearbyDevices = state.nearbyDevices.upsert(device))

    fun connectedDevice(state: BluetoothFeatureState, device: BluetoothDeviceInfo): BluetoothFeatureState =
        state.copy(connectedDevices = state.connectedDevices.upsert(device))

    fun disconnectedDevice(state: BluetoothFeatureState, address: String): BluetoothFeatureState =
        state.copy(connectedDevices = state.connectedDevices.filterNot { it.address == address })

    fun renamedDevice(state: BluetoothFeatureState, device: BluetoothDeviceInfo): BluetoothFeatureState =
        state.copy(
            pairedDevices = state.pairedDevices.replace(device),
            nearbyDevices = state.nearbyDevices.replace(device),
            connectedDevices = state.connectedDevices.replace(device),
        )

    private fun List<BluetoothDeviceInfo>.upsert(device: BluetoothDeviceInfo): List<BluetoothDeviceInfo> =
        (this + device).distinctBy(BluetoothDeviceInfo::address)

    private fun List<BluetoothDeviceInfo>.replace(device: BluetoothDeviceInfo): List<BluetoothDeviceInfo> =
        map { current -> if (current.address == device.address) device else current }
}

class BluetoothController(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val applicationContext = context.applicationContext
    private val adapter = applicationContext.getSystemService(BluetoothManager::class.java)?.adapter
    private val mutableState = MutableStateFlow(initialState())
    val state: StateFlow<BluetoothFeatureState> = mutableState.asStateFlow()
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scope.launch { handleIntent(intent) }
        }
    }

    fun start() {
        if (adapter == null) {
            publishUnavailable("Bluetooth is not supported")
            return
        }
        if (receiverRegistered) return
        Log.i(TAG, "event=controller_started feature=bluetooth")
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothDevice.ACTION_NAME_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                applicationContext.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        }
        refresh()
    }

    fun stop() {
        if (!receiverRegistered) return
        Log.i(TAG, "event=controller_stopping feature=bluetooth")
        if (receiverRegistered) runCatching { applicationContext.unregisterReceiver(receiver) }
        receiverRegistered = false
        Log.i(TAG, "event=controller_stopped feature=bluetooth")
    }

    @SuppressLint("MissingPermission")
    fun refresh() {
        val realAdapter = adapter ?: return publishUnavailable("Bluetooth is not supported")
        val previous = mutableState.value
        runCatching {
            val enabled = realAdapter.isEnabled
            mutableState.value.copy(
                status = FeatureStatus.READY,
                available = true,
                supported = true,
                enabled = enabled,
                discovering = enabled && realAdapter.isDiscovering,
                localName = realAdapter.name,
                localAddress = realAdapter.address.takeUnless { it == UNAVAILABLE_BLUETOOTH_ADDRESS },
                pairedDevices = realAdapter.bondedDevices.map { it.toInfo() }
                    .sortedBy { it.name ?: it.address },
                nearbyDevices = if (enabled) mutableState.value.nearbyDevices else emptyList(),
                connectedDevices = if (enabled) mutableState.value.connectedDevices else emptyList(),
                errorCode = CarServiceError.NONE,
                diagnosticMessage = null,
            )
        }.onSuccess {
            mutableState.value = it
            if (previous.status != it.status || previous.enabled != it.enabled) {
                Log.i(
                    TAG,
                    "event=state_changed feature=bluetooth status=${it.status} " +
                        "enabled=${it.enabled} paired_count=${it.pairedDevices.size}",
                )
            }
        }
            .onFailure { publishFailure("Unable to refresh Bluetooth state", it) }
    }

    @SuppressLint("MissingPermission")
    fun setEnabled(enabled: Boolean): Boolean {
        val realAdapter = adapter ?: return false
        return runCatching {
            @Suppress("DEPRECATION")
            if (enabled) realAdapter.enable() else realAdapter.disable()
        }.onSuccess { accepted ->
            logDebug("event=command_result command=set_bluetooth enabled=$enabled accepted=$accepted")
        }.onFailure { publishFailure("Unable to change Bluetooth state", it) }
            .getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun requestDiscovery(): Boolean {
        val realAdapter = adapter ?: return false
        if (!mutableState.value.enabled) {
            Log.w(TAG, "event=command_rejected command=bluetooth_discovery reason=disabled")
            return false
        }
        return runCatching {
            if (realAdapter.isDiscovering) realAdapter.cancelDiscovery()
            val accepted = realAdapter.startDiscovery()
            if (accepted) mutableState.update(BluetoothStateReducer::discoveryStarted)
            accepted
        }.onSuccess { accepted ->
            logDebug("event=command_result command=bluetooth_discovery accepted=$accepted")
        }.onFailure { publishFailure("Unable to start Bluetooth discovery", it) }
            .getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    fun renameLocalDevice(name: String): Boolean {
        val normalized = name.trim()
        val realAdapter = adapter ?: return false
        if (!mutableState.value.enabled || normalized.isEmpty()) {
            Log.w(
                TAG,
                "event=command_rejected command=bluetooth_rename reason=disabled_or_empty",
            )
            return false
        }
        return runCatching { realAdapter.setName(normalized) }
            .onSuccess { accepted ->
                if (accepted) mutableState.update { it.copy(localName = normalized) }
                logDebug(
                    "event=command_result command=bluetooth_rename accepted=$accepted " +
                        "name_length=${normalized.length}",
                )
            }
            .onFailure { publishFailure("Unable to rename the local Bluetooth device", it) }
            .getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.i(TAG, "event=adapter_state_changed feature=bluetooth enabled=true")
                        mutableState.update { BluetoothStateReducer.adapterChanged(it, true) }
                        refresh()
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        Log.i(TAG, "event=adapter_state_changed feature=bluetooth enabled=false")
                        mutableState.update { BluetoothStateReducer.adapterChanged(it, false) }
                    }
                }
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                Log.i(TAG, "event=discovery_started feature=bluetooth")
                mutableState.update(BluetoothStateReducer::discoveryStarted)
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                Log.i(
                    TAG,
                    "event=discovery_finished feature=bluetooth " +
                        "nearby_count=${mutableState.value.nearbyDevices.size}",
                )
                mutableState.update(BluetoothStateReducer::discoveryFinished)
            }
            BluetoothDevice.ACTION_FOUND -> intent.deviceInfo()?.let { device ->
                mutableState.update { BluetoothStateReducer.nearbyDevice(it, device) }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> intent.deviceInfo()?.let { device ->
                mutableState.update { BluetoothStateReducer.connectedDevice(it, device) }
                Log.i(
                    TAG,
                    "event=device_connection_changed feature=bluetooth connected=true " +
                        "connected_count=${mutableState.value.connectedDevices.size}",
                )
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> intent.deviceInfo()?.let { device ->
                mutableState.update { BluetoothStateReducer.disconnectedDevice(it, device.address) }
                Log.i(
                    TAG,
                    "event=device_connection_changed feature=bluetooth connected=false " +
                        "connected_count=${mutableState.value.connectedDevices.size}",
                )
            }
            BluetoothDevice.ACTION_NAME_CHANGED -> intent.deviceInfo()?.let { device ->
                mutableState.update { BluetoothStateReducer.renamedDevice(it, device) }
            }
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                intent.deviceInfo()?.let { device ->
                    mutableState.update { BluetoothStateReducer.renamedDevice(it, device) }
                }
                refresh()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun Intent.deviceInfo(): BluetoothDeviceInfo? {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return null
        return runCatching { device.toInfo() }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toInfo() = BluetoothDeviceInfo(
        address = address,
        name = name,
        bonded = bondState == BluetoothDevice.BOND_BONDED,
    )

    private fun initialState() = BluetoothFeatureState(
        status = if (adapter == null) FeatureStatus.UNAVAILABLE else FeatureStatus.CONNECTING,
        supported = adapter != null,
    )

    private fun publishUnavailable(message: String) {
        if (mutableState.value.status != FeatureStatus.UNAVAILABLE) {
            Log.w(TAG, "event=feature_unavailable feature=bluetooth reason=${message.toEventKey()}")
        }
        mutableState.value = BluetoothFeatureState(
            status = FeatureStatus.UNAVAILABLE,
            available = false,
            supported = false,
            errorCode = CarServiceError.PLATFORM_UNAVAILABLE,
            diagnosticMessage = message,
        )
    }

    private fun publishFailure(message: String, error: Throwable) {
        Log.e(
            TAG,
            "event=operation_failed feature=bluetooth operation=${message.toEventKey()}",
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

    private companion object {
        const val TAG = "MiniIviBluetooth"
        const val UNAVAILABLE_BLUETOOTH_ADDRESS = "02:00:00:00:00:00"
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }
}
