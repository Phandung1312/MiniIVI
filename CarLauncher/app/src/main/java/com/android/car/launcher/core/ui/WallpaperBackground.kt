package com.android.car.launcher.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.android.car.launcher.R

@Composable
fun WallpaperBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(MiniIviBackgroundBrush)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = dimensionResource(R.dimen.navigation_rail_clearance)),
            content = content,
        )
    }
}
