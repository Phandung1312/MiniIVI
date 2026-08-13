package com.android.car.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.ComposeTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.unit.dp
import com.android.car.launcher.core.ui.MiniIviTheme
import com.android.car.launcher.feature.dashboard.DashboardUiState
import com.android.car.launcher.feature.dashboard.HomeAppCatalog
import com.android.car.launcher.feature.dashboard.HomeDestination
import com.android.car.launcher.feature.dashboard.ui.HomeScreen
import com.miniivi.car.api.FeatureStatus
import com.miniivi.car.api.VehicleStatusState
import org.junit.Rule
import org.junit.Test

class LauncherComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboardShowsHonestUnavailableStates() {
        composeRule.setContent {
            MiniIviTheme {
                HomeScreen(
                    destination = HomeDestination.Home,
                    state = DashboardUiState(),
                    apps = HomeAppCatalog.apps,
                    onBackToHome = {},
                    onAppClick = {},
                    onPlayPause = {},
                    onNext = {},
                    onPrevious = {},
                )
            }
        }

        composeRule.onNodeWithText("Weather unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("No media loaded").assertIsDisplayed()
        composeRule.onNodeWithText("Vehicle status").assertIsDisplayed()
        composeRule.onNodeWithText("Battery").assertIsDisplayed()
        composeRule.onNodeWithText("Cabin").assertIsDisplayed()
        composeRule.onNodeWithText("Outside").assertIsDisplayed()
        composeRule.onNodeWithText("Range").assertIsDisplayed()
        composeRule.onNodeWithText("Tires").assertIsDisplayed()
        composeRule.onNodeWithText("Climate").assertIsDisplayed()
        composeRule.onNodeWithTag("vehicle_background").assertIsDisplayed()
        composeRule.onNodeWithTag("vehicle_header_icon").assertIsDisplayed()
        listOf("battery", "cabin", "outside", "range", "tires", "climate").forEach { tag ->
            composeRule.onNodeWithTag("vehicle_metric_icon_$tag").assertIsDisplayed()
        }
    }

    @Test
    fun appDrawerShowsExistingApplications() {
        composeRule.setContent {
            MiniIviTheme {
                HomeScreen(
                    destination = HomeDestination.Apps,
                    state = DashboardUiState(),
                    apps = HomeAppCatalog.apps,
                    onBackToHome = {},
                    onAppClick = {},
                    onPlayPause = {},
                    onNext = {},
                    onPrevious = {},
                )
            }
        }

        composeRule.onNodeWithText("Bluetooth").assertIsDisplayed()
        composeRule.onNodeWithText("Video").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.assertTextAbsent("YouTube")
        composeRule.assertTextAbsent("Apps")
        composeRule.assertTextAbsent("Choose an application")
    }

    @Test
    fun appDrawerUsesFourColumns() {
        composeRule.setContent {
            Box(Modifier.size(1280.dp, 720.dp)) {
                MiniIviTheme {
                    HomeScreen(
                        destination = HomeDestination.Apps,
                        state = DashboardUiState(),
                        apps = HomeAppCatalog.apps,
                        onBackToHome = {},
                        onAppClick = {},
                        onPlayPause = {},
                        onNext = {},
                        onPrevious = {},
                    )
                }
            }
        }

        val firstItem = composeRule.onNodeWithTag("app_item_media")
            .fetchSemanticsNode().boundsInRoot
        val fourthItem = composeRule.onNodeWithTag("app_item_browser")
            .fetchSemanticsNode().boundsInRoot
        val fifthItem = composeRule.onNodeWithTag("app_item_bluetooth")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(firstItem.width, firstItem.height, 1f)
        assertEquals(firstItem.top, fourthItem.top, 1f)
        assertTrue(fifthItem.top > firstItem.top)
    }

    @Test
    fun dashboardUsesFullWidthRowsAndEqualMapVehicleColumns() {
        composeRule.setContent {
            Box(Modifier.size(1280.dp, 720.dp).testTag("dashboard_root")) {
                MiniIviTheme {
                    HomeScreen(
                        destination = HomeDestination.Home,
                        state = DashboardUiState(),
                        apps = HomeAppCatalog.apps,
                        onBackToHome = {},
                        onAppClick = {},
                        onPlayPause = {},
                        onNext = {},
                        onPrevious = {},
                        mapPreviewContent = {
                            Box(Modifier.fillMaxSize().testTag("fake_map_preview"))
                        },
                    )
                }
            }
        }

        val topRowBounds = composeRule.onNodeWithTag("dashboard_top_row")
            .fetchSemanticsNode().boundsInRoot
        val bottomRowBounds = composeRule.onNodeWithTag("dashboard_bottom_row")
            .fetchSemanticsNode().boundsInRoot
        val mapBounds = composeRule.onNodeWithTag("map_card")
            .fetchSemanticsNode().boundsInRoot
        val vehicleColumnBounds = composeRule.onNodeWithTag("vehicle_column")
            .fetchSemanticsNode().boundsInRoot
        val vehicleCardBounds = composeRule.onNodeWithTag("vehicle_background")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeRule.onNodeWithTag("vehicle_image")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(167.2f, topRowBounds.left, 1f)
        assertEquals(topRowBounds.width, bottomRowBounds.width, 1f)
        assertEquals(mapBounds.width, vehicleColumnBounds.width, 1f)
        assertEquals(mapBounds.height, vehicleColumnBounds.height, 1f)
        assertEquals(mapBounds.top, vehicleColumnBounds.top, 1f)
        assertEquals(mapBounds.bottom, vehicleColumnBounds.bottom, 1f)
        assertEquals(mapBounds.width, vehicleCardBounds.width, 1f)
        assertEquals(mapBounds.top, vehicleCardBounds.top, 1f)
        assertTrue(vehicleCardBounds.bottom < mapBounds.bottom)
        assertTrue(imageBounds.left >= vehicleColumnBounds.left)
        assertTrue(imageBounds.right <= vehicleColumnBounds.right)
        assertTrue(imageBounds.top >= vehicleCardBounds.bottom)
        assertTrue(imageBounds.bottom <= vehicleColumnBounds.bottom)
        assertTrue(
            !composeRule.onNodeWithTag("vehicle_metrics_panel")
                .fetchSemanticsNode()
                .config
                .contains(SemanticsActions.OnClick),
        )
        composeRule.onNodeWithTag("fake_map_preview").assertIsDisplayed()
    }

    @Test
    fun vehicleImageGrowsOnFullHdDashboard() {
        composeRule.setContent {
            Box(Modifier.size(1920.dp, 1080.dp)) {
                MiniIviTheme {
                    HomeScreen(
                        destination = HomeDestination.Home,
                        state = DashboardUiState(
                            vehicleStatus = VehicleStatusState(
                                status = FeatureStatus.READY,
                                available = true,
                                hasBatteryPercentage = true,
                                batteryPercentage = 100f,
                                hasOutsideTemperature = true,
                                outsideTemperatureCelsius = 25f,
                                hasRange = true,
                                rangeKilometers = 50f,
                                hasTirePressure = true,
                                minimumTirePressureKpa = 200f,
                            ),
                        ),
                        apps = HomeAppCatalog.apps,
                        onBackToHome = {},
                        onAppClick = {},
                        onPlayPause = {},
                        onNext = {},
                        onPrevious = {},
                    )
                }
            }
        }

        val vehicleColumnBounds = composeRule.onNodeWithTag("vehicle_column")
            .fetchSemanticsNode().boundsInRoot
        val vehicleCardBounds = composeRule.onNodeWithTag("vehicle_background")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeRule.onNodeWithTag("vehicle_image")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(imageBounds.width >= 600f)
        assertTrue(imageBounds.left >= vehicleColumnBounds.left)
        assertTrue(imageBounds.right <= vehicleColumnBounds.right)
        assertTrue(imageBounds.top >= vehicleCardBounds.bottom)
        assertTrue(imageBounds.bottom <= vehicleColumnBounds.bottom)
    }
}

private fun ComposeTestRule.assertTextAbsent(text: String) {
    val exists = try {
        onNodeWithText(text).fetchSemanticsNode()
        true
    } catch (_: AssertionError) {
        false
    }
    assertTrue("Expected text '$text' to be absent", !exists)
}
