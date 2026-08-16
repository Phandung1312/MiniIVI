package com.android.car.launcher.feature.media.domain.usecase

import com.android.car.launcher.feature.media.domain.model.MediaState
import com.android.car.launcher.feature.media.domain.repository.MediaRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ObserveMediaStateUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    operator fun invoke(): StateFlow<MediaState> = repository.state
}

class LoadMediaLibraryUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    suspend operator fun invoke() = repository.loadSongs()
}

class ToggleMediaPlaybackUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    operator fun invoke() = repository.togglePlayback()
}

class PlayNextMediaTrackUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    operator fun invoke() = repository.playNext()
}

class PlayPreviousMediaTrackUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    operator fun invoke() = repository.playPrevious()
}

class SelectMediaTrackUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    operator fun invoke(index: Int) = repository.play(index)
}

class ReleaseMediaPlayerUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    operator fun invoke() = repository.releasePlayer()
}
