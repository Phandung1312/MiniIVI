package com.android.car.systemui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.longClick
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import com.android.car.systemui.domain.model.AudioState
import com.android.car.systemui.domain.model.BrightnessState
import com.android.car.systemui.domain.model.ClimateControlState
import com.android.car.systemui.domain.model.ClimateFanDirection
import com.android.car.systemui.domain.model.ClimateZone
import com.android.car.systemui.domain.model.ClimateZoneControlState
import com.android.car.systemui.domain.model.ExtendedControlsState
import com.android.car.systemui.domain.model.HvacState
import com.android.car.systemui.domain.model.QuickControl
import com.android.car.systemui.domain.model.QuickControlsState
import com.android.car.systemui.presentation.CarSystemUiTheme
import com.android.car.systemui.presentation.NavigationRailScreen
import com.android.car.systemui.presentation.ControlCenterOverlay
import com.android.car.systemui.presentation.model.ControlCenterUiState
import com.android.car.systemui.presentation.model.NavigationDestination
import kotlin.math.abs
import org.junit.Assert.assertEquals
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
    fun climateFanDirectionsUseThreeStableOptionsAndOneSelectionPerZone() {
        val clickedDirections = mutableListOf<Pair<ClimateZone, ClimateFanDirection>>()
        composeRule.setContent {
            CarSystemUiTheme {
                ControlCenterOverlay(
                    visible = true,
                    state = ControlCenterUiState(
                        extendedControls = ExtendedControlsState(
                            climate = ClimateControlState(
                                driverZone = ClimateZoneControlState(
                                    zone = ClimateZone.LEFT,
                                    fanDirection = ClimateFanDirection.FACE_AND_FEET,
                                    availableFanDirections = listOf(
                                        ClimateFanDirection.FACE,
                                        ClimateFanDirection.FACE,
                                    ),
                                ),
                                passengerZone = ClimateZoneControlState(
                                    zone = ClimateZone.RIGHT,
                                    fanDirection = ClimateFanDirection.FACE,
                                    availableFanDirections = listOf(
                                        ClimateFanDirection.FACE,
                                        ClimateFanDirection.FACE_AND_FEET,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onDismiss = {},
                    onBrightnessChanged = {},
                    onBrightnessChangeFinished = {},
                    onVolumeChanged = {},
                    onTemperatureDecrease = {},
                    onTemperatureIncrease = {},
                    onAcChanged = {},
                    onSettings = {},
                    onFanDirectionChanged = { zone, direction ->
                        clickedDirections += zone to direction
                    },
                    modifier = Modifier.size(1920.dp, 1080.dp),
                )
            }
        }

        composeRule.onAllNodesWithContentDescription("Face airflow")
            .assertCountEquals(2)
        composeRule.onAllNodesWithContentDescription("Feet airflow")
            .assertCountEquals(2)
        composeRule.onAllNodesWithContentDescription("Face and feet airflow")
            .assertCountEquals(2)

        assertEquals(listOf(false, true), selectedStates("Face airflow"))
        assertEquals(listOf(false, false), selectedStates("Feet airflow"))
        assertEquals(listOf(true, false), selectedStates("Face and feet airflow"))

        composeRule.onAllNodesWithContentDescription("Face airflow")[0].performClick()
        composeRule.onAllNodesWithContentDescription("Feet airflow")[0].performClick()
        composeRule.onAllNodesWithContentDescription("Face and feet airflow")[0].performClick()

        assertEquals(
            listOf(
                ClimateZone.LEFT to ClimateFanDirection.FACE,
                ClimateZone.LEFT to ClimateFanDirection.FEET,
                ClimateZone.LEFT to ClimateFanDirection.FACE_AND_FEET,
            ),
            clickedDirections,
        )
    }

    @Test
    fun navigationRailShowsSelectedDestinationAndStatusPanel() {
        composeRule.setContent {
            CarSystemUiTheme {
                NavigationRailScreen(
                    controlCenterVisible = false,
                    selectedDestination = NavigationDestination.APP_LIST,
                    quickControls = QuickControlsState(
                        available = true,
                        wifiEnabled = true,
                        wifiConnected = true,
                        bluetoothEnabled = false,
                        capabilities = setOf(QuickControl.WIFI, QuickControl.BLUETOOTH),
                    ),
                    audio = AudioState(volume = 0, maximum = 10, available = true),
                    onHome = {},
                    onAppList = {},
                    onPhone = {},
                    onControlCenter = {},
                    onSettings = {},
                    modifier = Modifier.size(135.2.dp, 720.dp),
                )
            }
        }

        composeRule.onNodeWithTag("navigation_home").assertIsDisplayed()
        composeRule.onNodeWithTag("navigation_apps").assertIsDisplayed()
        composeRule.onNodeWithTag("navigation_call").assertIsDisplayed()
        composeRule.onNodeWithTag("navigation_vehicle_controls").assertIsDisplayed()
        composeRule.onNodeWithTag("navigation_status_panel").assertIsDisplayed()
        val statusBounds = composeRule.onNodeWithTag("navigation_status_panel")
            .fetchSemanticsNode().boundsInRoot
        val callBounds = composeRule.onNodeWithTag("navigation_call")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(callBounds.width, statusBounds.width, 0.01f)
        assertTrue(callBounds.top >= statusBounds.bottom)
        composeRule.onNodeWithContentDescription("Wi-Fi Connected")
            .assertIsDisplayed()
            .assertHasNoClickAction()
        composeRule.onNodeWithContentDescription("Bluetooth Disabled")
            .assertIsDisplayed()
            .assertHasNoClickAction()
        composeRule.onNodeWithContentDescription("Audio muted")
            .assertIsDisplayed()
            .assertHasNoClickAction()
        composeRule.onNodeWithTag("navigation_home").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_apps").assertIsSelected()
        composeRule.onNodeWithTag("navigation_call").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_vehicle_controls").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_settings").assertIsNotSelected()
    }

    @Test
    fun navigationRailHasAtMostOneSelectedButton() {
        var selectedDestination by mutableStateOf(NavigationDestination.NONE)
        var controlCenterVisible by mutableStateOf(false)
        composeRule.setContent {
            CarSystemUiTheme {
                NavigationRailScreen(
                    controlCenterVisible = controlCenterVisible,
                    selectedDestination = selectedDestination,
                    onHome = {},
                    onAppList = {},
                    onPhone = {},
                    onControlCenter = {},
                    onSettings = {},
                    modifier = Modifier.size(135.2.dp, 720.dp),
                )
            }
        }

        composeRule.onNodeWithTag("navigation_home").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_apps").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_call").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_vehicle_controls").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_settings").assertIsNotSelected()

        selectedDestination = NavigationDestination.CONTROL_CENTER
        controlCenterVisible = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("navigation_home").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_apps").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_call").assertIsNotSelected()
        composeRule.onNodeWithTag("navigation_vehicle_controls").assertIsSelected()
        composeRule.onNodeWithTag("navigation_settings").assertIsNotSelected()
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
                    onOpenCamera = { showCamera = true },
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

    @Test
    fun longPressingWifiAndBluetoothOpensTargetsWithoutToggling() {
        val toggledControls = mutableListOf<QuickControl>()
        var wifiOpened = false
        var bluetoothOpened = false
        composeRule.setContent {
            CarSystemUiTheme {
                ControlCenterOverlay(
                    visible = true,
                    state = ControlCenterUiState(
                        extendedControls = ExtendedControlsState(
                            quickControls = QuickControlsState(
                                available = true,
                                capabilities = setOf(QuickControl.WIFI, QuickControl.BLUETOOTH),
                            ),
                        ),
                    ),
                    onDismiss = {},
                    onBrightnessChanged = {},
                    onBrightnessChangeFinished = {},
                    onVolumeChanged = {},
                    onTemperatureDecrease = {},
                    onTemperatureIncrease = {},
                    onAcChanged = {},
                    onQuickControlChanged = { control, _ -> toggledControls += control },
                    onOpenWifiSettings = { wifiOpened = true },
                    onOpenBluetoothApp = { bluetoothOpened = true },
                    onSettings = {},
                    modifier = Modifier.size(960.dp, 520.dp),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Wi-Fi").performClick()
        composeRule.onNodeWithContentDescription("Wi-Fi").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Bluetooth").performTouchInput { longClick() }

        assertEquals(listOf(QuickControl.WIFI), toggledControls)
        assertTrue(wifiOpened)
        assertTrue(bluetoothOpened)
    }

    private fun selectedStates(description: String): List<Boolean> =
        composeRule.onAllNodesWithContentDescription(description)
            .fetchSemanticsNodes()
            .map {
                if (it.config.contains(SemanticsProperties.Selected)) {
                    it.config[SemanticsProperties.Selected]
                } else {
                    false
                }
            }
}
