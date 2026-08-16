package com.android.car.launcher.feature.dashboard.data.mapper

import com.android.car.launcher.feature.dashboard.domain.model.FeatureStatus
import com.miniivi.car.api.FeatureStatus as CarFeatureStatus
import com.miniivi.car.api.HvacState as CarHvacState
import com.miniivi.car.api.VehicleStatusState as CarVehicleStatusState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarStateMappersTest {
    @Test
    fun mapsCarServiceStatesToPureDomainModels() {
        val hvac = CarHvacState(
            status = CarFeatureStatus.READY,
            available = true,
            hasCabinTemperature = true,
            cabinTemperatureCelsius = 24f,
            acAvailable = true,
            acOn = true,
        ).toDomain()
        val vehicle = CarVehicleStatusState(
            status = CarFeatureStatus.READY,
            available = true,
            hasBatteryPercentage = true,
            batteryPercentage = 70f,
        ).toDomain()

        assertEquals(FeatureStatus.READY, hvac.status)
        assertEquals(24f, hvac.cabinTemperatureCelsius)
        assertTrue(hvac.acOn)
        assertEquals(FeatureStatus.READY, vehicle.status)
        assertEquals(70f, vehicle.batteryPercentage)
    }
}
