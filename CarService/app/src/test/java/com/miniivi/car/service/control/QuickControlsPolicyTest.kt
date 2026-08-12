package com.miniivi.car.service.control

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QuickControlsPolicyTest {
    @Test
    fun realBackendAlwaysTakesPrecedence() {
        assertEquals(QuickControlBackend.REAL, QuickControlsPolicy.backend(realAvailable = true))
        assertEquals(QuickControlBackend.MOCK, QuickControlsPolicy.backend(realAvailable = false))
    }

    @Test
    fun realBackendFailureNeverFallsBackToMock() {
        assertFalse(QuickControlsPolicy.shouldUseMockAfterRealFailure(realAvailable = true))
        assertEquals(true, QuickControlsPolicy.shouldUseMockAfterRealFailure(realAvailable = false))
    }
}
