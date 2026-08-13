package com.miniivi.maps

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MapControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backAndMyLocationActionsAreExposed() {
        var backClicks = 0
        var locationClicks = 0
        composeRule.setContent {
            MapControls(
                loading = false,
                locating = false,
                loadFailed = false,
                onBack = { backClicks += 1 },
                onMyLocation = { locationClicks += 1 },
                onRetry = {},
            )
        }

        composeRule.onNodeWithTag("map_back").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("map_my_location").assertIsDisplayed().performClick()

        assertEquals(1, backClicks)
        assertEquals(1, locationClicks)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retryClicks = 0
        composeRule.setContent {
            MapControls(
                loading = false,
                locating = false,
                loadFailed = true,
                onBack = {},
                onMyLocation = {},
                onRetry = { retryClicks += 1 },
            )
        }

        composeRule.onNodeWithText("Unable to load the map").assertIsDisplayed()
        composeRule.onNodeWithTag("map_retry").assertIsDisplayed().performClick()

        assertEquals(1, retryClicks)
    }

    @Test
    fun loadingStateShowsProgress() {
        composeRule.setContent {
            MapControls(
                loading = true,
                locating = false,
                loadFailed = false,
                onBack = {},
                onMyLocation = {},
                onRetry = {},
            )
        }

        composeRule.onNodeWithTag("map_loading").assertIsDisplayed()
    }
}
