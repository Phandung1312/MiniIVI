package com.miniivi.car.api;

import com.miniivi.car.api.QuickControlsState;

oneway interface IQuickControlsStateListener {
    void onQuickControlsStateChanged(in QuickControlsState state);
}
