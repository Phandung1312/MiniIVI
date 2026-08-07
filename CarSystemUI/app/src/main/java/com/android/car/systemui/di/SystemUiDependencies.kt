package com.android.car.systemui.di

import android.content.Context
import com.android.car.systemui.data.repository.AndroidAudioRepository
import com.android.car.systemui.data.repository.AndroidBrightnessRepository
import com.android.car.systemui.data.repository.AndroidHvacRepository
import com.android.car.systemui.data.repository.AndroidNavigationRepository
import com.android.car.systemui.data.repository.AndroidStartupRepository
import com.android.car.systemui.data.repository.CurrentUserProvider
import com.android.car.systemui.presentation.SystemUiViewModelFactory

/**
 * Process-scoped dependencies for the privileged SystemUI process.
 *
 * AOSP/SystemUI builds can create this package with the framework Application class rather
 * than the manifest application class, so this intentionally does not rely on Hilt's
 * generated Application base class.
 */
class SystemUiDependencies private constructor(context: Context) {
    private val applicationContext = context.applicationContext
    private val currentUserProvider = CurrentUserProvider(applicationContext)
    private val brightnessRepository = AndroidBrightnessRepository(
        applicationContext,
        currentUserProvider,
    )
    private val audioRepository = AndroidAudioRepository(applicationContext)
    private val hvacRepository = AndroidHvacRepository(applicationContext)
    private val navigationRepository = AndroidNavigationRepository(currentUserProvider)

    val startupRepository = AndroidStartupRepository()
    val viewModelFactory = SystemUiViewModelFactory(
        navigationRepository,
        brightnessRepository,
        audioRepository,
        hvacRepository,
    )

    companion object {
        @Volatile private var instance: SystemUiDependencies? = null

        fun from(context: Context): SystemUiDependencies = instance ?: synchronized(this) {
            instance ?: SystemUiDependencies(context).also { instance = it }
        }
    }
}
