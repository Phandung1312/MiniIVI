package com.miniivi.maps.contract;

import android.os.Bundle;

interface IMapPreviewCallback {
    void onPreviewReady(long sessionId, in Bundle surfacePackage);
    void onPreviewStateChanged(long sessionId, int state);
    void onPreviewError(int errorCode, String message);
}
