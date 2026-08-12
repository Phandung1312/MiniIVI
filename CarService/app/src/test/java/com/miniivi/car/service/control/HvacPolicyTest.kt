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

    @Test
    fun driverSeatAndOppositeFrontSeatArePreferred() {
        val (driver, passenger) = HvacPolicy.selectFrontAreas(intArrayOf(1, 4, 16, 64), 4)
        assertEquals(4, driver)
        assertEquals(1, passenger)
    }

    @Test
    fun missingDriverSeatFallsBackToAvailableFrontAreas() {
        val (driver, passenger) = HvacPolicy.selectFrontAreas(intArrayOf(7, 9), 4)
        assertEquals(7, driver)
        assertEquals(9, passenger)
    }

    @Test
    fun syncCopiesDriverTemperatureToPassenger() {
        assertEquals(22f, HvacPolicy.syncedPassengerTemperature(true, 22f, 24f))
        assertEquals(24f, HvacPolicy.syncedPassengerTemperature(false, 22f, 24f))
    }

    @Test
    fun onlyAdvertisedFanDirectionsAreAccepted() {
        val available = intArrayOf(1, 3)
        assertEquals(true, HvacPolicy.isFanDirectionAvailable(3, available))
        assertEquals(false, HvacPolicy.isFanDirectionAvailable(2, available))
    }
}
