package com.android.car.launcher.feature.media.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.car.launcher.feature.media.domain.model.MediaState
import com.android.car.launcher.feature.media.domain.usecase.LoadMediaLibraryUseCase
import com.android.car.launcher.feature.media.domain.usecase.ObserveMediaStateUseCase
import com.android.car.launcher.feature.media.domain.usecase.PlayNextMediaTrackUseCase
import com.android.car.launcher.feature.media.domain.usecase.PlayPreviousMediaTrackUseCase
import com.android.car.launcher.feature.media.domain.usecase.ReleaseMediaPlayerUseCase
import com.android.car.launcher.feature.media.domain.usecase.SelectMediaTrackUseCase
import com.android.car.launcher.feature.media.domain.usecase.ToggleMediaPlaybackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MediaViewModel @Inject constructor(
    observeMediaState: ObserveMediaStateUseCase,
    private val loadMediaLibrary: LoadMediaLibraryUseCase,
    private val togglePlayback: ToggleMediaPlaybackUseCase,
    private val playNext: PlayNextMediaTrackUseCase,
    private val playPrevious: PlayPreviousMediaTrackUseCase,
    private val selectTrack: SelectMediaTrackUseCase,
    private val releasePlayer: ReleaseMediaPlayerUseCase,
) : ViewModel() {
    val state = observeMediaState()

    fun loadSongs() {
        if (state.value.isLoading) return
        viewModelScope.launch { loadMediaLibrary() }
    }

    fun onPlayPause() = togglePlayback()
    fun onNext() = playNext()
    fun onPrevious() = playPrevious()
    fun onTrackSelected(index: Int) = selectTrack(index)

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }
}
