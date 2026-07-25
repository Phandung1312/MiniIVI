package com.android.car.systemui.navigation;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.WallpaperManager;
import android.os.Process;

import java.lang.reflect.Method;

import com.android.car.systemui.wallpaper.CarWallpaperService;

public final class BootReceiver extends BroadcastReceiver {
    private static final int PER_USER_RANGE = 100000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Process.myUid() / PER_USER_RANGE != 0) {
            return;
        }

        context.startService(new Intent(context, BottomNavigationService.class));
        if (context.getSystemService(Context.WALLPAPER_SERVICE) == null) {
            return;
        }

        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            Method setWallpaperComponent = WallpaperManager.class.getMethod(
                    "setWallpaperComponent", ComponentName.class);
            setWallpaperComponent.invoke(
                    wallpaperManager, new ComponentName(context, CarWallpaperService.class));
        } catch (ReflectiveOperationException | SecurityException ignored) {
            // The platform image may already have selected the wallpaper for this user.
        }
    }
}
