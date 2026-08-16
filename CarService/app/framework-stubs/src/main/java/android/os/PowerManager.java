package android.os;

/** Compile-only declarations for platform APIs supplied by the system image. */
public final class PowerManager {
    public void goToSleep(long time) {
        throw new UnsupportedOperationException("Framework stub");
    }

    public int getMinimumScreenBrightnessSetting() {
        throw new UnsupportedOperationException("Framework stub");
    }

    public int getMaximumScreenBrightnessSetting() {
        throw new UnsupportedOperationException("Framework stub");
    }
}
