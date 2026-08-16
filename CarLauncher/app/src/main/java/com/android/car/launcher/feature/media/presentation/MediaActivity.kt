package com.android.car.launcher.feature.media.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.android.car.launcher.core.lifecycle.LifeCycleLogger
import com.android.car.launcher.core.ui.MiniIviTheme
import com.android.car.launcher.feature.media.presentation.ui.MediaRoute
import com.android.car.launcher.feature.media.presentation.viewmodel.MediaViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaActivity : LifeCycleLogger() {
    private val viewModel by viewModels<MediaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniIviTheme {
                MediaRoute(
                    viewModel = viewModel,
                    onBack = ::finish,
                )
            }
        }
    }
}
