package com.android.car.systemui.wallpaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.android.car.systemui.R;

public final class CarWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new CarWallpaperEngine();
    }

    private final class CarWallpaperEngine extends Engine {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private Bitmap wallpaper;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            wallpaper = BitmapFactory.decodeResource(getResources(), R.drawable.wall_paper);
            drawWallpaper();
        }

        @Override
        public void onSurfaceChanged(
                SurfaceHolder surfaceHolder, int format, int width, int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);
            drawWallpaper();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                drawWallpaper();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder);
        }

        @Override
        public void onDestroy() {
            if (wallpaper != null) {
                wallpaper.recycle();
                wallpaper = null;
            }
            super.onDestroy();
        }

        private void drawWallpaper() {
            if (wallpaper == null) return;

            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;

                int width = canvas.getWidth();
                int height = canvas.getHeight();
                float scale = Math.max(
                        (float) width / wallpaper.getWidth(),
                        (float) height / wallpaper.getHeight());
                int scaledWidth = Math.round(wallpaper.getWidth() * scale);
                int scaledHeight = Math.round(wallpaper.getHeight() * scale);
                int left = (width - scaledWidth) / 2;
                int top = (height - scaledHeight) / 2;
                canvas.drawBitmap(wallpaper, null,
                        new Rect(left, top, left + scaledWidth, top + scaledHeight), paint);
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
