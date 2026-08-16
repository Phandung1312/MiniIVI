package com.android.car.systemui.core

import android.content.res.Configuration
import com.android.car.systemui.domain.policy.SystemUserPolicy
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Provider
import org.junit.Assert.assertEquals
import org.junit.Test

class CarSystemUIInitializerTest {
    @Test
    fun startsProvidersOnceInStableOrder() {
        val events = mutableListOf<String>()
        val first = RecordingStartable("first", events)
        val second = RecordingStartable("second", events)
        val providers: Map<Class<*>, Provider<CarSystemUIStartable>> = mapOf(
            SecondStartable::class.java to providerOf(second),
            FirstStartable::class.java to providerOf(first),
        )
        val initializer = CarSystemUIInitializer(providers, FakeSystemUserPolicy(isSystemUser = true))

        initializer.startComponentsIfNeeded()
        initializer.startComponentsIfNeeded()

        assertEquals(listOf("first", "second"), events)
        assertEquals(1, first.startCount.get())
        assertEquals(1, second.startCount.get())
    }

    @Test
    fun doesNotStartForSecondaryUser() {
        val startable = RecordingStartable("secondary", mutableListOf())
        val providers: Map<Class<*>, Provider<CarSystemUIStartable>> = mapOf(
            FirstStartable::class.java to providerOf(startable),
        )
        val initializer = CarSystemUIInitializer(providers, FakeSystemUserPolicy(isSystemUser = false))

        initializer.startComponentsIfNeeded()

        assertEquals(0, startable.startCount.get())
    }

    @Test
    fun forwardsConfigurationChangesToStartedComponents() {
        val events = mutableListOf<String>()
        val startable = RecordingStartable("started", events)
        val initializer = CarSystemUIInitializer(
            mapOf(FirstStartable::class.java to providerOf(startable)),
            FakeSystemUserPolicy(isSystemUser = true),
        )

        initializer.startComponentsIfNeeded()
        initializer.onConfigurationChanged(Configuration())

        assertEquals(1, startable.configurationChangeCount.get())
    }

    private fun providerOf(startable: CarSystemUIStartable) = object : Provider<CarSystemUIStartable> {
        override fun get(): CarSystemUIStartable = startable
    }
}

private class FakeSystemUserPolicy(
    private val isSystemUser: Boolean,
) : SystemUserPolicy {
    override fun isSystemUser(): Boolean = isSystemUser
}

private class RecordingStartable(
    private val name: String,
    private val events: MutableList<String>,
) : CarSystemUIStartable {
    val startCount = AtomicInteger()
    val configurationChangeCount = AtomicInteger()

    override fun start() {
        startCount.incrementAndGet()
        events += name
    }

    override fun onConfigurationChanged(configuration: Configuration) {
        configurationChangeCount.incrementAndGet()
    }
}

private class FirstStartable
private class SecondStartable
