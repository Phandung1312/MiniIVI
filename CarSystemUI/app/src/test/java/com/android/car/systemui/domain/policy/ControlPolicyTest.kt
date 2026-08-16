package com.android.car.systemui.domain.policy

import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.TemperatureZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlPolicyTest {
    @Test
    fun audioProgressMapsToDiscreteAndClampedVolume() {
        assertEquals(0, AudioVolumePolicy.toVolume(-1f, 0, 10))
        assertEquals(7, AudioVolumePolicy.toVolume(0.7f, 0, 10))
        assertEquals(10, AudioVolumePolicy.toVolume(2f, 0, 10))
    }

    @Test
    fun hvacAdjustmentHonorsPropertyLimits() {
        val low = TemperatureZone(ClimateZone.LEFT, 16f, 16f, 30f)
        val high = TemperatureZone(ClimateZone.LEFT, 30f, 16f, 30f)
        val normal = TemperatureZone(ClimateZone.LEFT, 22f, 16f, 30f)
        assertEquals(16f, HvacTemperaturePolicy.adjust(low, -0.5f), 0.001f)
        assertEquals(30f, HvacTemperaturePolicy.adjust(high, 0.5f), 0.001f)
        assertEquals(22.5f, HvacTemperaturePolicy.adjust(normal, 0.5f), 0.001f)
    }
}
