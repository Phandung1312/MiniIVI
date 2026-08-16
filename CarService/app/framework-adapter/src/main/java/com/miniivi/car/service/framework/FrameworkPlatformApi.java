package com.miniivi.car.service.framework;

import android.app.ActivityManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.UserHandle;

/** Direct-call bridge for hidden framework APIs unavailable in the public SDK jar. */
public final class FrameworkPlatformApi {
    private FrameworkPlatformApi() {}

    public static int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    public static Context createContextAsUser(Context context, UserHandle user, int flags) {
        return context.createContextAsUser(user, flags);
    }

    public static void goToSleep(PowerManager manager, long time) {
        manager.goToSleep(time);
    }

    public static int getMinimumScreenBrightnessSetting(PowerManager manager) {
        return manager.getMinimumScreenBrightnessSetting();
    }

    public static int getMaximumScreenBrightnessSetting(PowerManager manager) {
        return manager.getMaximumScreenBrightnessSetting();
    }
}
