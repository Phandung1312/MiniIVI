package com.android.car.systemui.presentation

import android.content.res.Configuration
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.car.systemui.core.CarSystemUIStartable
import com.android.car.systemui.core.SystemUiComposeViewFactory
import com.android.car.systemui.core.SystemUiLifecycleOwner
import com.android.car.systemui.core.SystemUiWindowLayout
import com.android.car.systemui.presentation.controller.ControlCenterStateController
import com.android.car.systemui.presentation.controller.SystemUiStateController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControlCenterComponent @Inject constructor(
    private val windowManager: WindowManager,
    private val windowLayout: SystemUiWindowLayout,
    private val composeViewFactory: SystemUiComposeViewFactory,
    private val lifecycleOwner: SystemUiLifecycleOwner,
    private val systemUiStateController: SystemUiStateController,
    private val controlCenterStateController: ControlCenterStateController,
) : CarSystemUIStartable {
    private var overlayView: ComposeView? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var overlayAnimationVisible by mutableStateOf(false)
    private var overlayRemovalJob: Job? = null
    private var started = false

    override fun start() {
        if (started) return
        started = true
        controlCenterStateController.start()
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                systemUiStateController.state.collect { state ->
                    if (state.controlCenterVisible) showControlCenterWindow()
                    else hideControlCenterWindow(animated = true)
                }
            }
        }
    }

    override fun onConfigurationChanged(configuration: Configuration) {
        overlayLayoutParams?.let { params ->
            params.width = windowLayout.overlayWidthPx()
            params.x = windowLayout.navigationWidthPx()
            overlayView?.let { windowManager.updateViewLayout(it, params) }
        }
    }

    private fun showControlCenterWindow() {
        overlayRemovalJob?.cancel()
        overlayView?.let { existing ->
            overlayAnimationVisible = true
            existing.requestFocus()
            return
        }

        val view = composeViewFactory.create().apply {
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    systemUiStateController.dismissControlCenter()
                    true
                } else {
                    false
                }
            }
            setContent {
                CarSystemUiTheme {
                    val state by controlCenterStateController.state.collectAsStateWithLifecycle()
                    ControlCenterOverlay(
                        visible = overlayAnimationVisible,
                        state = state,
                        onDismiss = systemUiStateController::dismissControlCenter,
                        onBrightnessChanged = controlCenterStateController::onBrightnessChanged,
                        onBrightnessChangeFinished = {
                            logDebug(
                                "event=slider_commit control=brightness value=" +
                                    controlCenterStateController.state.value.displayedBrightness,
                            )
                            controlCenterStateController.onBrightnessChangeFinished()
                        },
                        onVolumeChanged = controlCenterStateController::onVolumeChanged,
                        onVolumeChangeFinished = {
                            logDebug(
                                "event=slider_commit control=volume value=" +
                                    controlCenterStateController.state.value.audio.volume,
                            )
                            controlCenterStateController.onVolumeChangeFinished()
                        },
                        onTemperatureDecrease = controlCenterStateController::decreaseTemperature,
                        onTemperatureIncrease = controlCenterStateController::increaseTemperature,
                        onAcChanged = controlCenterStateController::setAc,
                        onPowerChanged = controlCenterStateController::setPower,
                        onAutoChanged = controlCenterStateController::setAuto,
                        onSyncChanged = controlCenterStateController::setSync,
                        onRecirculationChanged = controlCenterStateController::setRecirculation,
                        onFanSpeedChanged = controlCenterStateController::setFanSpeed,
                        onFanDirectionChanged = controlCenterStateController::setFanDirection,
                        onFrontDefrostChanged = controlCenterStateController::setFrontDefrost,
                        onRearDefrostChanged = controlCenterStateController::setRearDefrost,
                        onSeatHeatingChanged = controlCenterStateController::setSeatHeating,
                        onSeatVentilationChanged = controlCenterStateController::setSeatVentilation,
                        onQuickControlChanged = controlCenterStateController::setQuickControl,
                        onShowMoreClimate = controlCenterStateController::showMoreClimate,
                        onHideMoreClimate = controlCenterStateController::hideMoreClimate,
                        onMaxAcChanged = controlCenterStateController::setMaxAc,
                        onMaxDefrostChanged = controlCenterStateController::setMaxDefrost,
                        onAutoRecirculationChanged = controlCenterStateController::setAutoRecirculation,
                        onSteeringWheelHeatChanged = controlCenterStateController::setSteeringWheelHeat,
                        onTemperatureUnitChanged = controlCenterStateController::setTemperatureUnit,
                        onOpenWifiSettings = {
                            openExternalApp(controlCenterStateController::openWifiSettings)
                        },
                        onOpenWirelessSettings = controlCenterStateController::openWirelessSettings,
                        onOpenBluetoothApp = {
                            openExternalApp(controlCenterStateController::openBluetoothApp)
                        },
                        onOpenCamera = controlCenterStateController::openCamera,
                        onHideCamera = controlCenterStateController::hideCamera,
                        onScreenOff = controlCenterStateController::requestScreenOff,
                        onDismissScreenCurtain = controlCenterStateController::dismissScreenCurtain,
                        onSettings = systemUiStateController::openSettings,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            windowLayout.overlayWidthPx(),
            WindowManager.LayoutParams.MATCH_PARENT,
            TYPE_NAVIGATION_BAR_PANEL,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.START
            x = windowLayout.navigationWidthPx()
            title = "CarSystemUI Control Center"
        }
        windowManager.addView(view, params)
        overlayView = view
        overlayLayoutParams = params
        view.requestFocus()
        overlayAnimationVisible = false
        view.post { overlayAnimationVisible = true }
        Log.i(TAG, "event=window_shown window=control_center width=${params.width}")
    }

    private fun hideControlCenterWindow(animated: Boolean) {
        val view = overlayView ?: return
        overlayRemovalJob?.cancel()
        overlayAnimationVisible = false
        overlayRemovalJob = lifecycleOwner.lifecycleScope.launch {
            if (animated) delay(OVERLAY_EXIT_DURATION_MS)
            if (!systemUiStateController.state.value.controlCenterVisible && overlayView === view) {
                runCatching { windowManager.removeViewImmediate(view) }
                overlayView = null
                overlayLayoutParams = null
                Log.i(TAG, "event=window_hidden window=control_center animated=$animated")
            }
        }
    }

    private fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, message)
    }

    private fun openExternalApp(open: () -> Unit) {
        open()
        systemUiStateController.onExternalAppOpened()
    }

    private companion object {
        const val TAG = "MiniIviSystemUi"
        const val TYPE_NAVIGATION_BAR_PANEL = 2024
        const val OVERLAY_EXIT_DURATION_MS = 220L
    }
}
