package com.android.car.systemui.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Q06: four direct-access controls for seat, display, climate, and audio. */
internal object MiniIviNavigationIcons {
    /** M16: a calm, minimal home destination mark. */
    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "MiniIviHomeM16",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                roundedRect(3.5f, 3.5f, 17f, 17f, 3.2f)
                roundedRect(8.1f, 8.1f, 7.8f, 7.8f, 1.35f)
            }
        }.build()
    }

    /** M03: four app tiles with the same rounded visual language as Q06. */
    val Apps: ImageVector by lazy {
        ImageVector.Builder(
            name = "MiniIviAppsM03",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                roundedRect(4.1f, 4.1f, 6.8f, 6.8f, 1.4f)
                roundedRect(13.1f, 4.1f, 6.8f, 6.8f, 1.4f)
                roundedRect(4.1f, 13.1f, 6.8f, 6.8f, 1.4f)
                roundedRect(13.1f, 13.1f, 6.8f, 6.8f, 1.4f)
            }
        }.build()
    }

    val ControlCenter: ImageVector by lazy {
        ImageVector.Builder(
            name = "MiniIviControlCenterQ06",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.35f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                roundedRect(3f, 3f, 8.9f, 8.9f, 1.5f)
                roundedRect(12.1f, 3f, 8.9f, 8.9f, 1.5f)
                roundedRect(3f, 12.1f, 8.9f, 8.9f, 1.5f)
                roundedRect(12.1f, 12.1f, 8.9f, 8.9f, 1.5f)

                // Driver seat
                moveTo(6f, 5.3f)
                verticalLineTo(7f)
                quadTo(6f, 7.85f, 6.85f, 7.85f)
                horizontalLineTo(8.1f)
                quadTo(8.9f, 7.85f, 8.9f, 8.65f)
                verticalLineTo(9.65f)
                moveTo(6.85f, 7.85f)
                lineTo(5.75f, 9.65f)

                // Display brightness
                circle(16.55f, 7.45f, 1.25f)
                moveTo(16.55f, 4.75f)
                verticalLineTo(5.25f)
                moveTo(16.55f, 9.65f)
                verticalLineTo(10.15f)
                moveTo(13.85f, 7.45f)
                horizontalLineTo(14.35f)
                moveTo(18.75f, 7.45f)
                horizontalLineTo(19.25f)
                moveTo(14.65f, 5.55f)
                lineTo(15f, 5.9f)
                moveTo(18.1f, 9f)
                lineTo(18.45f, 9.35f)
                moveTo(18.1f, 5.9f)
                lineTo(18.45f, 5.55f)
                moveTo(14.65f, 9.35f)
                lineTo(15f, 9f)

                // Cabin fan
                circle(7.45f, 16.55f, 0.65f)
                moveTo(7.45f, 15.9f)
                quadTo(6f, 14.65f, 5.65f, 15.75f)
                quadTo(5.35f, 16.8f, 6.8f, 16.75f)
                moveTo(8.05f, 16.9f)
                quadTo(9.9f, 16.7f, 9.25f, 18f)
                quadTo(8.7f, 18.95f, 7.7f, 17.85f)
                moveTo(7.2f, 17.05f)
                quadTo(6.95f, 18.85f, 5.85f, 18.2f)
                quadTo(4.9f, 17.65f, 6.05f, 16.65f)

                // Audio output
                moveTo(14.65f, 16f)
                horizontalLineTo(16f)
                lineTo(17.65f, 14.75f)
                verticalLineTo(18.35f)
                lineTo(16f, 17.1f)
                horizontalLineTo(14.65f)
                close()
                moveTo(18.5f, 15.65f)
                quadTo(19.5f, 16.55f, 18.5f, 17.45f)
                moveTo(19.3f, 14.85f)
                quadTo(21.2f, 16.55f, 19.3f, 18.25f)
            }
        }.build()
    }
}

private fun PathBuilder.roundedRect(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    moveTo(x + radius, y)
    horizontalLineTo(x + width - radius)
    quadTo(x + width, y, x + width, y + radius)
    verticalLineTo(y + height - radius)
    quadTo(x + width, y + height, x + width - radius, y + height)
    horizontalLineTo(x + radius)
    quadTo(x, y + height, x, y + height - radius)
    verticalLineTo(y + radius)
    quadTo(x, y, x + radius, y)
    close()
}

private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    val control = radius * 0.5522848f
    moveTo(centerX + radius, centerY)
    curveTo(
        centerX + radius,
        centerY + control,
        centerX + control,
        centerY + radius,
        centerX,
        centerY + radius,
    )
    curveTo(
        centerX - control,
        centerY + radius,
        centerX - radius,
        centerY + control,
        centerX - radius,
        centerY,
    )
    curveTo(
        centerX - radius,
        centerY - control,
        centerX - control,
        centerY - radius,
        centerX,
        centerY - radius,
    )
    curveTo(
        centerX + control,
        centerY - radius,
        centerX + radius,
        centerY - control,
        centerX + radius,
        centerY,
    )
    close()
}
