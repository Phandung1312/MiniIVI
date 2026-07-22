package com.android.car.systemui.navigation;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import com.android.car.systemui.R;

import java.lang.reflect.Method;

public final class BottomNavigationService extends Service implements HvacController.Listener {
    private static final int TYPE_NAVIGATION_BAR = 2019;
    private static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;

    private WindowManager windowManager;
    private View navigationBar;
    private HvacController hvacController;
    private TextView climateStatus;
    private TextView leftTemperature;
    private TextView rightTemperature;
    private ToggleButton acButton;
    private boolean updatingAcButton;
    private PopupWindow controlCenter;

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
                v -> injectKey(KeyEvent.KEYCODE_HOME));
        navigationBar.findViewById(R.id.nav_control_center).setOnClickListener(
                v -> toggleControlCenter());
        climateStatus = navigationBar.findViewById(R.id.climate_status);
        leftTemperature = navigationBar.findViewById(R.id.temp_left);
        rightTemperature = navigationBar.findViewById(R.id.temp_right);
        acButton = navigationBar.findViewById(R.id.climate_ac);
        navigationBar.findViewById(R.id.temp_left_down).setOnClickListener(
                v -> hvacController.adjustLeft(-0.5f));
        navigationBar.findViewById(R.id.temp_left_up).setOnClickListener(
                v -> hvacController.adjustLeft(0.5f));
        navigationBar.findViewById(R.id.temp_right_down).setOnClickListener(
                v -> hvacController.adjustRight(-0.5f));
        navigationBar.findViewById(R.id.temp_right_up).setOnClickListener(
                v -> hvacController.adjustRight(0.5f));
        acButton.setOnCheckedChangeListener((button, checked) -> {
            if (!updatingAcButton) hvacController.setAc(checked);
        });

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
        hvacController = new HvacController(this, this);
        hvacController.connect();
    }

    private void toggleControlCenter() {
        if (controlCenter != null && controlCenter.isShowing()) {
            controlCenter.dismiss();
            return;
        }

        View panel = LayoutInflater.from(this).inflate(R.layout.control_center_panel, null);
        panel.findViewById(R.id.control_wifi).setOnClickListener(
                v -> openSettings(Settings.ACTION_WIFI_SETTINGS));
        panel.findViewById(R.id.control_bluetooth).setOnClickListener(
                v -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS));
        panel.findViewById(R.id.control_settings).setOnClickListener(
                v -> openSettings(Settings.ACTION_SETTINGS));

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        SeekBar volume = panel.findViewById(R.id.control_volume_slider);
        volume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        controlCenter = new PopupWindow(panel,
                getResources().getDimensionPixelSize(R.dimen.control_center_width),
                WindowManager.LayoutParams.WRAP_CONTENT, true);
        controlCenter.setBackgroundDrawable(new ColorDrawable(0x00000000));
        controlCenter.setOutsideTouchable(true);
        controlCenter.setElevation(16f);
        controlCenter.showAtLocation(navigationBar, Gravity.BOTTOM | Gravity.RIGHT,
                getResources().getDimensionPixelSize(R.dimen.control_center_margin),
                getResources().getDimensionPixelSize(R.dimen.navigation_bar_height)
                        + getResources().getDimensionPixelSize(R.dimen.control_center_margin));
    }

    private void openSettings(String action) {
        try {
            startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            if (controlCenter != null) controlCenter.dismiss();
        } catch (ActivityNotFoundException ignored) {
        }
    }

    @Override
    public void onClimateData(Float cabin, Float leftSet, Float rightSet, Boolean acOn) {
        climateStatus.setText("Cabin " + HvacController.formatTemperature(cabin));
        leftTemperature.setText(HvacController.formatTemperature(leftSet));
        rightTemperature.setText(HvacController.formatTemperature(rightSet));
        if (acOn != null) {
            updatingAcButton = true;
            acButton.setChecked(acOn);
            acButton.setAlpha(acOn ? 1f : 0.55f);
            updatingAcButton = false;
        }
        
    }

    @Override
    public void onClimateUnavailable() {
        climateStatus.setText(R.string.climate_unavailable);
        acButton.setEnabled(false);
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
        } catch (ReflectiveOperationException ignored) {

        }
    }

    @Override
    public void onDestroy() {
        if (controlCenter != null) controlCenter.dismiss();
        if (hvacController != null) hvacController.disconnect();
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
