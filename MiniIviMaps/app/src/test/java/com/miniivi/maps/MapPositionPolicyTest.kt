package com.miniivi.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPositionPolicyTest {
    @Test
    fun newestPositionWins() {
        val persisted = MapPosition(10.0, 106.0, 100L)
        val system = MapPosition(11.0, 107.0, 200L)

        assertEquals(system, MapPositionPolicy.selectNewest(persisted, system))
        assertEquals(persisted, MapPositionPolicy.selectNewest(persisted, null))
        assertNull(MapPositionPolicy.selectNewest(null, null))
    }

    @Test
    fun coordinatesMustBeFiniteAndInsideWorldBounds() {
        assertTrue(MapPositionPolicy.isValid(MapPosition(10.0, 106.0, 1L)))
        assertFalse(MapPositionPolicy.isValid(MapPosition(91.0, 106.0, 1L)))
        assertFalse(MapPositionPolicy.isValid(MapPosition(10.0, 181.0, 1L)))
        assertFalse(MapPositionPolicy.isValid(MapPosition(Double.NaN, 106.0, 1L)))
    }
}
