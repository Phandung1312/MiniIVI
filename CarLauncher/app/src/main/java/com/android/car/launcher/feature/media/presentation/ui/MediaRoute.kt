package com.android.car.launcher.feature.media.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.android.car.launcher.feature.media.presentation.viewmodel.MediaViewModel
import com.android.car.launcher.feature.media.presentation.navigation.MediaNavigation

@Composable
internal fun MediaRoute(
    viewModel: MediaViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadSongs()
    }

    MediaNavigation(
        state = state,
        onBack = onBack,
        onLoad = viewModel::loadSongs,
        onPlayPause = viewModel::onPlayPause,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onTrackSelected = viewModel::onTrackSelected,
    )
}
