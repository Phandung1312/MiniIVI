package com.android.car.systemui.data.repository

import android.content.Context
import android.os.Process
import android.os.UserHandle

class CurrentUserProvider(private val applicationContext: Context) {
    fun userId(): Int = runCatching {
        Class.forName("android.app.ActivityManager")
            .getMethod("getCurrentUser")
            .invoke(null) as Int
    }.getOrDefault(Process.myUid() / PER_USER_RANGE)

    fun context(): Context = runCatching {
        val handle = UserHandle::class.java.getMethod("of", Int::class.javaPrimitiveType)
            .invoke(null, userId()) as UserHandle
        Context::class.java.getMethod(
            "createContextAsUser",
            UserHandle::class.java,
            Int::class.javaPrimitiveType,
        ).invoke(applicationContext, handle, 0) as Context
    }.getOrDefault(applicationContext)

    private companion object {
        const val PER_USER_RANGE = 100_000
    }
}
