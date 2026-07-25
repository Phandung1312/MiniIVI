package com.android.car.launcher.feature.dashboard

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.car.launcher.R
import com.android.car.launcher.core.navigation.AppDestination
import com.android.car.launcher.core.navigation.navigateTo
import com.android.car.launcher.core.ui.WallpaperBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    private val tag = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate")
        setContent {
            MaterialTheme {
                HomeScreen(
                    onBluetoothClick = { navigateTo(AppDestination.Bluetooth) },
                    onMediaClick = { navigateTo(AppDestination.Media) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume")
    }

    override fun onPause() {
        Log.d(tag, "onPause")
        super.onPause()
    }
}

@Composable
private fun HomeScreen(
    onBluetoothClick: () -> Unit,
    onMediaClick: () -> Unit,
) {
    WallpaperBackground {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTile(
                title = stringResource(R.string.bluetooth),
                icon = { BluetoothIcon() },
                onClick = onBluetoothClick,
            )
            AppTile(
                title = stringResource(R.string.media),
                icon = { MediaIcon() },
                onClick = onMediaClick,
            )
        }
    }
}

@Composable
private fun AppTile(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 190.dp)
            .background(Color(0xFF20252B), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun BluetoothIcon() {
    Canvas(Modifier.size(72.dp)) {
        val stroke = 5.dp.toPx()
        val centerX = size.width / 2
        drawLine(Color(0xFF63A8FF), Offset(centerX, 4.dp.toPx()), Offset(centerX, size.height - 4.dp.toPx()), stroke, StrokeCap.Round)
        drawLine(Color(0xFF63A8FF), Offset(centerX, 4.dp.toPx()), Offset(size.width * .75f, size.height * .27f), stroke, StrokeCap.Round)
        drawLine(Color(0xFF63A8FF), Offset(size.width * .75f, size.height * .27f), Offset(size.width * .25f, size.height * .73f), stroke, StrokeCap.Round)
        drawLine(Color(0xFF63A8FF), Offset(size.width * .25f, size.height * .27f), Offset(size.width * .75f, size.height * .73f), stroke, StrokeCap.Round)
        drawLine(Color(0xFF63A8FF), Offset(size.width * .75f, size.height * .73f), Offset(centerX, size.height - 4.dp.toPx()), stroke, StrokeCap.Round)
    }
}

@Composable
private fun MediaIcon() {
    Canvas(Modifier.size(72.dp)) {
        drawCircle(Color(0xFFFB6D79), style = Stroke(width = 5.dp.toPx()))
        val path = Path().apply {
            moveTo(size.width * .42f, size.height * .30f)
            lineTo(size.width * .72f, size.height * .50f)
            lineTo(size.width * .42f, size.height * .70f)
            close()
        }
        drawPath(path, Color(0xFFFB6D79))
    }
}
