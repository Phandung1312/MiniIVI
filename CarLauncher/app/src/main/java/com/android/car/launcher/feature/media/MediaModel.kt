package com.android.car.launcher.feature.media

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class MediaTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val contentUri: String,
    val durationMillis: Long,
)

data class MediaState(
    val permissionGranted: Boolean = false,
    val tracks: List<MediaTrack> = emptyList(),
    val selectedIndex: Int = 0,
    val isLoading: Boolean = false,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
) {
    val currentTrack: MediaTrack?
        get() = tracks.getOrNull(selectedIndex)
}

@HiltViewModel
class MediaModel @Inject constructor(
    private val repository: MediaRepository,
) : ViewModel() {
    val state = repository.state
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun onStoragePermissionResult(granted: Boolean) {
        repository.setPermissionGranted(granted)
        if (granted && state.value.tracks.isEmpty()) loadSongs()
    }

    fun loadSongs() {
        if (!state.value.permissionGranted || state.value.isLoading) return
        scope.launch { repository.loadSongs() }
    }

    fun onPlayPause() = repository.togglePlayback()
    fun onNext() = repository.playNext()
    fun onPrevious() = repository.playPrevious()
    fun onTrackSelected(index: Int) = repository.play(index)

    override fun onCleared() {
        scope.cancel()
        repository.releasePlayer()
        super.onCleared()
    }
}
