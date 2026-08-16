package com.android.car.launcher.feature.media.data.datasource

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.android.car.launcher.feature.media.domain.model.MediaTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class MediaStoreTrack(
    val track: MediaTrack,
    val contentUri: String,
)

class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun querySongs(): List<MediaStoreTrack> {
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
                    val uri = ContentUris.withAppendedId(collection, id).toString()

                    add(
                        MediaStoreTrack(
                            track = MediaTrack(
                                id = id,
                                title = title,
                                artist = artist,
                                durationMillis = cursor.getLong(durationColumn),
                            ),
                            contentUri = uri,
                        ),
                    )
                }
            }
        }
    }
}
