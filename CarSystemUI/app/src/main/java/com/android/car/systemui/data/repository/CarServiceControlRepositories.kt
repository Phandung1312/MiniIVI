package com.android.car.systemui.data.repository

import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.data.model.ExtendedControlsState
import com.android.car.systemui.data.model.TemperatureZone
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.HvacZone
import com.miniivi.car.api.HvacZoneState
import com.miniivi.car.client.MiniIviCarClient
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal object AudioLevelMapper {
    fun toVolume(progress: Float, minimum: Int, maximum: Int): Int =
        (minimum + progress.coerceIn(0f, 1f) * (maximum - minimum))
            .roundToInt()
            .coerceIn(minimum, maximum)
}

class CarServiceExtendedControlsRepository(
    private val client: MiniIviCarClient,
    scope: CoroutineScope,
) : ExtendedControlsRepository {
    override val state: StateFlow<ExtendedControlsState> = combine(
        client.climateControlState,
        client.quickControlsState,
        ::ExtendedControlsState,
    ).stateIn(scope, SharingStarted.Eagerly, ExtendedControlsState())

    override fun start() = client.start()
    override fun refresh() = client.start()
    override fun setPower(enabled: Boolean) { client.setClimatePowerEnabled(enabled) }
    override fun setAuto(enabled: Boolean) { client.setClimateAutoEnabled(enabled) }
    override fun setSync(enabled: Boolean) { client.setClimateSyncEnabled(enabled) }
    override fun setRecirculation(enabled: Boolean) { client.setClimateRecirculationEnabled(enabled) }
    override fun setFanSpeed(zone: ClimateZone, speed: Int) {
        client.setClimateFanSpeed(zone.apiZone, speed)
    }
    override fun setFanDirection(zone: ClimateZone, direction: Int) {
        client.setClimateFanDirection(zone.apiZone, direction)
    }
    override fun setDefroster(window: Int, enabled: Boolean) {
        client.setClimateDefrosterEnabled(window, enabled)
    }
    override fun setSeatHeating(zone: ClimateZone, level: Int) {
        client.setSeatHeatingLevel(zone.apiZone, level)
    }
    override fun setSeatVentilation(zone: ClimateZone, level: Int) {
        client.setSeatVentilationLevel(zone.apiZone, level)
    }
    override fun setMaxAc(enabled: Boolean) { client.setMaxAcEnabled(enabled) }
    override fun setMaxDefrost(enabled: Boolean) { client.setMaxDefrostEnabled(enabled) }
    override fun setAutoRecirculation(enabled: Boolean) {
        client.setAutoRecirculationEnabled(enabled)
    }
    override fun setSteeringWheelHeat(level: Int) { client.setSteeringWheelHeatLevel(level) }
    override fun setTemperatureUnit(unit: Int) { client.setTemperatureUnit(unit) }
    override fun setQuickControl(control: Int, enabled: Boolean) {
        client.setQuickControlEnabled(control, enabled)
    }
    override fun requestScreenOff() { client.requestScreenOff() }

    private val ClimateZone.apiZone: Int
        get() = if (this == ClimateZone.LEFT) HvacZone.LEFT else HvacZone.RIGHT
}

internal object HvacTemperaturePolicy {
    fun adjust(zone: TemperatureZone, delta: Float): Float =
        ((zone.temperature ?: return zone.minimum) + delta).coerceIn(zone.minimum, zone.maximum)
}

class CarServiceBrightnessRepository(
    private val client: MiniIviCarClient,
    scope: CoroutineScope,
) : BrightnessRepository {
    override val state: StateFlow<BrightnessState> = client.brightnessState
        .map {
            BrightnessState(
                progress = it.progress,
                available = it.available,
                automatic = it.automatic,
                errorMessage = it.diagnosticMessage,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, BrightnessState())

    override fun start() = client.start()
    override fun stop() = Unit
    override fun refresh() = client.start()

    override suspend fun setBrightness(progress: Float) {
        client.setBrightness(progress)
    }
}

class CarServiceAudioRepository(
    private val client: MiniIviCarClient,
    scope: CoroutineScope,
) : AudioRepository {
    override val state: StateFlow<AudioState> = client.audioState
        .map {
            AudioState(
                volume = it.volume,
                minimum = it.minimum,
                maximum = it.maximum,
                available = it.available,
                errorMessage = it.diagnosticMessage,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AudioState())

    override fun start() = client.start()
    override fun stop() = Unit
    override fun refresh() = client.start()
    override fun setVolume(volume: Int) {
        client.setMediaVolume(volume)
    }
}

class CarServiceHvacRepository(
    private val client: MiniIviCarClient,
    scope: CoroutineScope,
) : HvacRepository {
    override val state: StateFlow<HvacState> = client.hvacState
        .map {
            HvacState(
                connecting = it.status == FeatureStatus.CONNECTING,
                available = it.available,
                cabinTemperature = it.cabinTemperatureCelsius.takeIf { _ ->
                    it.hasCabinTemperature
                },
                leftZone = it.leftZone?.toSystemUiZone(),
                rightZone = it.rightZone?.toSystemUiZone(),
                acAvailable = it.acAvailable,
                acOn = it.acOn,
                errorMessage = it.diagnosticMessage,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, HvacState())

    override fun start() = client.start()
    override fun stop() = Unit
    override fun refresh() = client.start()

    override fun adjustTemperature(zone: ClimateZone, delta: Float) {
        val current = state.value
        val target = when (zone) {
            ClimateZone.LEFT -> current.leftZone
            ClimateZone.RIGHT -> current.rightZone
        } ?: return
        if (target.temperature == null) return
        val logicalZone = when (zone) {
            ClimateZone.LEFT -> HvacZone.LEFT
            ClimateZone.RIGHT -> HvacZone.RIGHT
        }
        client.setHvacTemperature(logicalZone, HvacTemperaturePolicy.adjust(target, delta))
    }

    override fun setAc(enabled: Boolean) {
        client.setAcEnabled(enabled)
    }

    private fun HvacZoneState.toSystemUiZone(): TemperatureZone = TemperatureZone(
        areaId = zone,
        temperature = temperatureCelsius.takeIf { hasTemperature },
        minimum = minimumCelsius,
        maximum = maximumCelsius,
    )
}
