package com.android.car.launcher.core.lifecycle

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

abstract class LifeCycleLogger : ComponentActivity() {
    private val componentName: String
        get() = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "event=activity_created component=$componentName")
    }

    override fun onStart() {
        super.onStart()
        logDebug("event=activity_started component=$componentName")
    }

    override fun onResume() {
        super.onResume()
        logDebug("event=activity_resumed component=$componentName")
    }

    override fun onPause() {
        logDebug("event=activity_paused component=$componentName")
        super.onPause()
    }

    override fun onStop() {
        logDebug("event=activity_stopped component=$componentName")
        super.onStop()
    }

    override fun onDestroy() {
        Log.i(TAG, "event=activity_destroyed component=$componentName")
        super.onDestroy()
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "MiniIviLauncherLife"
    }
}
