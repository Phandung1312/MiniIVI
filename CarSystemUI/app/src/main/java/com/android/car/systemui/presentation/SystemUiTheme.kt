package com.android.car.systemui.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

internal val SystemUiGlassSurface = Color(0x80594B6D)
internal val SystemUiGlassRaised = Color(0xA06F5E88)
internal val SystemUiGlassBorder = Color.White.copy(alpha = 0.18f)
internal val SystemUiGlassShadow = Color(0x66000000)
internal val SystemUiRailBrush = Brush.linearGradient(
    listOf(
        Color(0xAA211B2C),
        Color(0x882A2137),
    ),
)

private val SystemUiGlassBrush = Brush.linearGradient(
    listOf(SystemUiGlassSurface, Color(0x56322940)),
)

private val SystemUiGlassRaisedBrush = Brush.linearGradient(
    listOf(
        SystemUiGlassRaised,
        Color(0x704F4165),
    ),
)

private val CarSystemUiColors = darkColorScheme(
    primary = Color(0xFFA98CF5),
    secondary = Color(0xFFF0A8D8),
    surface = SystemUiGlassSurface,
    surfaceVariant = SystemUiGlassRaised,
    background = Color(0xFF201A29),
    onPrimary = Color.White,
    onSecondary = Color(0xFF201A29),
    onSurface = Color(0xFFF8F4FC),
    onBackground = Color(0xFFF8F4FC),
    outline = SystemUiGlassBorder,
)

@Composable
fun CarSystemUiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CarSystemUiColors) {
        CompositionLocalProvider(
            LocalContentColor provides CarSystemUiColors.onSurface,
            content = content,
        )
    }
}

@Composable
internal fun SystemUiGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape,
    raised: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val panelModifier = modifier
        .shadow(
            elevation = if (raised) 8.dp else 6.dp,
            shape = shape,
            ambientColor = SystemUiGlassShadow,
            spotColor = SystemUiGlassShadow,
        )
        .then(
            if (raised) Modifier.background(SystemUiGlassRaisedBrush, shape)
            else Modifier.background(SystemUiGlassBrush, shape),
        )
        .border(1.dp, SystemUiGlassBorder, shape)
        .clip(shape)
        .let { base ->
            if (onClick == null) base else base.clickable(enabled = enabled, onClick = onClick)
        }

    Box(modifier = panelModifier, content = content)
}
