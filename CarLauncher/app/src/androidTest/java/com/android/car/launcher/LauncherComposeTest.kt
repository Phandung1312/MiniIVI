package com.android.car.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlin.math.max
import org.junit.Assert.assertTrue
import androidx.compose.ui.unit.dp
import com.android.car.launcher.core.ui.MiniIviTheme
import com.android.car.launcher.feature.dashboard.DashboardUiState
import com.android.car.launcher.feature.dashboard.HomeAppCatalog
import com.android.car.launcher.feature.dashboard.HomeDestination
import com.android.car.launcher.feature.dashboard.ui.HomeScreen
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
        composeRule.onNodeWithText("Vehicle data unavailable").assertIsDisplayed()
        composeRule.onNodeWithTag("vehicle_background").assertIsDisplayed()
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

        composeRule.onNodeWithText("Apps").assertIsDisplayed()
        composeRule.onNodeWithText("Bluetooth").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun vehicleLayerStaysConstrainedAndAnchoredToBottomEnd() {
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
                    )
                }
            }
        }

        val rootBounds = composeRule.onNodeWithTag("dashboard_root")
            .fetchSemanticsNode().boundsInRoot
        val vehicleBounds = composeRule.onNodeWithTag("vehicle_background")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(vehicleBounds.width <= rootBounds.width * 0.24f + 2f)
        assertTrue(vehicleBounds.right >= rootBounds.right - max(64f, rootBounds.width * 0.04f))
        assertTrue(vehicleBounds.bottom >= rootBounds.bottom - max(64f, rootBounds.height * 0.10f))
    }
}
