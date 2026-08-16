package android.hardware.display;

import android.os.Handler;
import android.view.Display;
import java.util.concurrent.Executor;

/** Compile-only declarations for display APIs supplied by the system image. */
public final class DisplayManager {
    public interface DisplayListener {
        void onDisplayAdded(int displayId);
        void onDisplayRemoved(int displayId);
        void onDisplayChanged(int displayId);
    }

    public Display getDisplay(int displayId) {
        throw new UnsupportedOperationException("Framework stub");
    }

    public void registerDisplayListener(DisplayListener listener, Handler handler) {
        throw new UnsupportedOperationException("Framework stub");
    }

    public void unregisterDisplayListener(DisplayListener listener) {
        throw new UnsupportedOperationException("Framework stub");
    }

    public float getBrightness(int displayId, int brightnessUnit) {
        throw new UnsupportedOperationException("Framework stub");
    }

    public void setBrightness(int displayId, float brightness, int brightnessUnit) {
        throw new UnsupportedOperationException("Framework stub");
    }

    public void registerDisplayListener(
            Executor executor, long eventsMask, DisplayListener listener) {
        throw new UnsupportedOperationException("Framework stub");
    }

}
