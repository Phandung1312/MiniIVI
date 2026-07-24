package com.android.car.launcher.feature.media

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.car.launcher.R
import com.android.car.launcher.core.lifecycle.LifeCycleLogger
import com.android.car.launcher.core.ui.LoadingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaActivity : LifeCycleLogger() {
    private val model by viewModels<MediaModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by model.state.collectAsState()
                MediaNavigation(
                    state = state,
                    onBack = ::finish,
                    onLoad = model::loadSongs,
                    onPlayPause = model::onPlayPause,
                    onNext = model::onNext,
                    onPrevious = model::onPrevious,
                    onTrackSelected = model::onTrackSelected,
                )
            }
        }

        model.loadSongs()
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
    Surface(color = Color(0xFF0B0D10), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(28.dp)) {
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

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .background(Color(0xFF7A3345), RoundedCornerShape(30.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = Color(0xFFFF8498),
                        modifier = Modifier.size(68.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    state.currentTrack?.title.orEmpty(),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    state.currentTrack?.artist.orEmpty(),
                    color = Color(0xFFB8C0CC),
                    fontSize = 17.sp,
                )
                state.errorMessage?.let {
                    Text(it, color = Color(0xFFFF8498), modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = !state.isPreparing,
                        modifier = Modifier.size(58.dp),
                    ) {
                        Icon(Icons.Default.SkipPrevious, stringResource(R.string.previous), tint = Color.White)
                    }
                    IconButton(
                        onClick = onPlayPause,
                        enabled = !state.isPreparing,
                        modifier = Modifier
                            .size(58.dp)
                            .background(Color(0xFFFF6D82), CircleShape),
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (state.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = !state.isPreparing,
                        modifier = Modifier.size(58.dp),
                    ) {
                        Icon(Icons.Default.SkipNext, stringResource(R.string.next), tint = Color.White)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = onOpenLibrary) {
                    Text(stringResource(R.string.open_library))
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
    Surface(color = Color(0xFF0B0D10), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(28.dp)) {
            FeatureHeader(
                title = stringResource(R.string.library),
                onBack = { navController.popBackStack() },
                state = state,
                onLoad = onLoad,
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (index == state.selectedIndex) Color(0xFF303B48)
                                else Color(0xFF1D2228),
                                RoundedCornerShape(16.dp),
                            )
                            .clickable {
                                onTrackSelected(index)
                                navController.popBackStack()
                            }
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.LibraryMusic, null, tint = Color(0xFFFF8498))
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(track.artist, color = Color(0xFFB8C0CC))
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
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
        }
        Text(
            title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        LoadingActionButton(
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
        Text(message, color = Color(0xFFB8C0CC), fontSize = 18.sp)
    }
}
