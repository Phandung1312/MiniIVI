package com.android.car.systemui.data.repository

import com.android.car.systemui.data.model.TemperatureZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlPolicyTest {
    @Test
    fun audioProgressMapsToDiscreteAndClampedVolume() {
        assertEquals(0, AudioLevelMapper.toVolume(-1f, 0, 10))
        assertEquals(7, AudioLevelMapper.toVolume(0.7f, 0, 10))
        assertEquals(10, AudioLevelMapper.toVolume(2f, 0, 10))
    }

    @Test
    fun hvacAdjustmentHonorsPropertyLimits() {
        val low = TemperatureZone(1, 16f, 16f, 30f)
        val high = TemperatureZone(1, 30f, 16f, 30f)
        val normal = TemperatureZone(1, 22f, 16f, 30f)
        assertEquals(16f, HvacTemperaturePolicy.adjust(low, -0.5f), 0.001f)
        assertEquals(30f, HvacTemperaturePolicy.adjust(high, 0.5f), 0.001f)
        assertEquals(22.5f, HvacTemperaturePolicy.adjust(normal, 0.5f), 0.001f)
    }
}
