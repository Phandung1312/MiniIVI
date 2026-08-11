package com.android.car.systemui.navigation

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.car.systemui.R
import com.android.car.systemui.di.SystemUiDependencies
import com.android.car.systemui.presentation.CarSystemUiTheme
import com.android.car.systemui.presentation.NavigationRailScreen
import com.android.car.systemui.presentation.QuickControlOverlay
import com.android.car.systemui.presentation.QuickControlViewModel
import com.android.car.systemui.presentation.SystemUiViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BottomNavigationService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private lateinit var systemUiViewModel: SystemUiViewModel
    private lateinit var quickControlViewModel: QuickControlViewModel
    private var navigationView: ComposeView? = null
    private var navigationLayoutParams: WindowManager.LayoutParams? = null
    private var overlayView: ComposeView? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var overlayAnimationVisible by mutableStateOf(false)
    private var overlayRemovalJob: Job? = null

    override fun onCreate() {
        savedStateRegistryController.performAttach()
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WindowManager::class.java)
        val provider = ViewModelProvider(this, SystemUiDependencies.from(this).viewModelFactory)
        systemUiViewModel = provider[SystemUiViewModel::class.java]
        quickControlViewModel = provider[QuickControlViewModel::class.java]
        showNavigationBar()
        observeOverlayState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    private fun showNavigationBar() {
        if (navigationView != null) return
        val view = ownedComposeView().apply {
            setContent {
                CarSystemUiTheme {
                    val systemState by systemUiViewModel.state.collectAsStateWithLifecycle()
                    val navigationWidth = dimensionResource(R.dimen.navigation_rail_width)
                    NavigationRailScreen(
                        quickControlVisible = systemState.quickControlVisible,
                        onHome = systemUiViewModel::goHome,
                        onAppList = systemUiViewModel::openAppList,
                        onQuickControl = {
                            if (!systemState.quickControlVisible) quickControlViewModel.refresh()
                            systemUiViewModel.toggleQuickControl()
                        },
                        onSettings = systemUiViewModel::openSettings,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(navigationWidth),
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            navigationWidthPx(),
            WindowManager.LayoutParams.MATCH_PARENT,
            TYPE_NAVIGATION_BAR_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.START
            title = "CarSystemUI Navigation Rail"
        }
        windowManager.addView(view, params)
        navigationView = view
        navigationLayoutParams = params
    }

    private fun observeOverlayState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                systemUiViewModel.state.collect { state ->
                    if (state.quickControlVisible) showQuickControlWindow()
                    else hideQuickControlWindow(animated = true)
                }
            }
        }
    }

    private fun showQuickControlWindow() {
        overlayRemovalJob?.cancel()
        overlayView?.let { existing ->
            overlayAnimationVisible = true
            existing.requestFocus()
            return
        }

        val view = ownedComposeView().apply {
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    systemUiViewModel.dismissQuickControl()
                    true
                } else {
                    false
                }
            }
            setContent {
                CarSystemUiTheme {
                    val state by quickControlViewModel.state.collectAsStateWithLifecycle()
                    QuickControlOverlay(
                        visible = overlayAnimationVisible,
                        state = state,
                        onDismiss = systemUiViewModel::dismissQuickControl,
                        onBrightnessChanged = quickControlViewModel::onBrightnessChanged,
                        onBrightnessChangeFinished = quickControlViewModel::onBrightnessChangeFinished,
                        onVolumeChanged = quickControlViewModel::onVolumeChanged,
                        onTemperatureDecrease = quickControlViewModel::decreaseTemperature,
                        onTemperatureIncrease = quickControlViewModel::increaseTemperature,
                        onAcChanged = quickControlViewModel::setAc,
                        onSettings = {
                            systemUiViewModel.dismissQuickControl()
                            systemUiViewModel.openSettings()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            overlayWidthPx(),
            WindowManager.LayoutParams.MATCH_PARENT,
            TYPE_NAVIGATION_BAR_PANEL,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.START
            x = navigationWidthPx()
            title = "CarSystemUI Quick Control"
        }
        windowManager.addView(view, params)
        overlayView = view
        overlayLayoutParams = params
        view.requestFocus()
        overlayAnimationVisible = false
        view.post { overlayAnimationVisible = true }
    }

    private fun hideQuickControlWindow(animated: Boolean) {
        val view = overlayView ?: return
        overlayRemovalJob?.cancel()
        overlayAnimationVisible = false
        overlayRemovalJob = lifecycleScope.launch {
            if (animated) delay(OVERLAY_EXIT_DURATION_MS)
            if (!systemUiViewModel.state.value.quickControlVisible && overlayView === view) {
                runCatching { windowManager.removeViewImmediate(view) }
                overlayView = null
                overlayLayoutParams = null
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        navigationLayoutParams?.let { params ->
            params.width = navigationWidthPx()
            navigationView?.let { windowManager.updateViewLayout(it, params) }
        }
        overlayLayoutParams?.let { params ->
            params.width = overlayWidthPx()
            params.x = navigationWidthPx()
            overlayView?.let { windowManager.updateViewLayout(it, params) }
        }
    }

    private fun navigationWidthPx(): Int =
        resources.getDimensionPixelSize(R.dimen.navigation_rail_width)

    private fun overlayWidthPx(): Int =
        (availableDisplayWidthPx() - navigationWidthPx()).coerceAtLeast(1)

    private fun availableDisplayWidthPx(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return resources.displayMetrics.widthPixels
        }
        val metrics = windowManager.currentWindowMetrics
        val cutout = metrics.windowInsets.displayCutout
        return (
            metrics.bounds.width() -
                (cutout?.safeInsetLeft ?: 0) -
                (cutout?.safeInsetRight ?: 0)
            ).coerceAtLeast(1)
    }

    private fun ownedComposeView(): ComposeView = ComposeView(this).also { view ->
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onDestroy() {
        overlayRemovalJob?.cancel()
        overlayView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        navigationView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        overlayView = null
        overlayLayoutParams = null
        navigationView = null
        navigationLayoutParams = null
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    private companion object {
        const val TYPE_NAVIGATION_BAR_PANEL = 2024
        const val OVERLAY_EXIT_DURATION_MS = 220L
    }
}
