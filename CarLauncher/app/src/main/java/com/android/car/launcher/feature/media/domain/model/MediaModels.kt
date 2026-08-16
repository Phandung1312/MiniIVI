package com.android.car.launcher.feature.media.domain.model

data class MediaTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMillis: Long,
)

data class MediaState(
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
