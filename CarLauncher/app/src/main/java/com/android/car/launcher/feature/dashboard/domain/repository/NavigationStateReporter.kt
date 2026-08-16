package com.android.car.launcher.feature.dashboard.domain.repository

enum class LauncherNavigationDestination {
    Home,
    AppList,
    None,
}

interface NavigationStateReporter {
    fun report(destination: LauncherNavigationDestination)
}
