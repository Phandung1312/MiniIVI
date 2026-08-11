package com.android.car.systemui.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CarSystemUiColors = lightColorScheme(
    primary = Color(0xFF8066D8),
    secondary = Color(0xFFDDA8CF),
    surface = Color(0x99F4F1F7),
    surfaceVariant = Color(0xB5F8F5FA),
    background = Color(0xFFC9C5D2),
    onPrimary = Color.White,
    onSecondary = Color(0xFF25222B),
    onSurface = Color(0xFF25222B),
    onBackground = Color(0xFF25222B),
    outline = Color(0x997B7483),
)

@Composable
fun CarSystemUiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CarSystemUiColors, content = content)
}
