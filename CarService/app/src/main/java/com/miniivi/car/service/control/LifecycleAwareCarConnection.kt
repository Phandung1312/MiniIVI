package com.miniivi.car.service.control

import android.car.Car
import android.content.Context
import android.os.Handler
import android.util.Log

/**
 * Creates an android.car client that survives car_service restarts.
 *
 * The legacy Car.createCar(Context) overload intentionally kills its client process when
 * car_service dies. Keeping the lifecycle listener is therefore mandatory for a persistent
 * system bridge such as MiniIVI CarService.
 */
internal class LifecycleAwareCarConnection(
    private val context: Context,
    private val onReady: (Car) -> Unit,
    private val onUnavailable: () -> Unit,
) {
    private var lifecycleListener: Car.CarServiceLifecycleListener? = null

    @Volatile
    var car: Car? = null
        private set

    fun connect() {
        if (car != null) {
            logDebug("event=connect_skipped reason=already_connected")
            return
        }

        Log.i(TAG, "event=connection_requested backend=aaos")

        val listener = object : Car.CarServiceLifecycleListener {
            override fun onLifecycleChanged(lifecycleCar: Car, ready: Boolean) {
                car = lifecycleCar
                if (ready) {
                    Log.i(TAG, "event=connection_ready backend=aaos")
                    onReady(lifecycleCar)
                } else {
                    Log.w(TAG, "event=connection_unavailable backend=aaos")
                    onUnavailable()
                }
            }
        }
        lifecycleListener = listener

        car = Car.createCar(
            context,
            null as Handler?,
            Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT,
            listener,
        )
        logDebug("event=client_created backend=aaos wait_millis=$DO_NOT_WAIT_MILLIS")
    }

    fun disconnect() {
        val currentCar = car
        car = null
        lifecycleListener = null
        if (currentCar == null) return
        runCatching { currentCar.disconnect() }
            .onSuccess { Log.i(TAG, "event=connection_closed backend=aaos") }
            .onFailure { error ->
                Log.w(TAG, "event=connection_close_failed backend=aaos", error)
            }
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val DO_NOT_WAIT_MILLIS = Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT
        const val TAG = "MiniIviCarConnection"
    }
}
