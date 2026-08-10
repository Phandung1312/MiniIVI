package com.miniivi.car.api;

import com.miniivi.car.api.AudioState;
import com.miniivi.car.api.BrightnessState;
import com.miniivi.car.api.HvacState;
import com.miniivi.car.api.IAudioStateListener;
import com.miniivi.car.api.IBrightnessStateListener;
import com.miniivi.car.api.IHvacStateListener;

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
}
