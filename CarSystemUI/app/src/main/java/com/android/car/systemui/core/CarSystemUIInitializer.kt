package com.android.car.systemui.core

import android.content.res.Configuration
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class CarSystemUIInitializer @Inject constructor(
    private val startableProviders: Map<Class<*>, @JvmSuppressWildcards Provider<CarSystemUIStartable>>,
    private val systemUserPolicy: SystemUserPolicy,
) {
    private val startedComponents = CopyOnWriteArrayList<CarSystemUIStartable>()

    @Volatile
    private var componentsStarted = false

    @Synchronized
    fun startComponentsIfNeeded() {
        if (componentsStarted || !systemUserPolicy.isSystemUser()) return

        startableProviders.entries
            .sortedBy { it.key.name }
            .forEach { (_, provider) ->
                provider.get().also { component ->
                    component.start()
                    startedComponents += component
                }
            }
        componentsStarted = true
    }

    fun onConfigurationChanged(configuration: Configuration) {
        if (!componentsStarted) return
        startedComponents.forEach { it.onConfigurationChanged(configuration) }
    }
}
