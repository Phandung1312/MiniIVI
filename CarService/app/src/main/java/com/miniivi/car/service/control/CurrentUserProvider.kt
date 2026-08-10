package com.miniivi.car.service.control

import android.content.Context
import android.os.UserHandle
import com.miniivi.car.service.framework.FrameworkPlatformApi

class CurrentUserProvider(private val applicationContext: Context) {
    @Volatile private var lastKnownUserId: Int? = null

    fun userId(): Int = runCatching {
        FrameworkPlatformApi.getCurrentUser().also { lastKnownUserId = it }
    }.getOrElse { error ->
        lastKnownUserId ?: throw IllegalStateException("Unable to resolve the foreground user", error)
    }

    fun context(): Context {
        val userId = userId()
        val handle = UserHandle.getUserHandleForUid(userId * PER_USER_RANGE)
        return runCatching {
            Context::class.java.getMethod(
                "createContextAsUser",
                UserHandle::class.java,
                Int::class.javaPrimitiveType,
            ).invoke(applicationContext, handle, 0) as Context
        }.getOrElse { error ->
            throw IllegalStateException("Unable to create a context for user $userId", error)
        }
    }

    private companion object {
        const val PER_USER_RANGE = 100_000
    }
}
