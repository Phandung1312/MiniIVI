package com.android.car.systemui.boot

internal object BootHandoffContract {
    const val ACTION_LAUNCHER_FIRST_FRAME_DRAWN =
        "com.miniivi.intent.action.LAUNCHER_FIRST_FRAME_DRAWN"
    const val CONTROL_PERMISSION = "com.miniivi.car.permission.CONTROL"
    const val SYSTEM_UI_PACKAGE = "com.android.car.systemui"
    const val PER_USER_RANGE = 100_000
}

internal object BootUserPolicy {
    fun isSystemUser(uid: Int): Boolean = uid / BootHandoffContract.PER_USER_RANGE == 0

    fun shouldShowHandoff(action: String?, uid: Int): Boolean =
        action == android.content.Intent.ACTION_LOCKED_BOOT_COMPLETED && isSystemUser(uid)

    fun shouldRelayLauncherReady(uid: Int): Boolean = !isSystemUser(uid)
}
