package com.android.car.launcher.feature.media

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.car.launcher.R
import com.android.car.launcher.core.lifecycle.LifeCycleLogger
import com.android.car.launcher.core.ui.WallpaperBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaActivity : LifeCycleLogger() {
    private val viewModel by viewModels<MediaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                MediaNavigation(
                    state = state,
                    onBack = ::finish,
                    onLoad = viewModel::loadSongs,
                    onPlayPause = viewModel::onPlayPause,
                    onNext = viewModel::onNext,
                    onPrevious = viewModel::onPrevious,
                    onTrackSelected = viewModel::onTrackSelected,
                )
            }
        }

        viewModel.loadSongs()
    }
}

private object MediaRoute {
    const val PLAYER = "player"
    const val LIBRARY = "library"
}

@Composable
private fun MediaNavigation(
    state: MediaState,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTrackSelected: (Int) -> Unit,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MediaRoute.PLAYER) {
        composable(MediaRoute.PLAYER) {
            PlayerScreen(
                state = state,
                onBack = onBack,
                onLoad = onLoad,
                onOpenLibrary = { navController.navigate(MediaRoute.LIBRARY) },
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
            )
        }
        composable(MediaRoute.LIBRARY) {
            LibraryScreen(
                state = state,
                navController = navController,
                onLoad = onLoad,
                onTrackSelected = onTrackSelected,
            )
        }
    }
}

@Composable
private fun PlayerScreen(
    state: MediaState,
    onBack: () -> Unit,
    onLoad: () -> Unit,
    onOpenLibrary: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    WallpaperBackground {
        MediaBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 28.dp),
        ) {
            FeatureHeader(
                title = stringResource(R.string.media),
                onBack = onBack,
                state = state,
                onLoad = onLoad,
            )

            if (state.tracks.isEmpty()) {
                EmptyMediaContent(
                    message = state.errorMessage ?: stringResource(R.string.no_songs),
                )
                return@Column
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xF2332730), Color(0xEE1B202A)),
                        ),
                    )
                    .border(1.dp, Color(0x55FF8AA0), RoundedCornerShape(28.dp))
                    .padding(30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF9A4058), Color(0xFF4E2A3D)),
                            ),
                            RoundedCornerShape(36.dp),
                        )
                        .border(1.dp, Color(0x66FFB0BE), RoundedCornerShape(36.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = Color(0xFFFFA0B1),
                        modifier = Modifier.size(104.dp),
                    )
                }
                Spacer(Modifier.width(36.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.now_playing).uppercase(),
                        color = Color(0xFFFF91A5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        state.currentTrack?.title.orEmpty(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.currentTrack?.artist.orEmpty(),
                        color = Color(0xFFC1CAD4),
                        fontSize = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    state.errorMessage?.let {
                        Text(
                            it,
                            color = Color(0xFFFF9CAF),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(26.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaybackButton(
                            icon = {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    stringResource(R.string.previous),
                                    modifier = Modifier.size(34.dp),
                                )
                            },
                            onClick = onPrevious,
                            enabled = !state.isPreparing,
                        )
                        IconButton(
                            onClick = onPlayPause,
                            enabled = !state.isPreparing,
                            modifier = Modifier
                                .size(88.dp)
                                .background(Color(0xFFFF6D82), CircleShape)
                                .border(1.dp, Color(0x99FFC1CC), CircleShape),
                        ) {
                            if (state.isPreparing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                )
                            } else {
                                Icon(
                                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (state.isPlaying) stringResource(R.string.pause)
                                    else stringResource(R.string.play),
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp),
                                )
                            }
                        }
                        PlaybackButton(
                            icon = {
                                Icon(
                                    Icons.Default.SkipNext,
                                    stringResource(R.string.next),
                                    modifier = Modifier.size(34.dp),
                                )
                            },
                            onClick = onNext,
                            enabled = !state.isPreparing,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    MediaActionButton(
                        text = stringResource(R.string.open_library),
                        onClick = onOpenLibrary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    state: MediaState,
    navController: NavHostController,
    onLoad: () -> Unit,
    onTrackSelected: (Int) -> Unit,
) {
    WallpaperBackground {
        MediaBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 28.dp),
        ) {
            FeatureHeader(
                title = stringResource(R.string.library),
                onBack = { navController.popBackStack() },
                state = state,
                onLoad = onLoad,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.all_songs),
                    color = Color(0xFFE8EDF3),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF3D2933),
                    border = BorderStroke(1.dp, Color(0x55FF8AA0)),
                ) {
                    Text(
                        state.tracks.size.toString(),
                        color = Color(0xFFFFA0B1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track ->
                    val selected = index == state.selectedIndex
                    val shape = RoundedCornerShape(20.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) Color(0xF24B303B) else Color(0xE61C242D),
                                shape,
                            )
                            .border(
                                1.dp,
                                if (selected) Color(0x77FF8AA0) else Color(0x2EADBDCC),
                                shape,
                            )
                            .clickable {
                                onTrackSelected(index)
                                navController.popBackStack()
                            }
                            .heightIn(min = 88.dp)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(
                                    if (selected) Color(0xFF7A3345) else Color(0xFF382A34),
                                    RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = Color(0xFFFF91A5),
                                modifier = Modifier.size(29.dp),
                            )
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 18.dp),
                        ) {
                            Text(
                                track.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                track.artist,
                                color = Color(0xFFAEB9C5),
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selected) {
                            Text(
                                stringResource(
                                    if (state.isPlaying) R.string.playing else R.string.selected,
                                ),
                                color = Color(0xFFFFA0B1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(0xFF6B3142), CircleShape)
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                            )
                        } else {
                            Text(
                                formatDuration(track.durationMillis),
                                color = Color(0xFF8F9CAA),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHeader(
    title: String,
    onBack: () -> Unit,
    state: MediaState,
    onLoad: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xCC2D2830), CircleShape)
                .border(1.dp, Color(0x44FF8AA0), CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.back),
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(18.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        MediaActionButton(
            text = stringResource(R.string.load_music),
            loadingText = stringResource(R.string.loading_music),
            loading = state.isLoading,
            onClick = onLoad,
        )
    }
}

@Composable
private fun EmptyMediaContent(
    message: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFF4E2A3D), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = Color(0xFFFF91A5),
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            message,
            color = Color(0xFFC1CAD4),
            fontSize = 19.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 520.dp),
        )
    }
}

@Composable
private fun MediaBackdrop() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x991B1118), Color(0xDB0C0E14)),
                ),
            ),
    )
}

@Composable
private fun PlaybackButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(68.dp)
            .background(Color(0xFF332C35), CircleShape)
            .border(1.dp, Color(0x44FF9BAC), CircleShape),
    ) {
        icon()
    }
}

@Composable
private fun MediaActionButton(
    text: String,
    onClick: () -> Unit,
    loading: Boolean = false,
    loadingText: String = text,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.height(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF6D82),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF7A4050),
            disabledContentColor = Color(0xFFFFD6DD),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFFFFD6DD),
                strokeWidth = 2.5.dp,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            if (loading) loadingText else text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis.coerceAtLeast(0L) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
