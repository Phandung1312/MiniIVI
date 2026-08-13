package com.android.car.launcher.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstFrameNotifierTest {
    @Test
    fun notifiesOnlyOnceForAColdActivityCreation() {
        var notifications = 0
        val notifier = FirstFrameNotifier { notifications++ }

        notifier.notifyFrameSubmitted()
        notifier.notifyFrameSubmitted()

        assertEquals(1, notifications)
    }
}
