package com.android.car.systemui.data.policy

import android.os.Process
import com.android.car.systemui.domain.policy.SystemUserPolicy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSystemUserPolicy @Inject constructor() : SystemUserPolicy {
    override fun isSystemUser(): Boolean = Process.myUid() / PER_USER_RANGE == 0

    private companion object {
        const val PER_USER_RANGE = 100_000
    }
}
