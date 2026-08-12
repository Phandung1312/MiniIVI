package android.content;

import android.os.UserHandle;

/** Compile-only declaration for the platform API supplied by the system image. */
public abstract class Context {
    public void startActivityAsUser(Intent intent, UserHandle user) {
        throw new UnsupportedOperationException("Framework stub");
    }
}
