package com.android.car.launcher.feature.dashboard.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.miniivi.navigation.contract.INavigationStateService
import com.miniivi.navigation.contract.NavigationContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNavigationStateConnection @Inject constructor(
    @ApplicationContext context: Context,
) : NavigationStateConnection {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(context.mainLooper)
    private var listener: NavigationStateConnection.Listener? = null
    private var started = false
    private var binding = false
    private var bound = false
    private var retryAttempt = 0

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            binding = false
            bound = true
            retryAttempt = 0
            Log.i(TAG, "event=service_connected component=${name.flattenToShortString()}")
            if (started) {
                val service = INavigationStateService.Stub.asInterface(binder)
                listener?.onConnected(object : NavigationStateEndpoint {
                    override fun reportDestination(destination: Int): Boolean = runCatching {
                        service.reportDestination(destination)
                    }.isSuccess
                })
            } else {
                unbindSafely()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            handleDisconnect("service_disconnected")
        }

        override fun onBindingDied(name: ComponentName) {
            handleDisconnect("binding_died")
        }

        override fun onNullBinding(name: ComponentName) {
            handleDisconnect("null_binding")
        }
    }

    override fun start(listener: NavigationStateConnection.Listener) {
        if (started) return
        started = true
        this.listener = listener
        bindNow()
    }

    override fun stop() {
        started = false
        listener = null
        mainHandler.removeCallbacksAndMessages(RETRY_TOKEN)
        unbindSafely()
    }

    private fun bindNow() {
        if (!started || binding || bound) return
        binding = true
        val intent = Intent().setComponent(
            ComponentName(
                NavigationContract.SYSTEM_UI_PACKAGE,
                NavigationContract.NAVIGATION_STATE_SERVICE_CLASS,
            ),
        )
        val accepted = runCatching {
            applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.onFailure { error ->
            Log.e(TAG, "event=bind_failed service=navigation_state", error)
        }.getOrDefault(false)
        if (!accepted) {
            binding = false
            Log.w(TAG, "event=bind_rejected service=navigation_state")
            scheduleRetry()
        }
    }

    private fun handleDisconnect(reason: String) {
        Log.w(TAG, "event=connection_reset service=navigation_state reason=$reason")
        unbindSafely()
        listener?.onDisconnected()
        scheduleRetry()
    }

    private fun scheduleRetry() {
        if (!started) return
        retryAttempt = (retryAttempt + 1).coerceAtMost(MAX_RETRY_ATTEMPT)
        val delay = (
            INITIAL_RETRY_MILLIS * (1L shl (retryAttempt - 1))
            ).coerceAtMost(MAX_RETRY_MILLIS)
        mainHandler.removeCallbacksAndMessages(RETRY_TOKEN)
        Log.w(TAG, "event=bind_retry_scheduled service=navigation_state delay_ms=$delay")
        mainHandler.postAtTime({ bindNow() }, RETRY_TOKEN, SystemClock.uptimeMillis() + delay)
    }

    private fun unbindSafely() {
        if (bound || binding) {
            runCatching { applicationContext.unbindService(connection) }
                .onFailure { error ->
                    Log.w(TAG, "event=unbind_failed service=navigation_state", error)
                }
        }
        bound = false
        binding = false
    }

    private companion object {
        const val TAG = "MiniIviNavigationClient"
        const val INITIAL_RETRY_MILLIS = 1_000L
        const val MAX_RETRY_MILLIS = 30_000L
        const val MAX_RETRY_ATTEMPT = 6
        val RETRY_TOKEN = Any()
    }
}
