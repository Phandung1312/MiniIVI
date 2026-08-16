package com.android.car.launcher.feature.maps.presentation.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceControlViewHost.SurfacePackage
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.android.car.launcher.feature.maps.presentation.model.MapPreviewUiState
import com.miniivi.maps.contract.IMapPreviewCallback
import com.miniivi.maps.contract.IMapPreviewService
import com.miniivi.maps.contract.MapPreviewContract

internal class MapPreviewHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    var onStateChanged: (MapPreviewUiState) -> Unit = {}
        set(value) {
            field = value
            value(state)
        }

    private val surfaceView = SurfaceView(context).apply {
        isClickable = false
        isFocusable = false
    }
    private var state = MapPreviewUiState.CONNECTING
    private var service: IMapPreviewService? = null
    private var serviceBound = false
    private var requestPending = false
    private var sessionId = NO_SESSION
    private var surfacePackage: SurfacePackage? = null
    private var lifecycleOwner: LifecycleOwner? = null

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            post(::connect)
        }

        override fun onStop(owner: LifecycleOwner) {
            disconnect()
        }
    }

    private val callback = object : IMapPreviewCallback.Stub() {
        override fun onPreviewReady(id: Long, bundle: Bundle) {
            post { attachSurfacePackage(id, bundle) }
        }

        override fun onPreviewStateChanged(id: Long, remoteState: Int) {
            post {
                if (sessionId == id) setState(remoteState.toUiState())
            }
        }

        override fun onPreviewError(errorCode: Int, message: String?) {
            Log.w(TAG, "event=preview_failed error_code=$errorCode")
            post {
                requestPending = false
                setState(MapPreviewUiState.UNAVAILABLE)
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            Log.i(TAG, "event=service_connected component=${name.flattenToShortString()}")
            service = IMapPreviewService.Stub.asInterface(binder)
            requestPreviewIfReady()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.w(TAG, "event=service_disconnected component=${name.flattenToShortString()}")
            service = null
            resetPreview(MapPreviewUiState.UNAVAILABLE)
        }

        override fun onBindingDied(name: ComponentName) {
            Log.w(TAG, "event=binding_died component=${name.flattenToShortString()}")
            disconnect()
            if (isAttachedToWindow) post(::connect)
        }

        override fun onNullBinding(name: ComponentName) {
            Log.w(TAG, "event=null_binding component=${name.flattenToShortString()}")
            service = null
            setState(MapPreviewUiState.UNAVAILABLE)
        }
    }

    init {
        addView(
            surfaceView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.TRANSPARENT)
            cornerRadius = 24f * resources.displayMetrics.density
        }
        clipToOutline = true
        contentDescription = "Live navigation map"
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleOwner = findViewTreeLifecycleOwner().also { owner ->
            owner?.lifecycle?.addObserver(lifecycleObserver)
        }
        val lifecycle = lifecycleOwner?.lifecycle
        if (lifecycle == null || lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            post(::connect)
        }
    }

    override fun onDetachedFromWindow() {
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
        disconnect()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && isAttachedToWindow) {
            post(::connect)
        } else if (visibility != VISIBLE) {
            disconnect()
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return
        if (sessionId == NO_SESSION) {
            requestPreviewIfReady()
        } else {
            runCatching { service?.resizePreview(sessionId, width, height) }
        }
    }

    private fun connect() {
        if (serviceBound || !isAttachedToWindow) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            setState(MapPreviewUiState.UNAVAILABLE)
            return
        }
        setState(MapPreviewUiState.CONNECTING)
        logDebug("event=bind_requested service=map_preview")
        val intent = Intent().setComponent(
            ComponentName(
                MapPreviewContract.MAPS_PACKAGE,
                MapPreviewContract.PREVIEW_SERVICE,
            ),
        )
        serviceBound = runCatching {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.onFailure { error ->
            Log.w(TAG, "event=bind_failed service=map_preview", error)
        }.getOrDefault(false)
        if (!serviceBound) setState(MapPreviewUiState.UNAVAILABLE)
    }

    private fun disconnect() {
        val activeSession = sessionId
        if (activeSession != NO_SESSION) {
            runCatching { service?.releasePreview(activeSession) }
            logDebug("event=session_release_requested session_id=$activeSession")
        }
        resetPreview(MapPreviewUiState.CONNECTING)
        service = null
        if (serviceBound) {
            runCatching { context.unbindService(connection) }
            serviceBound = false
            logDebug("event=service_unbound service=map_preview")
        }
    }

    private fun requestPreviewIfReady() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || requestPending) return
        val previewService = service ?: return
        val hostToken = surfaceView.hostToken ?: return
        if (width <= 0 || height <= 0) return
        requestPending = true
        setState(MapPreviewUiState.LOCATING)
        val displayId = display?.displayId
        if (displayId == null) {
            requestPending = false
            setState(MapPreviewUiState.UNAVAILABLE)
            return
        }
        runCatching {
            previewService.createPreview(hostToken, displayId, width, height, callback)
            logDebug(
                "event=preview_requested display_id=$displayId width=$width height=$height",
            )
        }.onFailure { error ->
            Log.w(TAG, "event=preview_request_failed", error)
            requestPending = false
            setState(MapPreviewUiState.UNAVAILABLE)
        }
    }

    @RequiresApi(29)
    private fun attachSurfacePackage(id: Long, bundle: Bundle) {
        val childSurface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(MapPreviewContract.SURFACE_PACKAGE_KEY, SurfacePackage::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(MapPreviewContract.SURFACE_PACKAGE_KEY)
        }
        if (childSurface == null) {
            requestPending = false
            setState(MapPreviewUiState.UNAVAILABLE)
            return
        }
        surfacePackage?.release()
        surfacePackage = childSurface
        sessionId = id
        requestPending = false
        surfaceView.setChildSurfacePackage(childSurface)
        Log.i(TAG, "event=preview_ready session_id=$id")
    }

    private fun resetPreview(nextState: MapPreviewUiState) {
        requestPending = false
        sessionId = NO_SESSION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            surfacePackage?.release()
        }
        surfacePackage = null
        setState(nextState)
    }

    private fun setState(value: MapPreviewUiState) {
        if (state == value) return
        val previous = state
        state = value
        Log.i(
            TAG,
            "event=preview_state_changed from=${previous.name.lowercase()} to=${value.name.lowercase()}",
        )
        onStateChanged(value)
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private fun Int.toUiState(): MapPreviewUiState = when (this) {
        MapPreviewContract.STATE_LOCATING -> MapPreviewUiState.LOCATING
        MapPreviewContract.STATE_READY -> MapPreviewUiState.READY
        MapPreviewContract.STATE_LAST_KNOWN -> MapPreviewUiState.LAST_KNOWN
        MapPreviewContract.STATE_LOCATION_UNAVAILABLE -> MapPreviewUiState.LOCATION_UNAVAILABLE
        MapPreviewContract.STATE_TILE_UNAVAILABLE -> MapPreviewUiState.TILE_UNAVAILABLE
        else -> MapPreviewUiState.UNAVAILABLE
    }

    private companion object {
        const val TAG = "MiniIviMapPreview"
        const val NO_SESSION = 0L
    }
}
