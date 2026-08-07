package com.android.car.systemui.data.repository

import android.content.Context
import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.ClimateZone
import com.android.car.systemui.data.model.HvacState
import kotlinx.coroutines.flow.StateFlow

interface BrightnessRepository {
    val state: StateFlow<BrightnessState>
    fun start()
    fun stop()
    fun refresh()
    suspend fun setBrightness(progress: Float)
}

interface AudioRepository {
    val state: StateFlow<AudioState>
    fun start()
    fun stop()
    fun refresh()
    fun setVolume(volume: Int)
}

interface HvacRepository {
    val state: StateFlow<HvacState>
    fun start()
    fun stop()
    fun adjustTemperature(zone: ClimateZone, delta: Float)
    fun setAc(enabled: Boolean)
}

interface NavigationRepository {
    fun goHome()
    fun openSettings()
    fun openAppList()
}

interface StartupRepository {
    fun initialize(context: Context)
}
