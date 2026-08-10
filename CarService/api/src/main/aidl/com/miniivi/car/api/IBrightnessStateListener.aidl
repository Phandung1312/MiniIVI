package com.miniivi.car.api;

import com.miniivi.car.api.BrightnessState;

oneway interface IBrightnessStateListener {
    void onBrightnessStateChanged(in BrightnessState state);
}
