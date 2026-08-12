package com.android.car.launcher.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.car.launcher.R

object MiniIviColors {
    val Background = Color(0xFF201A29)
    val BackgroundLight = Color(0xFF15121C)
    val BackgroundGlow = Color(0xFF2A2137)
    val Surface = Color(0x80594B6D)
    val SurfaceRaised = Color(0xA06F5E88)
    internal val GlassBase = Color(0x56322940)
    val Primary = Color(0xFFA98CF5)
    val Secondary = Color(0xFFF0A8D8)
    val TextPrimary = Color(0xFFF8F4FC)
    val TextSecondary = Color(0xFFCFC6D8)
    val Border = Color.White.copy(alpha = 0.18f)
    val GlassHighlight = Color.White.copy(alpha = 0.12f)
    val GlassShadow = Color(0x66000000)
}

internal val MiniIviBackgroundBrush = Brush.linearGradient(
    listOf(
        MiniIviColors.BackgroundLight,
        MiniIviColors.BackgroundGlow,
        MiniIviColors.Background,
    ),
)

private val MiniIviGlassBrush = Brush.linearGradient(
    listOf(
        MiniIviColors.Surface,
        MiniIviColors.GlassBase,
    ),
)

private val MiniIviColorScheme = darkColorScheme(
    primary = MiniIviColors.Primary,
    secondary = MiniIviColors.Secondary,
    background = MiniIviColors.Background,
    surface = MiniIviColors.Surface,
    surfaceVariant = MiniIviColors.SurfaceRaised,
    onPrimary = Color.White,
    onSecondary = Color(0xFF201A29),
    onBackground = MiniIviColors.TextPrimary,
    onSurface = MiniIviColors.TextPrimary,
    outline = MiniIviColors.Border,
)

@Composable
fun MiniIviTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MiniIviColorScheme) {
        CompositionLocalProvider(
            LocalContentColor provides MiniIviColors.TextPrimary,
            content = content,
        )
    }
}

@Composable
fun MiniIviScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MiniIviBackgroundBrush),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = dimensionResource(R.dimen.navigation_rail_clearance)),
            content = content,
        )
    }
}

@Composable
fun MiniIviCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    color: Color = MiniIviColors.Surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glassModifier = modifier
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = MiniIviColors.GlassShadow,
            spotColor = MiniIviColors.GlassShadow,
        )
        .then(
            if (color == MiniIviColors.Surface) {
                Modifier.background(MiniIviGlassBrush, shape)
            } else {
                Modifier.background(color, shape)
            },
        )
        .border(BorderStroke(1.dp, MiniIviColors.Border), shape)
        .clip(shape)
        .let { base -> if (onClick == null) base else base.clickable(onClick = onClick) }

    Box(modifier = glassModifier, content = { content() })
}
