package com.android.car.systemui.data.repository

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.android.car.systemui.wallpaper.CarWallpaperService
import java.lang.reflect.Method

class AndroidStartupRepository : StartupRepository {
    override fun initialize(context: Context) {
        if (context.getSystemService(Context.WALLPAPER_SERVICE) == null) return
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
        }.onFailure { error ->
            Log.i(TAG, "Wallpaper is already configured or unavailable", error)
        }
    }

    private companion object {
        const val TAG = "CarSystemUI-Startup"
    }
}
