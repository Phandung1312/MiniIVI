package com.android.car.systemui.navigation

import android.content.Context
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.car.systemui.R
import com.android.car.systemui.core.CarSystemUIStartable
import com.android.car.systemui.core.SystemUiComposeViewFactory
import com.android.car.systemui.core.SystemUiWindowLayout
import com.android.car.systemui.presentation.CarSystemUiTheme
import com.android.car.systemui.presentation.NavigationRailScreen
import com.android.car.systemui.presentation.SystemUiStateController
import com.android.car.systemui.presentation.ControlCenterStateController
import com.miniivi.car.api.NavigationStateContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationComponent @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val windowManager: WindowManager,
    private val windowLayout: SystemUiWindowLayout,
    private val composeViewFactory: SystemUiComposeViewFactory,
    private val systemUiStateController: SystemUiStateController,
    private val controlCenterStateController: ControlCenterStateController,
    private val launcherDestinationReceiver: LauncherDestinationReceiver,
) : CarSystemUIStartable {
    private var navigationView: ComposeView? = null
    private var navigationLayoutParams: WindowManager.LayoutParams? = null

    override fun start() {
        if (navigationView != null) return
        registerLauncherDestinationReceiver()
        showNavigationBar()
    }

    override fun onConfigurationChanged(configuration: android.content.res.Configuration) {
        navigationLayoutParams?.let { params ->
            params.width = windowLayout.navigationWidthPx()
            navigationView?.let { windowManager.updateViewLayout(it, params) }
        }
    }

    private fun registerLauncherDestinationReceiver() {
        val filter = IntentFilter(NavigationStateContract.ACTION_DESTINATION_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                launcherDestinationReceiver,
                filter,
                CROSS_USER_PERMISSION,
                Handler(context.mainLooper),
                Context.RECEIVER_EXPORTED,
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(
                launcherDestinationReceiver,
                filter,
                CROSS_USER_PERMISSION,
                Handler(context.mainLooper),
            )
        }
    }

    private fun showNavigationBar() {
        val view = composeViewFactory.create().apply {
            setContent {
                CarSystemUiTheme {
                    val systemState by systemUiStateController.state.collectAsStateWithLifecycle()
                    val controlState by controlCenterStateController.state.collectAsStateWithLifecycle()
                    val quickControls = controlState.extendedControls.quickControls
                    val navigationWidth = dimensionResource(R.dimen.navigation_rail_width)
                    NavigationRailScreen(
                        controlCenterVisible = systemState.controlCenterVisible,
                        selectedDestination = systemState.selectedDestination,
                        quickControls = quickControls,
                        audio = controlState.audio,
                        onHome = systemUiStateController::goHome,
                        onAppList = systemUiStateController::openAppList,
                        onPhone = systemUiStateController::openPhone,
                        onControlCenter = {
                            if (!systemState.controlCenterVisible) {
                                controlCenterStateController.refresh()
                            }
                            systemUiStateController.toggleControlCenter()
                        },
                        onSettings = systemUiStateController::openSettings,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(navigationWidth),
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            windowLayout.navigationWidthPx(),
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
        Log.i(TAG, "event=window_shown window=navigation_rail width=${params.width}")
    }

    private companion object {
        const val TAG = "MiniIviSystemUi"
        const val CROSS_USER_PERMISSION = "android.permission.INTERACT_ACROSS_USERS_FULL"
        const val TYPE_NAVIGATION_BAR_PANEL = 2024
    }
}
