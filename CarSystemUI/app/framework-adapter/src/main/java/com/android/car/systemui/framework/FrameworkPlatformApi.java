package com.android.car.systemui.framework;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

/**
 * Runtime bridge for SystemUI-only framework calls. It is compiled against the
 * adjacent compile-only stubs and executes against the matching system image APIs.
 */
public final class FrameworkPlatformApi {
    private FrameworkPlatformApi() {}

    public static int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    public static void startActivityAsUser(Context context, Intent intent, UserHandle user) {
        context.startActivityAsUser(intent, user);
    }

}
