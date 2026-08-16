package com.android.car.launcher.feature.media.domain.repository

import com.android.car.launcher.feature.media.domain.model.MediaState
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    val state: StateFlow<MediaState>

    suspend fun loadSongs()
    fun togglePlayback()
    fun playNext()
    fun playPrevious()
    fun play(index: Int)
    fun releasePlayer()
}
