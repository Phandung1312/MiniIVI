package com.android.car.launcher.feature.media

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.android.car.launcher.core.lifecycle.LifeCycleLogger
import com.android.car.launcher.feature.media.ui.MediaRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaActivity : LifeCycleLogger() {
    private val viewModel by viewModels<MediaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MediaRoute(
                    viewModel = viewModel,
                    onBack = ::finish,
                )
            }
        }
    }
}
