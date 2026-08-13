package com.miniivi.maps

import android.Manifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLocationPermissionTest {
    @Test
    fun eitherFineOrCoarseLocationIsAccepted() {
        assertTrue(
            MapLocationPermission.isGranted(
                mapOf(Manifest.permission.ACCESS_FINE_LOCATION to true),
            ),
        )
        assertTrue(
            MapLocationPermission.isGranted(
                mapOf(Manifest.permission.ACCESS_COARSE_LOCATION to true),
            ),
        )
    }

    @Test
    fun deniedOrMissingLocationPermissionIsRejected() {
        assertFalse(
            MapLocationPermission.isGranted(
                mapOf(
                    Manifest.permission.ACCESS_FINE_LOCATION to false,
                    Manifest.permission.ACCESS_COARSE_LOCATION to false,
                ),
            ),
        )
        assertFalse(MapLocationPermission.isGranted(emptyMap()))
    }
}
