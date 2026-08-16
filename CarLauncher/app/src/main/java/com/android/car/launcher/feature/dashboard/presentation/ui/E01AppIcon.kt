package com.android.car.launcher.feature.dashboard.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.android.car.launcher.feature.dashboard.presentation.model.HomeAppIcon

private val E01Shape = RoundedCornerShape(28.dp)
private val E01GlyphColor = Color(0xFFF8FAFF)
private val E01GlyphShadow = Color.Black.copy(alpha = 0.24f)

@Composable
internal fun E01AppIcon(
    icon: HomeAppIcon,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = E01Shape,
                ambientColor = icon.shadowColor().copy(alpha = 0.34f),
                spotColor = icon.shadowColor().copy(alpha = 0.48f),
            )
            .clip(E01Shape)
            .background(icon.backgroundBrush())
            .border(1.dp, Color.White.copy(alpha = 0.22f), E01Shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.12f),
                        0.46f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.10f),
                    ),
                ),
        )
        E01Glyph(icon)
    }
}

@Composable
private fun E01Glyph(icon: HomeAppIcon) {
    when (icon) {
        HomeAppIcon.Weather -> WeatherGlyph()
        HomeAppIcon.Maps -> MapsGlyph()
        else -> StandardGlyph(icon.imageVector())
    }
}

@Composable
private fun StandardGlyph(imageVector: ImageVector) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = E01GlyphShadow,
            modifier = Modifier.size(66.dp).offset(y = 2.dp),
        )
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = E01GlyphColor,
            modifier = Modifier.size(64.dp),
        )
    }
}

@Composable
private fun WeatherGlyph() {
    Box(modifier = Modifier.size(82.dp)) {
        Icon(
            imageVector = Icons.Rounded.WbSunny,
            contentDescription = null,
            tint = Color(0xFFFFCA3A),
            modifier = Modifier
                .size(47.dp)
                .align(Alignment.TopEnd),
        )
        Icon(
            imageVector = Icons.Rounded.Cloud,
            contentDescription = null,
            tint = E01GlyphShadow,
            modifier = Modifier
                .size(69.dp)
                .align(Alignment.BottomStart)
                .offset(x = 2.dp, y = 2.dp),
        )
        Icon(
            imageVector = Icons.Rounded.Cloud,
            contentDescription = null,
            tint = E01GlyphColor,
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun MapsGlyph() {
    Box(modifier = Modifier.size(82.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Rounded.Map,
            contentDescription = null,
            tint = E01GlyphColor.copy(alpha = 0.90f),
            modifier = Modifier.size(76.dp).offset(y = 6.dp),
        )
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = Color(0xFFFF5A4F),
            modifier = Modifier.size(49.dp).offset(y = (-8).dp),
        )
    }
}

private fun HomeAppIcon.imageVector(): ImageVector = when (this) {
    HomeAppIcon.Media -> Icons.Rounded.PlayCircle
    HomeAppIcon.Video -> Icons.Rounded.Videocam
    HomeAppIcon.Weather -> Icons.Rounded.Cloud
    HomeAppIcon.Browser -> Icons.Rounded.Language
    HomeAppIcon.Bluetooth -> Icons.Rounded.Bluetooth
    HomeAppIcon.Maps -> Icons.Rounded.Map
    HomeAppIcon.Phone -> Icons.Rounded.Phone
    HomeAppIcon.Settings -> Icons.Rounded.Settings
}

private fun HomeAppIcon.backgroundBrush(): Brush = when (this) {
    HomeAppIcon.Media -> appIconBrush(Color(0xFFFF9F1C), Color(0xFFFF4B2B))
    HomeAppIcon.Video -> appIconBrush(Color(0xFFFF3B30), Color(0xFFB0004B))
    HomeAppIcon.Weather -> appIconBrush(Color(0xFF19C7F3), Color(0xFF1262D1))
    HomeAppIcon.Browser -> appIconBrush(Color(0xFF14C8F0), Color(0xFF075CC7))
    HomeAppIcon.Bluetooth -> appIconBrush(Color(0xFF149BFF), Color(0xFF0754C9))
    HomeAppIcon.Maps -> appIconBrush(Color(0xFF16C9B0), Color(0xFF087E75))
    HomeAppIcon.Phone -> appIconBrush(Color(0xFF45D33C), Color(0xFF078B37))
    HomeAppIcon.Settings -> appIconBrush(Color(0xFF9255F7), Color(0xFF4931B8))
}

private fun appIconBrush(top: Color, bottom: Color): Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0f to top,
        0.58f to bottom,
        1f to bottom.darken(0.76f),
    ),
)

private fun Color.darken(factor: Float): Color = Color(
    red = red * factor,
    green = green * factor,
    blue = blue * factor,
    alpha = alpha,
)

private fun HomeAppIcon.shadowColor(): Color = when (this) {
    HomeAppIcon.Media -> Color(0xFFFF6D2D)
    HomeAppIcon.Video -> Color(0xFFE3004F)
    HomeAppIcon.Weather -> Color(0xFF168CE5)
    HomeAppIcon.Browser -> Color(0xFF0A86DD)
    HomeAppIcon.Bluetooth -> Color(0xFF0878EB)
    HomeAppIcon.Maps -> Color(0xFF0AA893)
    HomeAppIcon.Phone -> Color(0xFF16A63E)
    HomeAppIcon.Settings -> Color(0xFF6841D6)
}
