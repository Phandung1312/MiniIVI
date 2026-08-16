package com.android.car.launcher.feature.media.presentation.ui

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
import com.android.car.launcher.R
import com.android.car.launcher.core.ui.MiniIviColors
import com.android.car.launcher.core.ui.WallpaperBackground
import com.android.car.launcher.feature.media.domain.model.MediaState

@Composable
internal fun PlayerScreen(
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
                            listOf(MiniIviColors.SurfaceRaised, MiniIviColors.Surface),
                        ),
                    )
                    .border(1.dp, MiniIviColors.Border, RoundedCornerShape(28.dp))
                    .padding(30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(MiniIviColors.Secondary, MiniIviColors.Primary),
                            ),
                            RoundedCornerShape(36.dp),
                        )
                        .border(1.dp, MiniIviColors.Border, RoundedCornerShape(36.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = Color.White,
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
                        color = MiniIviColors.Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        state.currentTrack?.title.orEmpty(),
                        color = MiniIviColors.TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.currentTrack?.artist.orEmpty(),
                        color = MiniIviColors.TextSecondary,
                        fontSize = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    state.errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
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
                                .background(MiniIviColors.Primary, CircleShape)
                                .border(1.dp, MiniIviColors.Border, CircleShape),
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
internal fun LibraryScreen(
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
                    color = MiniIviColors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = MiniIviColors.Primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MiniIviColors.Border),
                ) {
                    Text(
                        state.tracks.size.toString(),
                        color = MiniIviColors.Primary,
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
                                if (selected) MiniIviColors.Primary.copy(alpha = 0.13f)
                                else MiniIviColors.Surface,
                                shape,
                            )
                            .border(
                                1.dp,
                                if (selected) MiniIviColors.Primary.copy(alpha = 0.45f)
                                else MiniIviColors.Border,
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
                                    if (selected) MiniIviColors.Primary.copy(alpha = 0.22f)
                                    else MiniIviColors.Secondary.copy(alpha = 0.22f),
                                    RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = MiniIviColors.Primary,
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
                                color = MiniIviColors.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                track.artist,
                                color = MiniIviColors.TextSecondary,
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
                                color = MiniIviColors.Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(MiniIviColors.Primary.copy(alpha = 0.14f), CircleShape)
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                            )
                        } else {
                            Text(
                                formatDuration(track.durationMillis),
                                color = MiniIviColors.TextSecondary,
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
                .background(MiniIviColors.SurfaceRaised, CircleShape)
                .border(1.dp, MiniIviColors.Border, CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.back),
                tint = MiniIviColors.TextPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(18.dp))
        Text(
            title,
            color = MiniIviColors.TextPrimary,
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
                .background(MiniIviColors.Secondary.copy(alpha = 0.24f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = MiniIviColors.Primary,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            message,
            color = MiniIviColors.TextSecondary,
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
                    listOf(Color.Transparent, MiniIviColors.Primary.copy(alpha = 0.04f)),
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
            .background(MiniIviColors.SurfaceRaised, CircleShape)
            .border(1.dp, MiniIviColors.Border, CircleShape),
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
            containerColor = MiniIviColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = MiniIviColors.Primary.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.72f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
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
