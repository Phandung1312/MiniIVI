package com.android.car.launcher.feature.dashboard.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStartDestinationTest {
    @Test
    fun appsValueOpensAppDrawer() {
        assertEquals(HomeDestination.Apps, HomeStartDestination.fromValue("apps"))
    }

    @Test
    fun missingOrUnknownValueOpensDashboard() {
        assertEquals(HomeDestination.Home, HomeStartDestination.fromValue(null))
        assertEquals(HomeDestination.Home, HomeStartDestination.fromValue("unknown"))
    }
}
