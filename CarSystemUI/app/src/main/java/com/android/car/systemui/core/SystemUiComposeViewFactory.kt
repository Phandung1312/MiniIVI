package com.android.car.systemui.core

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.car.systemui.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemUiComposeViewFactory @Inject constructor(
    @ApplicationContext context: Context,
    private val lifecycleOwner: SystemUiLifecycleOwner,
) {
    private val themedContext = ContextThemeWrapper(context, R.style.Theme_CarSystemUI)

    fun create(): ComposeView = ComposeView(themedContext).also { view ->
        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    }
}
