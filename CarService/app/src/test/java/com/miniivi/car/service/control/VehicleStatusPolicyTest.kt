package com.miniivi.car.service.control

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleStatusPolicyTest {
    @Test
    fun batteryPercentageUsesCapacityAndClampsToDisplayRange() {
        assertEquals(50f, VehicleStatusPolicy.batteryPercentage(50f, 100f)!!, 0.001f)
        assertEquals(100f, VehicleStatusPolicy.batteryPercentage(120f, 100f)!!, 0.001f)
        assertEquals(0f, VehicleStatusPolicy.batteryPercentage(-1f, 100f)!!, 0.001f)
    }

    @Test
    fun batteryPercentageRejectsMissingCapacity() {
        assertNull(VehicleStatusPolicy.batteryPercentage(50f, 0f))
        assertNull(VehicleStatusPolicy.batteryPercentage(Float.NaN, 100f))
    }

    @Test
    fun minimumTirePressureIgnoresInvalidValues() {
        assertEquals(200f, VehicleStatusPolicy.minimumTirePressure(listOf(220f, 200f, Float.NaN))!!, 0.001f)
        assertNull(VehicleStatusPolicy.minimumTirePressure(listOf(Float.NaN, -1f)))
    }

    @Test
    fun fallbackValuesAreStableForUnsupportedProperties() {
        assertEquals(78f, VehicleStatusPolicy.MOCK_BATTERY_PERCENTAGE, 0.001f)
        assertEquals(30f, VehicleStatusPolicy.MOCK_OUTSIDE_TEMPERATURE_CELSIUS, 0.001f)
        assertEquals(320f, VehicleStatusPolicy.MOCK_RANGE_KILOMETERS, 0.001f)
        assertEquals(230f, VehicleStatusPolicy.MOCK_TIRE_PRESSURE_KPA, 0.001f)
    }
}
