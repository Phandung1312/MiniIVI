package com.android.car.systemui.framework;

import android.app.ActivityManager;
import android.hardware.display.DisplayManager;

/**
 * Runtime bridge for SystemUI-only framework calls. It is compiled against the
 * adjacent compile-only stubs and executes against the matching system image APIs.
 */
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
