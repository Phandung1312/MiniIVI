package com.miniivi.car.service.control

import org.junit.Assert.assertEquals
import org.junit.Test

class BrightnessPolicyTest {
    @Test
    fun progressRoundTripPreservesRepresentativeValues() {
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { progress ->
            val linear = BrightnessPolicy.progressToLinear(progress, 0f, 1f)
            assertEquals(progress, BrightnessPolicy.linearToProgress(linear, 0f, 1f), 0.001f)
        }
    }

    @Test
    fun settingConversionClampsOutOfRangeProgress() {
        assertEquals(1, BrightnessPolicy.progressToSetting(-1f, 1, 255))
        assertEquals(255, BrightnessPolicy.progressToSetting(2f, 1, 255))
    }
}
