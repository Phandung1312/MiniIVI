package com.android.car.launcher.feature.media.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRefreshCoordinatorTest {
    @Test
    fun collapsesNotificationsWithinDebounceWindow() = runTest {
        var refreshCount = 0
        val coordinator = MediaRefreshCoordinator(this, DEBOUNCE_MILLIS) { refreshCount++ }

        repeat(3) { coordinator.requestRefresh() }
        advanceTimeBy(DEBOUNCE_MILLIS - 1)
        runCurrent()
        assertEquals(0, refreshCount)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, refreshCount)

        coordinator.close()
    }

    @Test
    fun refreshesAgainWhenMediaChangesAfterAnEmptyResult() = runTest {
        var availableTracks = emptyList<String>()
        val observedLibraries = mutableListOf<List<String>>()
        val coordinator = MediaRefreshCoordinator(this, DEBOUNCE_MILLIS) {
            observedLibraries += availableTracks
        }

        coordinator.requestRefresh()
        advanceTimeBy(DEBOUNCE_MILLIS)
        runCurrent()

        availableTracks = listOf("sample-track.mp3")
        coordinator.requestRefresh()
        advanceTimeBy(DEBOUNCE_MILLIS)
        runCurrent()

        assertEquals(listOf(emptyList<String>(), availableTracks), observedLibraries)
        coordinator.close()
    }

    @Test
    fun neverRunsRefreshesConcurrently() = runTest {
        var activeRefreshes = 0
        var maximumActiveRefreshes = 0
        var refreshCount = 0
        val coordinator = MediaRefreshCoordinator(this, DEBOUNCE_MILLIS) {
            activeRefreshes++
            maximumActiveRefreshes = maxOf(maximumActiveRefreshes, activeRefreshes)
            refreshCount++
            delay(REFRESH_DURATION_MILLIS)
            activeRefreshes--
        }

        coordinator.requestRefresh()
        advanceTimeBy(DEBOUNCE_MILLIS)
        runCurrent()
        repeat(3) { coordinator.requestRefresh() }

        advanceTimeBy(REFRESH_DURATION_MILLIS + DEBOUNCE_MILLIS)
        runCurrent()

        assertEquals(2, refreshCount)
        assertEquals(1, maximumActiveRefreshes)
        coordinator.close()
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 300L
        const val REFRESH_DURATION_MILLIS = 500L
    }
}
