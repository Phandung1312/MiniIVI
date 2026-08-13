package com.android.car.systemui.boot

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager

class BootHandoffService : Service() {
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val stateMachine = BootHandoffStateMachine()
    private var handoffView: IviShimmerView? = null
    private val timeout = Runnable {
        Log.w(TAG, "Removing IVI handoff after the fail-safe timeout")
        removeImmediately()
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showHandoff()
            ACTION_DISMISS -> dismissHandoff()
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun showHandoff() {
        if (!stateMachine.requestShow()) return
        val view = IviShimmerView(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            TYPE_BOOT_PROGRESS,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.OPAQUE,
        ).apply {
            title = "MiniIVI Boot Handoff"
        }
        try {
            windowManager.addView(view, params)
            handoffView = view
            handler.postDelayed(timeout, FAIL_SAFE_TIMEOUT_MILLIS)
            Log.i(TAG, "IVI boot handoff is visible")
        } catch (exception: RuntimeException) {
            stateMachine.completeRemoval()
            Log.e(TAG, "Unable to add the IVI boot handoff window", exception)
            stopSelf()
        }
    }

    private fun dismissHandoff() {
        val view = handoffView
        if (view == null) {
            stopSelf()
            return
        }
        if (!stateMachine.requestDismiss()) return
        handler.removeCallbacks(timeout)
        view.animate()
            .alpha(0.0f)
            .setDuration(FADE_OUT_DURATION_MILLIS)
            .withEndAction {
                Log.i(TAG, "Launcher first frame is ready; IVI boot handoff removed")
                removeImmediately()
            }
            .start()
    }

    private fun removeImmediately() {
        handler.removeCallbacks(timeout)
        handoffView?.let { view ->
            view.animate().cancel()
            runCatching { windowManager.removeViewImmediate(view) }
        }
        handoffView = null
        stateMachine.completeRemoval()
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        handoffView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        handoffView = null
        stateMachine.completeRemoval()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "MiniIviBootHandoff"
        private const val ACTION_SHOW = "com.miniivi.boot.action.SHOW_HANDOFF"
        private const val ACTION_DISMISS = "com.miniivi.boot.action.DISMISS_HANDOFF"
        private const val TYPE_BOOT_PROGRESS = 2021
        internal const val FADE_OUT_DURATION_MILLIS = 250L
        internal const val FAIL_SAFE_TIMEOUT_MILLIS = 30_000L

        fun showIntent(context: Context): Intent =
            Intent(context, BootHandoffService::class.java).setAction(ACTION_SHOW)

        fun dismissIntent(context: Context): Intent =
            Intent(context, BootHandoffService::class.java).setAction(ACTION_DISMISS)
    }
}
