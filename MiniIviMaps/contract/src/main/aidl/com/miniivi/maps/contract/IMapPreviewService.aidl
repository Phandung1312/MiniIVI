package com.miniivi.maps.contract;

import android.os.IBinder;
import com.miniivi.maps.contract.IMapPreviewCallback;

interface IMapPreviewService {
    void createPreview(
        IBinder hostToken,
        int displayId,
        int width,
        int height,
        IMapPreviewCallback callback
    );
    void resizePreview(long sessionId, int width, int height);
    void releasePreview(long sessionId);
}
