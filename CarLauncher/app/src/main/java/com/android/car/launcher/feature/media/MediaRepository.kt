package com.android.car.launcher.feature.media

import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

interface MediaController {
    val state: StateFlow<MediaState>
    suspend fun loadSongs()
    fun togglePlayback()
    fun playNext()
    fun playPrevious()
}

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaController {
    private val _state = MutableStateFlow(MediaState())
    override val state = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    override suspend fun loadSongs() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val result = runCatching {
            withContext(Dispatchers.IO) { querySongs() }
        }

        result.onSuccess { tracks ->
            releasePlayer()
            _state.update {
                it.copy(
                    tracks = tracks,
                    selectedIndex = 0,
                    isLoading = false,
                    isPreparing = false,
                    isPlaying = false,
                    errorMessage = null,
                )
            }
            Log.d(TAG, "Loaded ${tracks.size} audio files from MediaStore")
        }.onFailure { error ->
            Log.e(TAG, "Unable to load audio from MediaStore", error)
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load audio files",
                )
            }
        }
    }

    private fun querySongs(): List<MediaTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
        )

        return buildList {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(nameColumn).orEmpty()
                    val title = cursor.getString(titleColumn)
                        ?.takeIf(String::isNotBlank)
                        ?: displayName.substringBeforeLast('.')
                    val artist = cursor.getString(artistColumn)
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                        ?: "Unknown artist"

                    add(
                        MediaTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            contentUri = ContentUris.withAppendedId(collection, id).toString(),
                            durationMillis = cursor.getLong(durationColumn),
                        ),
                    )
                }
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
            }

            else -> {
                player.start()
                _state.update { it.copy(isPlaying = true) }
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

    fun play(index: Int) {
        val track = _state.value.tracks.getOrNull(index) ?: return
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
                player.setDataSource(context, android.net.Uri.parse(track.contentUri))
                player.setOnPreparedListener {
                    it.start()
                    _state.update { state -> state.copy(isPreparing = false, isPlaying = true) }
                }
                player.setOnCompletionListener { playNext() }
                player.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    releasePlayer()
                    _state.update {
                        it.copy(errorMessage = "Unable to play ${track.title}")
                    }
                    true
                }
                player.prepareAsync()
            }
        }.onFailure { error ->
            Log.e(TAG, "Unable to play ${track.contentUri}", error)
            releasePlayer()
            _state.update { it.copy(errorMessage = error.message ?: "Unable to play this song") }
        }
    }

    fun releasePlayer(updateState: Boolean = true) {
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
    }

    private companion object {
        const val TAG = "MediaRepository"
    }
}
