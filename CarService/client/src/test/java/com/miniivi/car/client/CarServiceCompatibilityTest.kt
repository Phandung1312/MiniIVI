package com.miniivi.car.client

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarServiceCompatibilityTest {
    @Test
    fun versionThreeDoesNotUseVersionFourTransactions() {
        assertFalse(CarServiceCompatibility.supportsRefresh(3))
        assertFalse(CarServiceCompatibility.supportsBluetooth(3))
    }

    @Test
    fun versionFourEnablesRefreshAndBluetoothTransactions() {
        assertTrue(CarServiceCompatibility.supportsRefresh(4))
        assertTrue(CarServiceCompatibility.supportsBluetooth(4))
    }
}
