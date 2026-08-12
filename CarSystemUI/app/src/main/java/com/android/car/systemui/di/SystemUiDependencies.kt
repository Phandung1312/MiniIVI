package com.android.car.systemui.di

import android.content.Context
import com.android.car.systemui.data.repository.AndroidNavigationRepository
import com.android.car.systemui.data.repository.AndroidStartupRepository
import com.android.car.systemui.data.repository.CarServiceAudioRepository
import com.android.car.systemui.data.repository.CarServiceBrightnessRepository
import com.android.car.systemui.data.repository.CarServiceHvacRepository
import com.android.car.systemui.data.repository.CarServiceExtendedControlsRepository
import com.android.car.systemui.data.repository.CurrentUserProvider
import com.android.car.systemui.presentation.SystemUiViewModelFactory
import com.miniivi.car.client.MiniIviCarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-scoped dependencies for the privileged SystemUI process.
 *
 * AOSP/SystemUI builds can create this package with the framework Application class rather
 * than the manifest application class, so this intentionally does not rely on Hilt's
 * generated Application base class.
 */
class SystemUiDependencies private constructor(context: Context) {
    private val applicationContext = context.applicationContext
    private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val carClient = MiniIviCarClient(applicationContext)
    private val currentUserProvider = CurrentUserProvider(applicationContext)
    private val brightnessRepository = CarServiceBrightnessRepository(carClient, processScope)
    private val audioRepository = CarServiceAudioRepository(carClient, processScope)
    private val hvacRepository = CarServiceHvacRepository(carClient, processScope)
    private val extendedControlsRepository = CarServiceExtendedControlsRepository(carClient, processScope)
    private val navigationRepository = AndroidNavigationRepository(currentUserProvider)

    val startupRepository = AndroidStartupRepository()
    val viewModelFactory = SystemUiViewModelFactory(
        navigationRepository,
        brightnessRepository,
        audioRepository,
        hvacRepository,
        extendedControlsRepository,
    )

    companion object {
        @Volatile private var instance: SystemUiDependencies? = null

        fun from(context: Context): SystemUiDependencies = instance ?: synchronized(this) {
            instance ?: SystemUiDependencies(context).also { instance = it }
        }
    }
}
