package com.android.car.launcher.feature.media.data.repository

import android.content.Context
import android.database.ContentObserver
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.android.car.launcher.feature.media.data.datasource.MediaStoreDataSource
import com.android.car.launcher.feature.media.data.datasource.MediaStoreTrack
import com.android.car.launcher.feature.media.domain.model.MediaState
import com.android.car.launcher.feature.media.domain.model.MediaTrack
import com.android.car.launcher.feature.media.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: MediaStoreDataSource,
) : MediaRepository {
    private val _state = MutableStateFlow(MediaState())
    override val state = _state.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val loadMutex = Mutex()
    private val sourceUris = mutableMapOf<Long, String>()
    private val refreshCoordinator = MediaRefreshCoordinator(
        scope = repositoryScope,
        debounceMillis = MEDIA_CHANGE_DEBOUNCE_MILLIS,
        refresh = ::loadSongs,
    )
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshCoordinator.requestRefresh()
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            refreshCoordinator.requestRefresh()
        }
    }

    private var mediaPlayer: MediaPlayer? = null

    init {
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver,
        )
    }

    override suspend fun loadSongs(): Unit = loadMutex.withLock {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val storeTracks = withContext(Dispatchers.IO) { dataSource.querySongs() }
            val tracks = storeTracks.map(MediaStoreTrack::track)
            sourceUris.clear()
            storeTracks.forEach { sourceUris[it.track.id] = it.contentUri }

            val previousTrackId = _state.value.currentTrack?.id
            val updatedIndex = tracks.indexOfFirst { it.id == previousTrackId }
            val keepsCurrentTrack = previousTrackId != null && updatedIndex >= 0
            if (!keepsCurrentTrack) {
                releasePlayer(updateState = false)
            }
            _state.update {
                it.copy(
                    tracks = tracks,
                    selectedIndex = if (keepsCurrentTrack) updatedIndex else 0,
                    isLoading = false,
                    isPreparing = if (keepsCurrentTrack) it.isPreparing else false,
                    isPlaying = if (keepsCurrentTrack) it.isPlaying else false,
                    errorMessage = null,
                )
            }
            Log.i(
                TAG,
                "event=library_loaded track_count=${tracks.size} retained_selection=$keepsCurrentTrack",
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "event=library_load_failed", error)
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load audio files",
                )
            }
        }
    }

    override fun togglePlayback() {
        val player = mediaPlayer
        when {
            _state.value.tracks.isEmpty() || _state.value.isPreparing -> Unit
            player == null -> play(_state.value.selectedIndex)
            player.isPlaying -> {
                player.pause()
                _state.update { it.copy(isPlaying = false) }
                logDebug("event=playback_changed state=paused")
            }
            else -> {
                player.start()
                _state.update { it.copy(isPlaying = true) }
                logDebug("event=playback_changed state=playing")
            }
        }
    }

    override fun playNext() {
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return
        play((_state.value.selectedIndex + 1) % tracks.size)
    }

    override fun playPrevious() {
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return
        val previous = if (_state.value.selectedIndex == 0) tracks.lastIndex
        else _state.value.selectedIndex - 1
        play(previous)
    }

    override fun play(index: Int) {
        val track = _state.value.tracks.getOrNull(index) ?: return
        val contentUri = sourceUris[track.id] ?: return
        logDebug("event=playback_requested index=$index track_count=${_state.value.tracks.size}")
        releasePlayer(updateState = false)
        _state.update {
            it.copy(
                selectedIndex = index,
                isPreparing = true,
                isPlaying = false,
                errorMessage = null,
            )
        }

        runCatching {
            MediaPlayer().also { player ->
                mediaPlayer = player
                player.setDataSource(context, Uri.parse(contentUri))
                player.setOnPreparedListener {
                    it.start()
                    _state.update { state -> state.copy(isPreparing = false, isPlaying = true) }
                    Log.i(TAG, "event=playback_started index=${_state.value.selectedIndex}")
                }
                player.setOnCompletionListener {
                    logDebug("event=playback_completed index=${_state.value.selectedIndex}")
                    playNext()
                }
                player.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "event=playback_failed stage=player what=$what extra=$extra")
                    releasePlayer()
                    _state.update {
                        it.copy(errorMessage = "Unable to play ${track.title}")
                    }
                    true
                }
                player.prepareAsync()
            }
        }.onFailure { error ->
            Log.e(TAG, "event=playback_failed stage=prepare index=$index", error)
            releasePlayer()
            _state.update { it.copy(errorMessage = error.message ?: "Unable to play this song") }
        }
    }

    override fun releasePlayer() {
        releasePlayer(updateState = true)
    }

    private fun releasePlayer(updateState: Boolean) {
        val hadPlayer = mediaPlayer != null
        mediaPlayer?.runCatching {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            release()
        }
        mediaPlayer = null
        if (updateState) {
            _state.update { it.copy(isPreparing = false, isPlaying = false) }
        }
        if (hadPlayer) logDebug("event=player_released update_state=$updateState")
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "MiniIviMedia"
        const val MEDIA_CHANGE_DEBOUNCE_MILLIS = 300L
    }
}
