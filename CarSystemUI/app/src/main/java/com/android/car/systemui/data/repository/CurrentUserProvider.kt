package com.android.car.systemui.data.repository

import android.content.Context
import android.os.UserHandle
import com.android.car.systemui.framework.FrameworkPlatformApi

class CurrentUserProvider(private val applicationContext: Context) {
    @Volatile
    private var lastKnownUserId: Int? = null

    fun userId(): Int = runCatching {
        FrameworkPlatformApi.getCurrentUser().also { lastKnownUserId = it }
    }.getOrElse { error ->
        lastKnownUserId ?: throw IllegalStateException("Unable to resolve foreground user", error)
    }

    fun userHandle(): UserHandle =
        UserHandle.getUserHandleForUid(userId() * PER_USER_RANGE)

    fun context(): Context = runCatching {
        Context::class.java.getMethod(
            "createContextAsUser",
            UserHandle::class.java,
            Int::class.javaPrimitiveType,
        ).invoke(applicationContext, userHandle(), 0) as Context
    }.getOrElse { error ->
        throw IllegalStateException("Unable to create context for foreground user ${userId()}", error)
    }

    private companion object {
        const val PER_USER_RANGE = 100_000
    }
}
