package com.miniivi.maps.preview

import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.miniivi.maps.MapLocationState
import com.miniivi.maps.MapAppearance
import com.miniivi.maps.MapsApplication
import com.miniivi.maps.OpenStreetMapView
import com.miniivi.maps.contract.IMapPreviewCallback
import com.miniivi.maps.contract.IMapPreviewService
import com.miniivi.maps.contract.MapPreviewContract
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MapPreviewService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessions = mutableMapOf<Long, PreviewSession>()
    private val nextSessionId = AtomicLong(1L)
    private val tracker by lazy { (application as MapsApplication).locationTracker }

    private val binder = object : IMapPreviewService.Stub() {
        override fun createPreview(
            hostToken: IBinder,
            displayId: Int,
            width: Int,
            height: Int,
            callback: IMapPreviewCallback,
        ) {
            handler.post {
                createPreviewOnMain(hostToken, displayId, width, height, callback)
            }
        }

        override fun resizePreview(sessionId: Long, width: Int, height: Int) {
            handler.post {
                if (width > 0 && height > 0) sessions[sessionId]?.surface?.resize(width, height)
            }
        }

        override fun releasePreview(sessionId: Long) {
            handler.post { releaseSession(sessionId) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "event=service_created feature=map_preview")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "event=service_bound action=${intent?.action ?: "none"}")
        return binder
    }

    override fun onDestroy() {
        Log.i(TAG, "event=service_destroying active_sessions=${sessions.size}")
        sessions.keys.toList().forEach(::releaseSession)
        scope.cancel()
        super.onDestroy()
        Log.i(TAG, "event=service_destroyed feature=map_preview")
    }

    private fun createPreviewOnMain(
        hostToken: IBinder,
        displayId: Int,
        requestedWidth: Int,
        requestedHeight: Int,
        callback: IMapPreviewCallback,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(TAG, "event=preview_rejected reason=unsupported_platform")
            callback.sendError(
                MapPreviewContract.ERROR_UNSUPPORTED_PLATFORM,
                "Embedded map preview requires Android 10 or newer",
            )
            return
        }
        val width = requestedWidth.coerceAtLeast(1)
        val height = requestedHeight.coerceAtLeast(1)
        val display = getSystemService(DisplayManager::class.java).getDisplay(displayId)
        if (display == null || !hostToken.isBinderAlive) {
            Log.w(
                TAG,
                "event=preview_rejected reason=invalid_host display_id=$displayId " +
                    "host_alive=${hostToken.isBinderAlive}",
            )
            callback.sendError(MapPreviewContract.ERROR_INVALID_HOST, "Invalid preview host")
            return
        }

        val sessionId = nextSessionId.getAndIncrement()
        val mapView = OpenStreetMapView(this).apply {
            appearance = MapAppearance.DASHBOARD_DARK
            gesturesEnabled = false
            isClickable = false
            isFocusable = false
        }
        val surface = runCatching {
            PreviewSurfaceFactory.create(this, display, hostToken, mapView, width, height)
        }.onFailure { error ->
            Log.e(TAG, "event=surface_creation_failed display_id=$displayId", error)
        }.getOrNull()
        if (surface == null) {
            callback.sendError(
                MapPreviewContract.ERROR_RENDERER_UNAVAILABLE,
                "Unable to create the map renderer",
            )
            return
        }

        val deathRecipient = IBinder.DeathRecipient {
            Log.w(TAG, "event=preview_host_died session_id=$sessionId")
            handler.post { releaseSession(sessionId) }
        }
        runCatching { callback.asBinder().linkToDeath(deathRecipient, 0) }
            .onFailure {
                surface.release()
                callback.sendError(MapPreviewContract.ERROR_INVALID_HOST, "Preview host disconnected")
                return
            }

        if (sessions.isEmpty()) tracker.start()
        val session = PreviewSession(
            id = sessionId,
            callback = callback,
            callbackBinder = callback.asBinder(),
            deathRecipient = deathRecipient,
            mapView = mapView,
            surface = surface,
        )
        sessions[sessionId] = session
        Log.i(
            TAG,
            "event=session_created session_id=$sessionId display_id=$displayId width=$width " +
                "height=$height active_sessions=${sessions.size}",
        )
        mapView.onInitialLoadFailed = {
            session.tileFailed = true
            Log.w(TAG, "event=session_tile_unavailable session_id=$sessionId")
            session.sendState(MapPreviewContract.STATE_TILE_UNAVAILABLE)
        }
        mapView.onLoadingChanged = { loading ->
            if (!loading && session.tileFailed) {
                session.tileFailed = false
                Log.i(TAG, "event=session_tile_recovered session_id=$sessionId")
                session.sendState(session.locationState)
            }
        }
        val previewDelivered = runCatching {
            callback.onPreviewReady(sessionId, surface.surfacePackage())
        }.onFailure {
                Log.w(TAG, "event=preview_delivery_failed session_id=$sessionId", it)
                releaseSession(sessionId)
            }.isSuccess
        if (previewDelivered) {
            Log.i(TAG, "event=preview_delivered session_id=$sessionId")
            session.locationJob = scope.launch {
                tracker.state.collectLatest { state -> updateLocation(session, state) }
            }
        }
    }

    private fun updateLocation(session: PreviewSession, state: MapLocationState) {
        state.position?.let { position ->
            session.mapView.showLocation(position.latitude, position.longitude)
        }
        val previousState = session.locationState
        session.locationState = when {
            !state.permissionGranted -> MapPreviewContract.STATE_LOCATION_UNAVAILABLE
            state.hasLiveFix -> MapPreviewContract.STATE_READY
            state.position != null -> MapPreviewContract.STATE_LAST_KNOWN
            state.locating -> MapPreviewContract.STATE_LOCATING
            else -> MapPreviewContract.STATE_LOCATION_UNAVAILABLE
        }
        if (previousState != session.locationState) {
            Log.i(
                TAG,
                "event=session_location_state_changed session_id=${session.id} " +
                    "from=$previousState to=${session.locationState}",
            )
        }
        if (!session.tileFailed) session.sendState(session.locationState)
    }

    private fun releaseSession(sessionId: Long) {
        val session = sessions.remove(sessionId) ?: return
        session.locationJob?.cancel()
        runCatching {
            session.callbackBinder.unlinkToDeath(session.deathRecipient, 0)
        }
        session.surface.release()
        Log.i(
            TAG,
            "event=session_released session_id=$sessionId active_sessions=${sessions.size}",
        )
        if (sessions.isEmpty()) tracker.stop()
    }

    private fun IMapPreviewCallback.sendError(code: Int, message: String) {
        runCatching { onPreviewError(code, message) }
            .onFailure { error ->
                if (error !is RemoteException) {
                    Log.w(TAG, "event=preview_error_delivery_failed error_code=$code", error)
                }
            }
    }

    private data class PreviewSession(
        val id: Long,
        val callback: IMapPreviewCallback,
        val callbackBinder: IBinder,
        val deathRecipient: IBinder.DeathRecipient,
        val mapView: OpenStreetMapView,
        val surface: PreviewSurface,
        var locationJob: Job? = null,
        var locationState: Int = MapPreviewContract.STATE_LOCATING,
        var tileFailed: Boolean = false,
    ) {
        fun sendState(state: Int) {
            runCatching { callback.onPreviewStateChanged(id, state) }
        }
    }

    private companion object {
        const val TAG = "MiniIviMapPreview"
    }
}
