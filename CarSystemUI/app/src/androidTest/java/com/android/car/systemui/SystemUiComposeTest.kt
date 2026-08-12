package com.android.car.systemui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.presentation.CarSystemUiTheme
import com.android.car.systemui.presentation.NavigationRailScreen
import com.android.car.systemui.presentation.ControlCenterOverlay
import com.android.car.systemui.presentation.ControlCenterUiState
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SystemUiComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationRailShowsBrandAndExposesSystemActions() {
        var clicked = false
        composeRule.setContent {
            CarSystemUiTheme {
                NavigationRailScreen(
                    controlCenterVisible = false,
                    onHome = {},
                    onAppList = {},
                    onControlCenter = { clicked = true },
                    onSettings = {},
                    modifier = Modifier.size(135.2.dp, 720.dp),
                )
            }
        }

        composeRule.onNodeWithTag("navigation_brand").assertIsDisplayed()
        composeRule.onNodeWithText("IVI").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("MI").fetchSemanticsNodes().isEmpty())
        val brandBounds = composeRule.onNodeWithTag("navigation_brand")
            .fetchSemanticsNode().boundsInRoot
        val brandTextBounds = composeRule.onNodeWithText("IVI")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            abs(brandBounds.center.x - brandTextBounds.center.x) <= 1f &&
                abs(brandBounds.center.y - brandTextBounds.center.y) <= 1f,
        )
        assertTrue(
            brandBounds.width in 83f..84f,
        )
        assertTrue(
            composeRule.onNodeWithContentDescription("Home")
                .fetchSemanticsNode().boundsInRoot.width in 83f..84f,
        )
        assertTrue(
            composeRule.onAllNodesWithContentDescription("Back")
                .fetchSemanticsNodes().isEmpty(),
        )
        composeRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("App list").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Control Center")
            .assertIsDisplayed()
            .performClick()
        assertTrue(clicked)
    }

    @Test
    fun controlCenterUsesCabinFlowLayoutAndEnglishDescriptions() {
        composeRule.setContent {
            CarSystemUiTheme {
                ControlCenterOverlay(
                    visible = true,
                    state = ControlCenterUiState(
                        brightness = BrightnessState(available = true),
                        audio = AudioState(volume = 5, maximum = 10, available = true),
                        hvac = HvacState(available = false, connecting = false),
                    ),
                    onDismiss = {},
                    onBrightnessChanged = {},
                    onBrightnessChangeFinished = {},
                    onVolumeChanged = {},
                    onTemperatureDecrease = {},
                    onTemperatureIncrease = {},
                    onAcChanged = {},
                    onSettings = {},
                    modifier = Modifier.size(960.dp, 520.dp),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Brightness").assertIsDisplayed()
        composeRule.onNodeWithText("Climate & Controls").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Cabin airflow").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Camera").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Screen Off").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Camera")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun moreClimateAndMockCameraFlowsAreRenderedInTheOverlayWindow() {
        var showMore by mutableStateOf(false)
        var showCamera by mutableStateOf(false)
        composeRule.setContent {
            CarSystemUiTheme {
                ControlCenterOverlay(
                    visible = true,
                    state = ControlCenterUiState(
                        brightness = BrightnessState(available = true),
                        audio = AudioState(volume = 5, maximum = 10, available = true),
                        hvac = HvacState(available = false, connecting = false),
                        moreClimateVisible = showMore,
                        cameraVisible = showCamera,
                    ),
                    onDismiss = {},
                    onBrightnessChanged = {},
                    onBrightnessChangeFinished = {},
                    onVolumeChanged = {},
                    onTemperatureDecrease = {},
                    onTemperatureIncrease = {},
                    onAcChanged = {},
                    onSettings = {},
                    onShowMoreClimate = { showMore = true },
                    onShowCamera = { showCamera = true },
                    modifier = Modifier.size(1920.dp, 1080.dp),
                )
            }
        }

        composeRule.onNodeWithContentDescription("More climate").performClick()
        composeRule.onNodeWithText("More Climate").assertIsDisplayed()
        composeRule.onNodeWithText("Temperature units").assertIsDisplayed()

        showMore = false
        showCamera = true
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Mock 360 degree camera view").assertIsDisplayed()
    }
}
