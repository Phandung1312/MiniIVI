package com.miniivi.car.service.control

import android.content.Context
import android.os.Handler
import android.util.Log
import java.lang.reflect.Proxy

/**
 * Creates an android.car client that survives car_service restarts.
 *
 * The legacy Car.createCar(Context) overload intentionally kills its client process when
 * car_service dies. Keeping the lifecycle listener is therefore mandatory for a persistent
 * system bridge such as MiniIVI CarService.
 */
internal class LifecycleAwareCarConnection(
    private val context: Context,
    private val onReady: (Any) -> Unit,
    private val onUnavailable: () -> Unit,
) {
    private var lifecycleListener: Any? = null

    @Volatile
    var car: Any? = null
        private set

    fun connect() {
        if (car != null) {
            logDebug("event=connect_skipped reason=already_connected")
            return
        }

        Log.i(TAG, "event=connection_requested backend=aaos")

        val carClass = Class.forName(CAR_CLASS)
        val listenerClass = Class.forName(CAR_LIFECYCLE_LISTENER_CLASS)
        val listener = Proxy.newProxyInstance(
            listenerClass.classLoader,
            arrayOf(listenerClass),
        ) { proxy, method, args ->
            when (method.name) {
                "onLifecycleChanged" -> {
                    val lifecycleCar = requireNotNull(args?.getOrNull(0))
                    val ready = args.getOrNull(1) as? Boolean ?: false
                    car = lifecycleCar
                    if (ready) {
                        Log.i(TAG, "event=connection_ready backend=aaos")
                        onReady(lifecycleCar)
                    } else {
                        Log.w(TAG, "event=connection_unavailable backend=aaos")
                        onUnavailable()
                    }
                    null
                }
                "toString" -> "MiniIVI car service lifecycle listener"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        }
        lifecycleListener = listener

        car = carClass.getMethod(
            "createCar",
            Context::class.java,
            Handler::class.java,
            Long::class.javaPrimitiveType,
            listenerClass,
        ).invoke(
            null,
            context,
            null,
            DO_NOT_WAIT_MILLIS,
            listener,
        ) ?: error("Unable to create the AAOS car client")
        logDebug("event=client_created backend=aaos wait_millis=$DO_NOT_WAIT_MILLIS")
    }

    fun disconnect() {
        val currentCar = car
        car = null
        lifecycleListener = null
        if (currentCar == null) return
        runCatching { currentCar.javaClass.getMethod("disconnect").invoke(currentCar) }
            .onSuccess { Log.i(TAG, "event=connection_closed backend=aaos") }
            .onFailure { error ->
                Log.w(TAG, "event=connection_close_failed backend=aaos", error)
            }
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val CAR_CLASS = "android.car.Car"
        const val CAR_LIFECYCLE_LISTENER_CLASS =
            "android.car.Car\$CarServiceLifecycleListener"
        const val DO_NOT_WAIT_MILLIS = 0L
        const val TAG = "MiniIviCarConnection"
    }
}
