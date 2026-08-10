package com.miniivi.car.service.framework;

import android.app.ActivityManager;
import android.hardware.display.DisplayManager;

/** Runtime bridge compiled against compile-only framework declarations. */
public final class FrameworkPlatformApi {
    private FrameworkPlatformApi() {}

    public static int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    public static float getBrightness(DisplayManager manager, int displayId) {
        return manager.getBrightness(displayId);
    }

    public static void setBrightness(DisplayManager manager, int displayId, float brightness) {
        manager.setBrightness(displayId, brightness);
    }
}
