package com.miniivi.car.api;

import com.miniivi.car.api.VehicleStatusState;

oneway interface IVehicleStatusListener {
    void onVehicleStatusChanged(in VehicleStatusState state);
}
