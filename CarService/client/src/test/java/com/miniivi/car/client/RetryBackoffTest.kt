package com.miniivi.car.client

import org.junit.Assert.assertEquals
import org.junit.Test

class RetryBackoffTest {
    @Test
    fun delayDoublesUntilMaximumAndCanReset() {
        val backoff = RetryBackoff(initialDelayMillis = 1_000, maximumDelayMillis = 30_000)

        assertEquals(1_000L, backoff.nextDelay())
        assertEquals(2_000L, backoff.nextDelay())
        assertEquals(4_000L, backoff.nextDelay())
        assertEquals(8_000L, backoff.nextDelay())
        assertEquals(16_000L, backoff.nextDelay())
        assertEquals(30_000L, backoff.nextDelay())
        assertEquals(30_000L, backoff.nextDelay())

        backoff.reset()
        assertEquals(1_000L, backoff.nextDelay())
    }
}
