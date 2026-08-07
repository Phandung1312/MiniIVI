package com.android.car.systemui.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessMapperTest {
    @Test
    fun settingAndProgressAreClamped() {
        assertEquals(0f, BrightnessMapper.toProgress(-10), 0.001f)
        assertEquals(1f, BrightnessMapper.toProgress(500), 0.001f)
        assertEquals(BrightnessMapper.MINIMUM, BrightnessMapper.toSetting(-1f))
        assertEquals(BrightnessMapper.MAXIMUM, BrightnessMapper.toSetting(2f))
    }

    @Test
    fun endpointsRoundTrip() {
        assertEquals(0f, BrightnessMapper.toProgress(BrightnessMapper.toSetting(0f)), 0.001f)
        assertEquals(1f, BrightnessMapper.toProgress(BrightnessMapper.toSetting(1f)), 0.001f)
    }
}
