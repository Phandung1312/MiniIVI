package com.android.car.systemui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.android.car.systemui.presentation.BottomNavigationScreen
import com.android.car.systemui.presentation.CarSystemUiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SystemUiComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quickControlIsTheRightmostNavigationAction() {
        var clicked = false
        composeRule.setContent {
            CarSystemUiTheme {
                BottomNavigationScreen(
                    quickControlVisible = false,
                    onHome = {},
                    onSettings = {},
                    onAppList = {},
                    onQuickControl = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Trung tâm điều khiển")
            .assertIsDisplayed()
            .performClick()
        assertTrue(clicked)
    }
}
