package com.miniivi.car.api;

import com.miniivi.car.api.HvacState;

oneway interface IHvacStateListener {
    void onHvacStateChanged(in HvacState state);
}
