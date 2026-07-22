package com.android.car.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HomeActivity : ComponentActivity() {
    private  val TAG = this.javaClass.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HomeScreen(
                    onBluetoothClick = { openBluetooth() },
                    onMediaClick = { openMedia() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "resume excuted")
    }

    override fun onPause() {
        super.onPause()
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }

    private fun openBluetooth() {
        launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun openMedia() {
        val mediaIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_MUSIC)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launch(mediaIntent)
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_SHORT).show()
        }
    }


}

@Composable
private fun HomeScreen(
    onBluetoothClick: () -> Unit,
    onMediaClick: () -> Unit,
) {
    Surface(color = Color(0xFF0B0D10), modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTile(
                title = "Bluetooth",
                icon = { BluetoothIcon() },
                onClick = onBluetoothClick,
            )
            AppTile(
                title = "Media",
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
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * .42f, size.height * .30f)
            lineTo(size.width * .72f, size.height * .50f)
            lineTo(size.width * .42f, size.height * .70f)
            close()
        }
        drawPath(path, Color(0xFFFB6D79))
    }
}
