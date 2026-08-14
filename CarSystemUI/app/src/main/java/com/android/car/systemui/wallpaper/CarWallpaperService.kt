package com.android.car.systemui.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.android.car.systemui.R
import kotlin.math.max
import kotlin.math.roundToInt

class CarWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        Log.i(TAG, "event=engine_created feature=wallpaper")
        return CarWallpaperEngine()
    }

    private inner class CarWallpaperEngine : Engine() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var wallpaper: Bitmap? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            wallpaper = BitmapFactory.decodeResource(resources, R.drawable.wall_paper)
            Log.i(
                TAG,
                "event=wallpaper_loaded available=${wallpaper != null}",
            )
            drawWallpaper()
        }

        override fun onSurfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(surfaceHolder, format, width, height)
            drawWallpaper()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) drawWallpaper()
        }

        override fun onDestroy() {
            wallpaper?.recycle()
            wallpaper = null
            Log.i(TAG, "event=engine_destroyed feature=wallpaper")
            super.onDestroy()
        }

        private fun drawWallpaper() {
            val bitmap = wallpaper ?: return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                val scale = max(
                    canvas.width.toFloat() / bitmap.width,
                    canvas.height.toFloat() / bitmap.height,
                )
                val scaledWidth = (bitmap.width * scale).roundToInt()
                val scaledHeight = (bitmap.height * scale).roundToInt()
                val left = (canvas.width - scaledWidth) / 2
                val top = (canvas.height - scaledHeight) / 2
                canvas.drawBitmap(
                    bitmap,
                    null,
                    Rect(left, top, left + scaledWidth, top + scaledHeight),
                    paint,
                )
            } finally {
                canvas?.let(holder::unlockCanvasAndPost)
            }
        }
    }

    private companion object {
        const val TAG = "MiniIviWallpaper"
    }
}
