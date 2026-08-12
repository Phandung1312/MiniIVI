package com.miniivi.car.api;

import com.miniivi.car.api.ClimateControlState;

oneway interface IClimateControlStateListener {
    void onClimateControlStateChanged(in ClimateControlState state);
}
