package com.android.car.systemui.boot

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import com.miniivi.bootbrand.BootBrandSpec
import kotlin.math.min

internal class IviShimmerView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }
    private val segments = BootBrandSpec.copyStrokeSegments()
    private var shimmerProgress = 0.0f
    private val animator = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
        duration = BootBrandSpec.SHIMMER_DURATION_MILLIS
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            shimmerProgress = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(BootBrandSpec.BACKGROUND_COLOR)

        val scale = min(
            width / BootBrandSpec.CANVAS_WIDTH.toFloat(),
            height / BootBrandSpec.CANVAS_HEIGHT.toFloat(),
        )
        val offsetX = (width - (BootBrandSpec.DESIGN_WIDTH * scale)) / 2.0f
        val offsetY = (height - (BootBrandSpec.DESIGN_HEIGHT * scale)) / 2.0f

        val checkpoint = canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        paint.shader = null
        paint.strokeWidth = BootBrandSpec.SHADOW_STROKE_WIDTH
        paint.color = BootBrandSpec.PRIMARY_COLOR
        paint.alpha = 31
        drawSegments(canvas, xOffset = 10.0f)

        paint.strokeWidth = BootBrandSpec.TEXT_STROKE_WIDTH
        paint.color = BootBrandSpec.TEXT_COLOR
        paint.alpha = 255
        drawSegments(canvas)

        val shimmerCenter = BootBrandSpec.shimmerCenter(shimmerProgress)
        paint.strokeWidth = BootBrandSpec.SHADOW_STROKE_WIDTH
        paint.alpha = 148
        paint.shader = shimmerShader(shimmerCenter, glow = true)
        drawSegments(canvas)

        paint.strokeWidth = BootBrandSpec.TEXT_STROKE_WIDTH + 2.0f
        paint.alpha = 255
        paint.shader = shimmerShader(shimmerCenter, glow = false)
        drawSegments(canvas)

        paint.shader = null
        canvas.restoreToCount(checkpoint)
    }

    private fun shimmerShader(center: Float, glow: Boolean): Shader {
        val halfWidth = BootBrandSpec.SHIMMER_HALF_WIDTH
        return LinearGradient(
            center - halfWidth,
            0.0f,
            center + halfWidth,
            0.0f,
            intArrayOf(
                withAlpha(BootBrandSpec.PRIMARY_COLOR, 0),
                withAlpha(BootBrandSpec.PRIMARY_COLOR, if (glow) 150 else 235),
                withAlpha(BootBrandSpec.SECONDARY_COLOR, if (glow) 135 else 245),
                withAlpha(BootBrandSpec.PRIMARY_COLOR, if (glow) 150 else 235),
                withAlpha(BootBrandSpec.SECONDARY_COLOR, 0),
            ),
            floatArrayOf(0.0f, 0.30f, 0.50f, 0.70f, 1.0f),
            Shader.TileMode.CLAMP,
        )
    }

    private fun drawSegments(canvas: Canvas, xOffset: Float = 0.0f) {
        var index = 0
        while (index < segments.size) {
            canvas.drawLine(
                segments[index] + xOffset,
                segments[index + 1],
                segments[index + 2] + xOffset,
                segments[index + 3],
                paint,
            )
            index += 4
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}
