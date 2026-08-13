package com.miniivi.maps.preview

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.view.Display
import android.view.SurfaceControlViewHost
import android.view.View
import androidx.annotation.RequiresApi
import com.miniivi.maps.contract.MapPreviewContract

internal interface PreviewSurface {
    fun surfacePackage(): Bundle
    fun resize(width: Int, height: Int)
    fun release()
}

internal object PreviewSurfaceFactory {
    @RequiresApi(29)
    fun create(
        context: Context,
        display: Display,
        hostToken: IBinder,
        view: View,
        width: Int,
        height: Int,
    ): PreviewSurface = Api29PreviewSurface(context, display, hostToken, view, width, height)
}

@RequiresApi(29)
private class Api29PreviewSurface(
    context: Context,
    display: Display,
    hostToken: IBinder,
    view: View,
    width: Int,
    height: Int,
) : PreviewSurface {
    private val viewHost = SurfaceControlViewHost(context, display, hostToken).apply {
        setView(view, width, height)
    }

    override fun surfacePackage(): Bundle = Bundle().apply {
        putParcelable(MapPreviewContract.SURFACE_PACKAGE_KEY, viewHost.surfacePackage)
    }

    override fun resize(width: Int, height: Int) {
        viewHost.relayout(width, height)
    }

    override fun release() {
        viewHost.release()
    }
}
