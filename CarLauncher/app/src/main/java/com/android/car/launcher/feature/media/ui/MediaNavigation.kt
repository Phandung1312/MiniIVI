package com.android.car.launcher.feature.media.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.car.launcher.feature.media.MediaState

private object MediaDestination {
    const val PLAYER = "player"
    const val LIBRARY = "library"
}

@Composable
internal fun MediaNavigation(
    state: MediaState,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTrackSelected: (Int) -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MediaDestination.PLAYER,
    ) {
        composable(MediaDestination.PLAYER) {
            PlayerScreen(
                state = state,
                onBack = onBack,
                onLoad = onLoad,
                onOpenLibrary = { navController.navigate(MediaDestination.LIBRARY) },
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
            )
        }
        composable(MediaDestination.LIBRARY) {
            LibraryScreen(
                state = state,
                navController = navController,
                onLoad = onLoad,
                onTrackSelected = onTrackSelected,
            )
        }
    }
}
