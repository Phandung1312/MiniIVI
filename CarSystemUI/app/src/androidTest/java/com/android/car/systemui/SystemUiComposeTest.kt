package com.android.car.systemui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.car.systemui.data.model.AudioState
import com.android.car.systemui.data.model.BrightnessState
import com.android.car.systemui.data.model.HvacState
import com.android.car.systemui.presentation.CarSystemUiTheme
import com.android.car.systemui.presentation.NavigationRailScreen
import com.android.car.systemui.presentation.QuickControlOverlay
import com.android.car.systemui.presentation.QuickControlUiState
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
                    quickControlVisible = false,
                    onHome = {},
                    onAppList = {},
                    onQuickControl = { clicked = true },
                    onSettings = {},
                    modifier = Modifier.size(104.dp, 720.dp),
                )
            }
        }

        composeRule.onNodeWithTag("navigation_brand").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("App list").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Quick controls")
            .assertIsDisplayed()
            .performClick()
        assertTrue(clicked)
    }

    @Test
    fun quickControlCardsUseEqualControlBodyHeightsAndEnglishDescriptions() {
        composeRule.setContent {
            CarSystemUiTheme {
                QuickControlOverlay(
                    visible = true,
                    state = QuickControlUiState(
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
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        val brightnessHeight = composeRule.onNodeWithTag("brightness_slider")
            .fetchSemanticsNode().boundsInRoot.height
        val volumeHeight = composeRule.onNodeWithTag("volume_slider")
            .fetchSemanticsNode().boundsInRoot.height
        val hvacHeight = composeRule.onNodeWithTag("hvac_card")
            .fetchSemanticsNode().boundsInRoot.height

        assertTrue(abs(brightnessHeight - volumeHeight) <= 1f)
        assertTrue(abs(brightnessHeight - hvacHeight) <= 1f)
    }
}
