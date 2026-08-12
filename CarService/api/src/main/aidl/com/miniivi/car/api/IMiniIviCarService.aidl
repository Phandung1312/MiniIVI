package com.miniivi.car.api;

import com.miniivi.car.api.AudioState;
import com.miniivi.car.api.BrightnessState;
import com.miniivi.car.api.HvacState;
import com.miniivi.car.api.IAudioStateListener;
import com.miniivi.car.api.IBrightnessStateListener;
import com.miniivi.car.api.IHvacStateListener;
import com.miniivi.car.api.IVehicleStatusListener;
import com.miniivi.car.api.VehicleStatusState;
import com.miniivi.car.api.ClimateControlState;
import com.miniivi.car.api.QuickControlsState;
import com.miniivi.car.api.IClimateControlStateListener;
import com.miniivi.car.api.IQuickControlsStateListener;

interface IMiniIviCarService {
    int getApiVersion();

    BrightnessState getBrightnessState();
    AudioState getAudioState();
    HvacState getHvacState();

    void registerBrightnessListener(IBrightnessStateListener listener);
    void unregisterBrightnessListener(IBrightnessStateListener listener);
    void registerAudioListener(IAudioStateListener listener);
    void unregisterAudioListener(IAudioStateListener listener);
    void registerHvacListener(IHvacStateListener listener);
    void unregisterHvacListener(IHvacStateListener listener);

    oneway void setBrightness(float progress);
    oneway void setMediaVolume(int volume);
    oneway void setHvacTemperature(int zone, float celsius);
    oneway void setAcEnabled(boolean enabled);

    VehicleStatusState getVehicleStatusState();
    void registerVehicleStatusListener(IVehicleStatusListener listener);
    void unregisterVehicleStatusListener(IVehicleStatusListener listener);

    ClimateControlState getClimateControlState();
    void registerClimateControlStateListener(IClimateControlStateListener listener);
    void unregisterClimateControlStateListener(IClimateControlStateListener listener);
    oneway void setClimatePowerEnabled(boolean enabled);
    oneway void setClimateAutoEnabled(boolean enabled);
    oneway void setClimateSyncEnabled(boolean enabled);
    oneway void setClimateRecirculationEnabled(boolean enabled);
    oneway void setClimateFanSpeed(int zone, int speed);
    oneway void setClimateFanDirection(int zone, int direction);
    oneway void setClimateDefrosterEnabled(int window, boolean enabled);
    oneway void setSeatHeatingLevel(int zone, int level);
    oneway void setSeatVentilationLevel(int zone, int level);
    oneway void setMaxAcEnabled(boolean enabled);
    oneway void setMaxDefrostEnabled(boolean enabled);
    oneway void setAutoRecirculationEnabled(boolean enabled);
    oneway void setSteeringWheelHeatLevel(int level);
    oneway void setTemperatureUnit(int unit);

    QuickControlsState getQuickControlsState();
    void registerQuickControlsStateListener(IQuickControlsStateListener listener);
    void unregisterQuickControlsStateListener(IQuickControlsStateListener listener);
    oneway void setQuickControlEnabled(int control, boolean enabled);
    oneway void requestScreenOff();
}
