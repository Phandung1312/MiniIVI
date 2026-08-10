package com.android.car.systemui.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessMapperTest {
    @Test
    fun settingAndProgressAreClamped() {
        assertEquals(0f, BrightnessMapper.settingToProgress(-10, 10, 200), 0.001f)
        assertEquals(1f, BrightnessMapper.settingToProgress(500, 10, 200), 0.001f)
        assertEquals(10, BrightnessMapper.progressToSetting(-1f, 10, 200))
        assertEquals(200, BrightnessMapper.progressToSetting(2f, 10, 200))
    }

    @Test
    fun endpointsRoundTrip() {
        val minimum = 4
        val maximum = 1023
        assertEquals(
            0f,
            BrightnessMapper.settingToProgress(
                BrightnessMapper.progressToSetting(0f, minimum, maximum),
                minimum,
                maximum,
            ),
            0.001f,
        )
        assertEquals(
            1f,
            BrightnessMapper.settingToProgress(
                BrightnessMapper.progressToSetting(1f, minimum, maximum),
                minimum,
                maximum,
            ),
            0.001f,
        )
    }

    @Test
    fun displayBrightnessRoundTripsOnThePerceptualSliderScale() {
        val progress = 0.72f
        val displayBrightness = BrightnessMapper.progressToLinear(progress, 0f, 1f)

        assertEquals(
            progress,
            BrightnessMapper.linearToProgress(displayBrightness, 0f, 1f),
            0.001f,
        )
    }
}
