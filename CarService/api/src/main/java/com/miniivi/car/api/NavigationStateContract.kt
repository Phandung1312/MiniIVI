package com.miniivi.car.api

/** Internal contract used to keep the system navigation rail in sync with the launcher. */
object NavigationStateContract {
    const val ACTION_DESTINATION_CHANGED =
        "com.android.car.systemui.action.NAVIGATION_DESTINATION_CHANGED"
    const val EXTRA_DESTINATION = "com.android.car.systemui.extra.NAVIGATION_DESTINATION"

    const val DESTINATION_HOME = "home"
    const val DESTINATION_APP_LIST = "apps"
    const val DESTINATION_NONE = "none"
}
