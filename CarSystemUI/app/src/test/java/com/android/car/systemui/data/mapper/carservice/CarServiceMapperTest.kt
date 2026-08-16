package com.android.car.systemui.data.mapper.carservice

import com.android.car.systemui.domain.model.ClimateCapability
import com.android.car.systemui.domain.model.ClimateFanDirection
import com.android.car.systemui.domain.model.ClimateWindow
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.QuickControl
import com.android.car.systemui.domain.model.TemperatureUnit
import com.miniivi.car.api.AudioState as ApiAudioState
import com.miniivi.car.api.BrightnessState as ApiBrightnessState
import com.miniivi.car.api.ClimateCapability as ApiClimateCapability
import com.miniivi.car.api.ClimateControlState as ApiClimateControlState
import com.miniivi.car.api.ClimateFanDirection as ApiClimateFanDirection
import com.miniivi.car.api.ClimateWindow as ApiClimateWindow
import com.miniivi.car.api.ClimateZoneControlState as ApiClimateZoneControlState
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacState as ApiHvacState
import com.miniivi.car.api.HvacZone
import com.miniivi.car.api.HvacZoneState as ApiHvacZoneState
import com.miniivi.car.api.QuickControl as ApiQuickControl
import com.miniivi.car.api.QuickControlsState as ApiQuickControlsState
import com.miniivi.car.api.TemperatureUnit as ApiTemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarServiceMapperTest {
    @Test
    fun brightnessAndAudioStatesMapPlatformPayloads() {
        val brightness = ApiBrightnessState(
            status = FeatureStatus.READY,
            available = true,
            progress = 0.8f,
            automatic = true,
            diagnosticMessage = "brightness warning",
        ).toDomain()
        val audio = ApiAudioState(
            status = FeatureStatus.READY,
            available = true,
            volume = 7,
            minimum = 2,
            maximum = 12,
            diagnosticMessage = "audio warning",
        ).toDomain()

        assertEquals(0.8f, brightness.progress, 0.001f)
        assertTrue(brightness.available)
        assertTrue(brightness.automatic)
        assertEquals("brightness warning", brightness.errorMessage)
        assertEquals(7, audio.volume)
        assertEquals(2, audio.minimum)
        assertEquals(12, audio.maximum)
        assertEquals("audio warning", audio.errorMessage)
    }

    @Test
    fun hvacStateMapsZonesAndTemperaturePresence() {
        val state = ApiHvacState(
            status = FeatureStatus.READY,
            available = true,
            hasCabinTemperature = true,
            cabinTemperatureCelsius = 24.5f,
            leftZone = ApiHvacZoneState(
                zone = HvacZone.LEFT,
                available = true,
                hasTemperature = true,
                temperatureCelsius = 21f,
                minimumCelsius = 16f,
                maximumCelsius = 30f,
            ),
            rightZone = ApiHvacZoneState(
                zone = HvacZone.RIGHT,
                hasTemperature = false,
                temperatureCelsius = 23f,
            ),
        ).toDomain()

        assertEquals(24.5f, state.cabinTemperature)
        assertEquals(ClimateZone.LEFT, state.leftZone?.zone)
        assertEquals(21f, state.leftZone?.temperature)
        assertEquals(ClimateZone.RIGHT, state.rightZone?.zone)
        assertEquals(null, state.rightZone?.temperature)
        assertTrue(state.dualZone)
    }

    @Test
    fun extendedControlsMapEnumValuesAndCapabilityBitmasks() {
        val climate = ApiClimateControlState(
            status = FeatureStatus.READY,
            available = true,
            driverZone = ApiClimateZoneControlState(
                zone = HvacZone.LEFT,
                fanDirection = ApiClimateFanDirection.DEFROST,
                availableFanDirections = intArrayOf(
                    ApiClimateFanDirection.FEET,
                    99,
                ),
            ),
            temperatureUnit = 99,
            realCapabilities = ApiClimateCapability.POWER or
                ApiClimateCapability.FAN_DIRECTION,
        )
        val quickControls = ApiQuickControlsState(
            status = FeatureStatus.READY,
            available = true,
            realCapabilities = ApiQuickControl.WIFI_CAPABILITY or
                ApiQuickControl.HOTSPOT_CAPABILITY or
                ApiQuickControl.SCREEN_OFF_CAPABILITY,
        )

        val state = toDomain(climate, quickControls)

        assertEquals(ClimateFanDirection.DEFROST, state.climate.driverZone.fanDirection)
        assertEquals(
            listOf(ClimateFanDirection.FEET),
            state.climate.driverZone.availableFanDirections,
        )
        assertEquals(TemperatureUnit.FAHRENHEIT, state.climate.temperatureUnit)
        assertEquals(
            setOf(ClimateCapability.POWER, ClimateCapability.FAN_DIRECTION),
            state.climate.capabilities,
        )
        assertEquals(setOf(QuickControl.WIFI, QuickControl.HOTSPOT), state.quickControls.capabilities)
        assertTrue(state.quickControls.screenOffAvailable)
        assertFalse(QuickControl.VALET_MODE in state.quickControls.capabilities)
    }

    @Test
    fun unknownAndDuplicateFanDirectionsDoNotCreateDuplicateDomainOptions() {
        val climate = ApiClimateControlState(
            status = FeatureStatus.READY,
            available = true,
            driverZone = ApiClimateZoneControlState(
                zone = HvacZone.LEFT,
                fanDirection = ApiClimateFanDirection.FACE_AND_FEET,
                availableFanDirections = intArrayOf(
                    ApiClimateFanDirection.FACE,
                    99,
                    ApiClimateFanDirection.FACE,
                    ApiClimateFanDirection.FACE_AND_FEET,
                ),
            ),
        )

        val state = toDomain(climate, ApiQuickControlsState())

        assertEquals(ClimateFanDirection.FACE_AND_FEET, state.climate.driverZone.fanDirection)
        assertEquals(
            listOf(ClimateFanDirection.FACE, ClimateFanDirection.FACE_AND_FEET),
            state.climate.driverZone.availableFanDirections,
        )
    }

    @Test
    fun domainCommandsMapToCarServiceCodes() {
        assertEquals(HvacZone.LEFT, ClimateZone.LEFT.toApi())
        assertEquals(ApiClimateFanDirection.FACE_AND_FEET, ClimateFanDirection.FACE_AND_FEET.toApi())
        assertEquals(ApiClimateWindow.REAR, ClimateWindow.REAR.toApi())
        assertEquals(ApiTemperatureUnit.CELSIUS, TemperatureUnit.CELSIUS.toApi())
        assertEquals(ApiQuickControl.BLUETOOTH, QuickControl.BLUETOOTH.toApi())
    }
}
