package com.android.car.launcher.feature.dashboard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.car.launcher.R
import com.android.car.launcher.core.ui.WallpaperBackground
import com.android.car.launcher.feature.dashboard.HomeApp
import com.android.car.launcher.feature.dashboard.HomeAppIcon
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun HomeScreen(
    apps: List<HomeApp>,
    onAppClick: (HomeApp) -> Unit,
) {
    WallpaperBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x8A070A12), Color(0xD90A0C12)),
                    ),
                ),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val isWide = maxWidth > maxHeight
                val horizontalPadding = if (maxWidth >= 900.dp) 48.dp else 24.dp
                val heroHeight = when {
                    maxHeight < 700.dp -> 260.dp
                    isWide -> 340.dp
                    else -> 360.dp
                }
                val gridHeight = when {
                    maxHeight < 700.dp -> 250.dp
                    isWide -> 280.dp
                    else -> 400.dp
                }
                val tileWidth = if (maxWidth >= 900.dp) 196.dp else 164.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                ) {
                    HomeHero(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight),
                        largeClock = isWide,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.home_applications),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(items = apps, key = HomeApp::id) { app ->
                            HomeAppTile(
                                app = app,
                                modifier = Modifier
                                    .width(tileWidth)
                                    .fillMaxHeight(),
                                onClick = { onAppClick(app) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHero(
    modifier: Modifier = Modifier,
    largeClock: Boolean,
) {
    val clock = rememberHomeClock()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF121925)),
    ) {
        Image(
            painter = painterResource(R.drawable.home_banner_wide),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(horizontalBias = 1f, verticalBias = .75f),
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xC70A0D15),
                            Color(0x7A0A0D15),
                            Color(0x120A0D15),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x58090D15)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(28.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White.copy(alpha = .78f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_greeting),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = clock.time,
                    color = Color.White,
                    fontSize = if (largeClock) 58.sp else 50.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = clock.date,
                    color = Color.White.copy(alpha = .78f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeAppTile(
    app: HomeApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xD9212731))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(app.accentColor.copy(alpha = .18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(app.icon, app.accentColor)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(app.titleRes),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppIcon(icon: HomeAppIcon, color: Color) {
    val imageVector = when (icon) {
        HomeAppIcon.Media,
        HomeAppIcon.YouTube -> Icons.Rounded.PlayCircle
        HomeAppIcon.Bluetooth -> Icons.Rounded.Bluetooth
        HomeAppIcon.Maps -> Icons.Rounded.Map
        HomeAppIcon.Weather -> Icons.Rounded.Cloud
        HomeAppIcon.Phone -> Icons.Rounded.Phone
        HomeAppIcon.Browser -> Icons.Rounded.Language
        HomeAppIcon.Settings -> Icons.Rounded.Settings
    }
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(32.dp),
    )
}

private data class HomeClock(
    val time: String,
    val date: String,
)

@Composable
private fun rememberHomeClock(): HomeClock {
    val locale = remember { Locale.ENGLISH }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, dd MMMM", locale) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            val millisecondsToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
            delay(millisecondsToNextMinute.coerceAtLeast(1_000L))
            now = LocalDateTime.now()
        }
    }

    return HomeClock(
        time = now.format(timeFormatter),
        date = now.format(dateFormatter),
    )
}
