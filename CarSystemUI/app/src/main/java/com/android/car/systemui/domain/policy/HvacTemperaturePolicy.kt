package com.android.car.systemui.domain.policy

import com.android.car.systemui.domain.model.TemperatureZone

internal object HvacTemperaturePolicy {
    fun adjust(zone: TemperatureZone, delta: Float): Float =
        ((zone.temperature ?: return zone.minimum) + delta).coerceIn(zone.minimum, zone.maximum)
}
