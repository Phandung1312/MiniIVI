package com.miniivi.car.service.control

import com.miniivi.car.api.BluetoothDeviceInfo
import com.miniivi.car.api.BluetoothFeatureState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothStateReducerTest {
    private val device = BluetoothDeviceInfo("00:11:22:33:44:55", "Phone")

    @Test
    fun adapterOffClearsTransientDeviceState() {
        val state = BluetoothFeatureState(
            enabled = true,
            discovering = true,
            nearbyDevices = listOf(device),
            connectedDevices = listOf(device),
        )

        val updated = BluetoothStateReducer.adapterChanged(state, enabled = false)

        assertFalse(updated.enabled)
        assertFalse(updated.discovering)
        assertTrue(updated.nearbyDevices.isEmpty())
        assertTrue(updated.connectedDevices.isEmpty())
    }

    @Test
    fun discoveryStartClearsOldNearbyDevices() {
        val updated = BluetoothStateReducer.discoveryStarted(
            BluetoothFeatureState(nearbyDevices = listOf(device)),
        )

        assertTrue(updated.discovering)
        assertTrue(updated.nearbyDevices.isEmpty())
    }

    @Test
    fun nearbyAndConnectedDevicesAreDeduplicatedByAddress() {
        val renamed = device.copy(name = "Renamed")
        val nearby = BluetoothStateReducer.nearbyDevice(
            BluetoothStateReducer.nearbyDevice(BluetoothFeatureState(), device),
            renamed,
        )
        val connected = BluetoothStateReducer.connectedDevice(
            BluetoothStateReducer.connectedDevice(BluetoothFeatureState(), device),
            renamed,
        )

        assertEquals(1, nearby.nearbyDevices.size)
        assertEquals(1, connected.connectedDevices.size)
    }

    @Test
    fun nameChangeUpdatesEveryKnownList() {
        val state = BluetoothFeatureState(
            pairedDevices = listOf(device),
            nearbyDevices = listOf(device),
            connectedDevices = listOf(device),
        )

        val updated = BluetoothStateReducer.renamedDevice(state, device.copy(name = "Renamed"))

        assertEquals("Renamed", updated.pairedDevices.single().name)
        assertEquals("Renamed", updated.nearbyDevices.single().name)
        assertEquals("Renamed", updated.connectedDevices.single().name)
    }

    @Test
    fun disconnectRemovesOnlyMatchingAddress() {
        val other = BluetoothDeviceInfo("AA:BB:CC:DD:EE:FF", "Other")
        val updated = BluetoothStateReducer.disconnectedDevice(
            BluetoothFeatureState(connectedDevices = listOf(device, other)),
            device.address,
        )

        assertEquals(listOf(other), updated.connectedDevices)
    }
}
