package com.android.car.launcher.core.lifecycle

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

abstract class LifeCycleLogger : ComponentActivity() {
    private val logTag: String
        get() = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(logTag, "onCreate")
    }

    override fun onStart() {
        super.onStart()
        Log.d(logTag, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(logTag, "onResume")
    }

    override fun onPause() {
        Log.d(logTag, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(logTag, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(logTag, "onDestroy")
        super.onDestroy()
    }
}
