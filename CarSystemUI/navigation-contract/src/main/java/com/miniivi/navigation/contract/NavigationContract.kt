package com.miniivi.navigation.contract

/** Cross-process contract owned by the SystemUI navigation surface. */
object NavigationContract {
    const val SYSTEM_UI_PACKAGE = "com.android.car.systemui"
    const val NAVIGATION_STATE_SERVICE_CLASS =
        "com.android.car.systemui.navigation.NavigationStateService"
    const val BIND_NAVIGATION_STATE_PERMISSION =
        "com.android.car.systemui.permission.BIND_NAVIGATION_STATE"

    const val DESTINATION_HOME = 1
    const val DESTINATION_APP_LIST = 2
    const val DESTINATION_NONE = 3
}
