package com.android.car.systemui.navigation

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
import com.android.car.systemui.presentation.BottomNavigationScreen
import com.android.car.systemui.presentation.CarSystemUiTheme
import com.android.car.systemui.presentation.QuickControlOverlay
import com.android.car.systemui.presentation.QuickControlViewModel
import com.android.car.systemui.presentation.SystemUiViewModel
import com.android.car.systemui.presentation.SystemUiViewModelFactory
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
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (
                    systemUiViewModel.state.value.quickControlVisible &&
                    keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_UP
                ) {
                    systemUiViewModel.dismissQuickControl()
                    true
                } else false
            }
            setContent {
                CarSystemUiTheme {
                    val systemState by systemUiViewModel.state.collectAsStateWithLifecycle()
                    val quickControlState by quickControlViewModel.state.collectAsStateWithLifecycle()
                    val navigationHeight = dimensionResource(R.dimen.navigation_bar_height)
                    Box(Modifier.fillMaxSize()) {
                        QuickControlOverlay(
                            visible = overlayAnimationVisible,
                            state = quickControlState,
                            onDismiss = systemUiViewModel::dismissQuickControl,
                            onBrightnessChanged = quickControlViewModel::onBrightnessChanged,
                            onBrightnessChangeFinished = quickControlViewModel::onBrightnessChangeFinished,
                            onVolumeChanged = quickControlViewModel::onVolumeChanged,
                            onTemperatureDecrease = quickControlViewModel::decreaseTemperature,
                            onTemperatureIncrease = quickControlViewModel::increaseTemperature,
                            onAcChanged = quickControlViewModel::setAc,
                            modifier = Modifier.fillMaxSize().padding(bottom = navigationHeight),
                        )
                        BottomNavigationScreen(
                            quickControlVisible = systemState.quickControlVisible,
                            onHome = systemUiViewModel::goHome,
                            onSettings = systemUiViewModel::openSettings,
                            onAppList = systemUiViewModel::openAppList,
                            onQuickControl = {
                                if (!systemState.quickControlVisible) quickControlViewModel.refresh()
                                systemUiViewModel.toggleQuickControl()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(navigationHeight),
                        )
                    }
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.navigation_bar_height),
            TYPE_NAVIGATION_BAR,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
            title = "CarSystemUI Bottom Navigation"
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
        val view = navigationView ?: return
        val params = navigationLayoutParams ?: return
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        params.flags = params.flags or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        params.dimAmount = 0.22f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            params.setBlurBehindRadius(28)
        }
        windowManager.updateViewLayout(view, params)
        view.requestFocus()
        overlayAnimationVisible = false
        view.post { overlayAnimationVisible = true }
    }

    private fun hideQuickControlWindow(animated: Boolean) {
        val view = navigationView ?: return
        val params = navigationLayoutParams ?: return
        overlayRemovalJob?.cancel()
        overlayAnimationVisible = false
        overlayRemovalJob = lifecycleScope.launch {
            if (animated) delay(OVERLAY_EXIT_DURATION_MS)
            if (!systemUiViewModel.state.value.quickControlVisible && navigationView === view) {
                params.height = resources.getDimensionPixelSize(R.dimen.navigation_bar_height)
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                    params.setBlurBehindRadius(0)
                }
                params.dimAmount = 0f
                windowManager.updateViewLayout(view, params)
                view.clearFocus()
            }
        }
    }

    private fun ownedComposeView(): ComposeView = ComposeView(this).also { view ->
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onDestroy() {
        overlayRemovalJob?.cancel()
        navigationView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        navigationView = null
        navigationLayoutParams = null
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    private companion object {
        const val TYPE_NAVIGATION_BAR = 2019
        const val OVERLAY_EXIT_DURATION_MS = 220L
    }
}
