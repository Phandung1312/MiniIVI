package com.miniivi.car.api;

import com.miniivi.car.api.BluetoothFeatureState;

oneway interface IBluetoothFeatureStateListener {
    void onBluetoothFeatureStateChanged(in BluetoothFeatureState state);
}
