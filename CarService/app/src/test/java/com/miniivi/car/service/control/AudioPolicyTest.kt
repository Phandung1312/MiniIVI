package com.miniivi.car.service.control

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPolicyTest {
    @Test
    fun clampKeepsVolumeInsideGroupRange() {
        assertEquals(2, AudioPolicy.clamp(0, 2, 10))
        assertEquals(6, AudioPolicy.clamp(6, 2, 10))
        assertEquals(10, AudioPolicy.clamp(20, 2, 10))
    }
}
