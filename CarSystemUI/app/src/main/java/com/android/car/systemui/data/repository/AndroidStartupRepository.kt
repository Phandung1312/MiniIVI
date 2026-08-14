package com.android.car.systemui.data.repository

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.android.car.systemui.wallpaper.CarWallpaperService
import java.lang.reflect.Method

class AndroidStartupRepository : StartupRepository {
    override fun initialize(context: Context) {
        if (context.getSystemService(Context.WALLPAPER_SERVICE) == null) {
            Log.w(TAG, "event=startup_step_skipped step=wallpaper reason=service_unavailable")
            return
        }
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val setWallpaperComponent: Method = WallpaperManager::class.java.getMethod(
                "setWallpaperComponent",
                ComponentName::class.java,
            )
            setWallpaperComponent.invoke(
                wallpaperManager,
                ComponentName(context, CarWallpaperService::class.java),
            )
            Log.i(TAG, "event=startup_step_completed step=wallpaper")
        }.onFailure { error ->
            Log.i(TAG, "event=startup_step_unchanged step=wallpaper", error)
        }
    }

    private companion object {
        const val TAG = "MiniIviStartup"
    }
}
