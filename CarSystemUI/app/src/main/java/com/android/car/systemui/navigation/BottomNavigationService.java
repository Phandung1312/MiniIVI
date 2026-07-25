package com.android.car.systemui.navigation;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.android.car.systemui.R;

import java.lang.reflect.Method;

public final class BottomNavigationService extends Service {
    private static final String TAG = "CarSystemUI-Navigation";
    private static final int TYPE_NAVIGATION_BAR = 2019;
    private static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    private static final String CAR_LAUNCHER_PACKAGE = "com.android.car.launcher";
    private static final String APP_LIST_ACTIVITY =
            "com.android.car.launcher.feature.dashboard.HomeActivity";

    private WindowManager windowManager;
    private View navigationBar;

    @Override
    public void onCreate() {
        super.onCreate();
        showNavigationBar();
    }

    private void showNavigationBar() {
        if (navigationBar != null) return;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        navigationBar = LayoutInflater.from(this).inflate(R.layout.bottom_navigation_bar, null);
        navigationBar.findViewById(R.id.nav_home).setOnClickListener(
                view -> injectKey(KeyEvent.KEYCODE_HOME));
        navigationBar.findViewById(R.id.nav_settings).setOnClickListener(
                view -> openSettings());
        navigationBar.findViewById(R.id.nav_app_list).setOnClickListener(
                view -> showAppList());

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.navigation_bar_height),
                TYPE_NAVIGATION_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;
        params.setTitle("CarSystemUI Bottom Navigation");
        windowManager.addView(navigationBar, params);
    }

    private void openSettings() {
        try {
            startActivityForCurrentUser(new Intent(Settings.ACTION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | ReflectiveOperationException error) {
            Log.e(TAG, "Unable to open Settings", error);
        }
    }

    private void showAppList() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setClassName(CAR_LAUNCHER_PACKAGE, APP_LIST_ACTIVITY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForCurrentUser(intent);
        } catch (ActivityNotFoundException | ReflectiveOperationException error) {
            Log.e(TAG, "Unable to show app list", error);
        }
    }

    /** Launches activities in the foreground Android user rather than SystemUI's system user. */
    private void startActivityForCurrentUser(Intent intent)
            throws ReflectiveOperationException {
        Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
        int currentUserId = (int) activityManagerClass.getMethod("getCurrentUser").invoke(null);
        UserHandle currentUser = (UserHandle) UserHandle.class
                .getMethod("of", int.class)
                .invoke(null, currentUserId);
        Method startActivityAsUser = getBaseContext().getClass().getMethod(
                "startActivityAsUser", Intent.class, UserHandle.class);
        startActivityAsUser.invoke(getBaseContext(), intent, currentUser);
    }

    private void injectKey(int keyCode) {
        try {
            Class<?> inputManagerClass = Class.forName("android.hardware.input.InputManager");
            Object inputManager = inputManagerClass.getMethod("getInstance").invoke(null);
            Method inject = inputManagerClass.getMethod("injectInputEvent",
                    android.view.InputEvent.class, int.class);
            long now = SystemClock.uptimeMillis();
            inject.invoke(inputManager, new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0),
                    INJECT_INPUT_EVENT_MODE_ASYNC);
            inject.invoke(inputManager, new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0),
                    INJECT_INPUT_EVENT_MODE_ASYNC);
        } catch (ReflectiveOperationException error) {
            Log.e(TAG, "Unable to inject navigation key " + keyCode, error);
        }
    }

    @Override
    public void onDestroy() {
        if (navigationBar != null && windowManager != null) {
            windowManager.removeView(navigationBar);
            navigationBar = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
