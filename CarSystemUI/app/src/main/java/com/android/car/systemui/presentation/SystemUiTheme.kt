package com.android.car.systemui.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CarSystemUiColors = darkColorScheme(
    primary = Color(0xFF5EA3FF),
    secondary = Color(0xFF68D391),
    surface = Color(0xFF1C222A),
    surfaceVariant = Color(0xFF29313C),
    background = Color(0xFF090C10),
    onPrimary = Color.White,
    onSurface = Color.White,
    onBackground = Color.White,
)

@Composable
fun CarSystemUiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CarSystemUiColors, content = content)
}
