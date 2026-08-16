package com.android.car.systemui.core

import android.os.Process
import javax.inject.Inject
import javax.inject.Singleton

interface SystemUserPolicy {
    fun isSystemUser(): Boolean
}

@Singleton
class AndroidSystemUserPolicy @Inject constructor() : SystemUserPolicy {
    override fun isSystemUser(): Boolean = Process.myUid() / PER_USER_RANGE == 0

    private companion object {
        const val PER_USER_RANGE = 100_000
    }
}
