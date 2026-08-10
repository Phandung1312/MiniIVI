package com.miniivi.car.api;

import com.miniivi.car.api.AudioState;

oneway interface IAudioStateListener {
    void onAudioStateChanged(in AudioState state);
}
