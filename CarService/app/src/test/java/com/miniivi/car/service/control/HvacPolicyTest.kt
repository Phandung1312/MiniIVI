package com.miniivi.car.service.control

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HvacPolicyTest {
    @Test
    fun oneAreaMapsToLeftZoneOnly() {
        val (left, right) = HvacPolicy.selectLogicalAreas(intArrayOf(7))
        assertEquals(7, left)
        assertNull(right)
    }

    @Test
    fun multipleAreasMapFirstAndLast() {
        val (left, right) = HvacPolicy.selectLogicalAreas(intArrayOf(1, 2, 4))
        assertEquals(1, left)
        assertEquals(4, right)
    }

    @Test
    fun requestedTemperatureIsClampedToCapabilityRange() {
        assertEquals(16f, HvacPolicy.clampTemperature(10f, 16f, 30f))
        assertEquals(23f, HvacPolicy.clampTemperature(23f, 16f, 30f))
        assertEquals(30f, HvacPolicy.clampTemperature(40f, 16f, 30f))
    }
}
