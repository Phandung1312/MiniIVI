package com.android.car.launcher.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object MiniIviColors {
    val Background = Color(0xFFC9C5D2)
    val BackgroundLight = Color(0xFFE2DEE8)
    val BackgroundGlow = Color(0xFFD7D0E3)
    val Surface = Color(0x99F4F1F7)
    val SurfaceRaised = Color(0xB5F8F5FA)
    val Primary = Color(0xFF8066D8)
    val Secondary = Color(0xFFDDA8CF)
    val TextPrimary = Color(0xFF25222B)
    val TextSecondary = Color(0xFF6E6877)
    val Border = Color.White.copy(alpha = 0.76f)
    val GlassHighlight = Color.White.copy(alpha = 0.46f)
    val GlassShadow = Color(0x3D6E6877)
}

private val MiniIviColorScheme = lightColorScheme(
    primary = MiniIviColors.Primary,
    secondary = MiniIviColors.Secondary,
    background = MiniIviColors.Background,
    surface = MiniIviColors.Surface,
    surfaceVariant = MiniIviColors.SurfaceRaised,
    onPrimary = Color.White,
    onSecondary = MiniIviColors.TextPrimary,
    onBackground = MiniIviColors.TextPrimary,
    onSurface = MiniIviColors.TextPrimary,
    outline = Color(0x807B7483),
)

@Composable
fun MiniIviTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MiniIviColorScheme, content = content)
}

@Composable
fun MiniIviScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 104.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        MiniIviColors.BackgroundLight,
                        MiniIviColors.BackgroundGlow,
                        MiniIviColors.Background,
                    ),
                ),
            ),
        content = content,
    )
}

@Composable
fun MiniIviCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    color: Color = MiniIviColors.Surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            color = color,
            border = BorderStroke(1.dp, MiniIviColors.Border),
            shadowElevation = 5.dp,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(cornerRadius),
            color = color,
            border = BorderStroke(1.dp, MiniIviColors.Border),
            shadowElevation = 5.dp,
            onClick = onClick,
            content = content,
        )
    }
}
