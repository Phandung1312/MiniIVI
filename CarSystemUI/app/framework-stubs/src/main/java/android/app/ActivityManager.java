package android.app;

/** Compile-only declaration for the platform API supplied by the system image. */
public final class ActivityManager {
    private ActivityManager() {}

    public static int getCurrentUser() {
        throw new UnsupportedOperationException("Framework stub");
    }
}
