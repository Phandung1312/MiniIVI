package com.android.car.systemui.data.repository.carservice

import com.android.car.systemui.data.mapper.carservice.toApi
import com.android.car.systemui.data.mapper.carservice.toDomain
import com.android.car.systemui.domain.model.ClimateFanDirection
import com.android.car.systemui.domain.model.ClimateWindow
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.ExtendedControlsState
import com.android.car.systemui.domain.model.QuickControl
import com.android.car.systemui.domain.model.TemperatureUnit
import com.android.car.systemui.domain.repository.ExtendedControlsRepository
import com.android.car.systemui.di.ApplicationScope
import com.miniivi.car.client.MiniIviCarClient
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class CarServiceExtendedControlsRepository @Inject constructor(
    private val client: MiniIviCarClient,
    @ApplicationScope
    scope: CoroutineScope,
) : ExtendedControlsRepository {
    override val state: StateFlow<ExtendedControlsState> = combine(
        client.climateControlState,
        client.quickControlsState,
    ) { climate, quickControls ->
        toDomain(climate, quickControls)
    }.stateIn(scope, SharingStarted.Eagerly, ExtendedControlsState())

    override fun refresh() { client.refreshQuickControls() }
    override fun setPower(enabled: Boolean) { client.setClimatePowerEnabled(enabled) }
    override fun setAuto(enabled: Boolean) { client.setClimateAutoEnabled(enabled) }
    override fun setSync(enabled: Boolean) { client.setClimateSyncEnabled(enabled) }
    override fun setRecirculation(enabled: Boolean) {
        client.setClimateRecirculationEnabled(enabled)
    }
    override fun setFanSpeed(zone: ClimateZone, speed: Int) {
        client.setClimateFanSpeed(zone.toApi(), speed)
    }
    override fun setFanDirection(zone: ClimateZone, direction: ClimateFanDirection) {
        client.setClimateFanDirection(zone.toApi(), direction.toApi())
    }
    override fun setDefroster(window: ClimateWindow, enabled: Boolean) {
        client.setClimateDefrosterEnabled(window.toApi(), enabled)
    }
    override fun setSeatHeating(zone: ClimateZone, level: Int) {
        client.setSeatHeatingLevel(zone.toApi(), level)
    }
    override fun setSeatVentilation(zone: ClimateZone, level: Int) {
        client.setSeatVentilationLevel(zone.toApi(), level)
    }
    override fun setMaxAc(enabled: Boolean) { client.setMaxAcEnabled(enabled) }
    override fun setMaxDefrost(enabled: Boolean) { client.setMaxDefrostEnabled(enabled) }
    override fun setAutoRecirculation(enabled: Boolean) {
        client.setAutoRecirculationEnabled(enabled)
    }
    override fun setSteeringWheelHeat(level: Int) { client.setSteeringWheelHeatLevel(level) }
    override fun setTemperatureUnit(unit: TemperatureUnit) {
        client.setTemperatureUnit(unit.toApi())
    }
    override fun setQuickControl(control: QuickControl, enabled: Boolean) {
        client.setQuickControlEnabled(control.toApi(), enabled)
    }
    override fun requestScreenOff() { client.requestScreenOff() }
}
