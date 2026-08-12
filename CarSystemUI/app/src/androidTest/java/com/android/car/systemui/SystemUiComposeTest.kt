package com.android.car.systemui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.layout.size
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
    fun controlCenterCardsUseEqualControlBodyHeightsAndEnglishDescriptions() {
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
        composeRule.onNodeWithText("Control Center").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        val brightnessHeight = composeRule.onNodeWithTag("brightness_slider", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.height
        val volumeHeight = composeRule.onNodeWithTag("volume_slider", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.height
        val hvacHeight = composeRule.onNodeWithTag("hvac_card", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.height

        assertTrue(abs(brightnessHeight - volumeHeight) <= 1f)
        assertTrue(abs(brightnessHeight - hvacHeight) <= 1f)
    }
}
