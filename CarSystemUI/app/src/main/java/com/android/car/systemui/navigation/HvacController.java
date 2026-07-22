package com.android.car.systemui.navigation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;

/** Connects the navigation bar to real AAOS VHAL climate properties. */
final class HvacController {
    interface Listener {
        void onClimateData(Float cabin, Float leftSet, Float rightSet, Boolean acOn);
        void onClimateUnavailable();
    }

    private static final String TAG = "CarSystemUI-HVAC";
    private static final String PROPERTY_SERVICE = "property";
    private static final float SENSOR_RATE_ONCHANGE = 0f;

    // Stable VehiclePropertyIds values defined by Android Automotive VHAL.
    private static final int HVAC_TEMPERATURE_CURRENT = 0x15600502;
    private static final int HVAC_TEMPERATURE_SET = 0x15600503;
    private static final int HVAC_AC_ON = 0x15200505;

    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Object car;
    private Object propertyManager;
    private Object propertyCallback;
    private int leftArea;
    private int rightArea;
    private int cabinArea;
    private int acArea;
    private boolean hasCabinTemperature;
    private boolean hasAc;
    private Float cabinTemperature;
    private Float leftTemperature;
    private Float rightTemperature;
    private Boolean acOn;

    HvacController(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    void connect() {
        new Thread(this::connectBlocking, "CarSystemUI-HVAC").start();
    }

    private void connectBlocking() {
        try {
            Class<?> carClass = Class.forName("android.car.Car");
            car = carClass.getMethod("createCar", Context.class).invoke(null, context);
            propertyManager = carClass.getMethod("getCarManager", String.class)
                    .invoke(car, PROPERTY_SERVICE);
            if (propertyManager == null) throw new IllegalStateException("No car property service");

            int[] setAreas = getAreaIds(HVAC_TEMPERATURE_SET);
            int[] currentAreas = getAreaIds(HVAC_TEMPERATURE_CURRENT);
            int[] acAreas = getAreaIds(HVAC_AC_ON);
            if (setAreas.length == 0) throw new IllegalStateException("No HVAC temperature zones");
            leftArea = setAreas[0];
            rightArea = setAreas.length > 1 ? setAreas[setAreas.length - 1] : leftArea;
            cabinArea = currentAreas.length > 0 ? currentAreas[0] : leftArea;
            acArea = acAreas.length > 0 ? acAreas[0] : leftArea;
            hasCabinTemperature = currentAreas.length > 0;
            hasAc = acAreas.length > 0;

            createAndRegisterCallback();
            if (hasCabinTemperature) {
                cabinTemperature = readFloat(HVAC_TEMPERATURE_CURRENT, cabinArea);
            }
            leftTemperature = readFloat(HVAC_TEMPERATURE_SET, leftArea);
            rightTemperature = readFloat(HVAC_TEMPERATURE_SET, rightArea);
            if (hasAc) acOn = readBoolean(HVAC_AC_ON, acArea);
            publish();
        } catch (Exception error) {
            Log.e(TAG, "Cannot connect to AAOS climate properties", error);
            mainHandler.post(listener::onClimateUnavailable);
        }
    }

    private int[] getAreaIds(int propertyId) throws Exception {
        Method method = propertyManager.getClass().getMethod("getCarPropertyConfig", int.class);
        Object config = method.invoke(propertyManager, propertyId);
        if (config == null) return new int[0];
        Object ids = config.getClass().getMethod("getAreaIds").invoke(config);
        int length = Array.getLength(ids);
        int[] result = new int[length];
        for (int i = 0; i < length; i++) result[i] = (Integer) Array.get(ids, i);
        return result;
    }

    private void createAndRegisterCallback() throws Exception {
        Class<?> callbackClass = Class.forName(
                "android.car.hardware.property.CarPropertyManager$CarPropertyEventCallback");
        propertyCallback = Proxy.newProxyInstance(callbackClass.getClassLoader(),
                new Class<?>[]{callbackClass}, (proxy, method, args) -> {
                    if (method.getName().equals("onChangeEvent") && args != null) {
                        handlePropertyValue(args[0]);
                    } else if (method.getName().equals("toString")) {
                        return "CarSystemUI HVAC callback";
                    } else if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    } else if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    return null;
                });
        Method register = propertyManager.getClass().getMethod("registerCallback",
                callbackClass, int.class, float.class);
        if (hasCabinTemperature) {
            register.invoke(propertyManager, propertyCallback, HVAC_TEMPERATURE_CURRENT,
                    SENSOR_RATE_ONCHANGE);
        }
        register.invoke(propertyManager, propertyCallback, HVAC_TEMPERATURE_SET,
                SENSOR_RATE_ONCHANGE);
        if (hasAc) {
            register.invoke(propertyManager, propertyCallback, HVAC_AC_ON, SENSOR_RATE_ONCHANGE);
        }
    }

    private void handlePropertyValue(Object value) {
        try {
            int propertyId = (Integer) value.getClass().getMethod("getPropertyId").invoke(value);
            int areaId = (Integer) value.getClass().getMethod("getAreaId").invoke(value);
            Object data = value.getClass().getMethod("getValue").invoke(value);
            if (propertyId == HVAC_TEMPERATURE_CURRENT && areaId == cabinArea) {
                cabinTemperature = ((Number) data).floatValue();
            } else if (propertyId == HVAC_TEMPERATURE_SET) {
                if (areaId == leftArea) leftTemperature = ((Number) data).floatValue();
                if (areaId == rightArea) rightTemperature = ((Number) data).floatValue();
            } else if (propertyId == HVAC_AC_ON && areaId == acArea) {
                acOn = (Boolean) data;
            }
            publish();
        } catch (Exception error) {
            Log.w(TAG, "Invalid climate property event", error);
        }
    }

    private Float readFloat(int propertyId, int areaId) throws Exception {
        Object value = propertyManager.getClass()
                .getMethod("getFloatProperty", int.class, int.class)
                .invoke(propertyManager, propertyId, areaId);
        return ((Number) value).floatValue();
    }

    private Boolean readBoolean(int propertyId, int areaId) throws Exception {
        return (Boolean) propertyManager.getClass()
                .getMethod("getBooleanProperty", int.class, int.class)
                .invoke(propertyManager, propertyId, areaId);
    }

    void adjustLeft(float delta) {
        if (leftTemperature != null) setTemperature(leftArea, leftTemperature + delta);
    }

    void adjustRight(float delta) {
        if (rightTemperature != null) setTemperature(rightArea, rightTemperature + delta);
    }

    private void setTemperature(int areaId, float temperature) {
        try {
            propertyManager.getClass().getMethod("setFloatProperty", int.class, int.class,
                    float.class).invoke(propertyManager, HVAC_TEMPERATURE_SET, areaId, temperature);
        } catch (Exception error) {
            Log.e(TAG, "Cannot set HVAC temperature", error);
        }
    }

    void setAc(boolean enabled) {
        if (!hasAc) return;
        try {
            propertyManager.getClass().getMethod("setBooleanProperty", int.class, int.class,
                    boolean.class).invoke(propertyManager, HVAC_AC_ON, acArea, enabled);
        } catch (Exception error) {
            Log.e(TAG, "Cannot set A/C state", error);
        }
    }

    private void publish() {
        Float cabin = cabinTemperature;
        Float left = leftTemperature;
        Float right = rightTemperature;
        Boolean ac = acOn;
        mainHandler.post(() -> listener.onClimateData(cabin, left, right, ac));
    }

    static String formatTemperature(Float value) {
        return value == null ? "--°C" : String.format(Locale.getDefault(), "%.1f°C", value);
    }

    void disconnect() {
        if (propertyManager != null && propertyCallback != null) {
            try {
                for (Method method : propertyManager.getClass().getMethods()) {
                    if (method.getName().equals("unregisterCallback")
                            && method.getParameterTypes().length == 1) {
                        method.invoke(propertyManager, propertyCallback);
                        break;
                    }
                }
            } catch (Exception error) {
                Log.w(TAG, "Cannot unregister HVAC callback", error);
            }
        }
        if (car != null) {
            try {
                car.getClass().getMethod("disconnect").invoke(car);
            } catch (Exception ignored) {
            }
        }
    }
}
