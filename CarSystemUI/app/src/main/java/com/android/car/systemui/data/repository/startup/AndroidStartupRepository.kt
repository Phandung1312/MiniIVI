package com.android.car.systemui.data.repository.startup

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.android.car.systemui.domain.repository.StartupRepository
import com.android.car.systemui.wallpaper.CarWallpaperService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.reflect.Method
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidStartupRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) : StartupRepository {
    override fun initialize() {
        if (applicationContext.getSystemService(Context.WALLPAPER_SERVICE) == null) {
            Log.w(TAG, "event=startup_step_skipped step=wallpaper reason=service_unavailable")
            return
        }
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            val setWallpaperComponent: Method = WallpaperManager::class.java.getMethod(
                "setWallpaperComponent",
                ComponentName::class.java,
            )
            setWallpaperComponent.invoke(
                wallpaperManager,
                ComponentName(applicationContext, CarWallpaperService::class.java),
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
