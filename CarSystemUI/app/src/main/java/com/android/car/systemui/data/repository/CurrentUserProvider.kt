package com.android.car.systemui.data.repository

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import com.android.car.systemui.framework.FrameworkPlatformApi

class CurrentUserProvider(private val applicationContext: Context) {
    @Volatile
    private var lastKnownUserId: Int? = null
    @Volatile
    private var fallbackReported = false

    fun userId(): Int = runCatching {
        FrameworkPlatformApi.getCurrentUser().also { userId ->
            val previous = lastKnownUserId
            lastKnownUserId = userId
            fallbackReported = false
            if (previous != userId) Log.i(TAG, "event=foreground_user_changed user_id=$userId")
        }
    }.getOrElse { error ->
        lastKnownUserId?.also { userId ->
            if (!fallbackReported) {
                fallbackReported = true
                Log.w(TAG, "event=foreground_user_fallback user_id=$userId", error)
            }
        } ?: throw IllegalStateException("Unable to resolve foreground user", error)
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

    fun launch(intent: Intent) {
        FrameworkPlatformApi.startActivityAsUser(applicationContext, intent, userHandle())
    }

    private companion object {
        const val TAG = "MiniIviCurrentUser"
        const val PER_USER_RANGE = 100_000
    }
}
