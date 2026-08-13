package com.miniivi.maps.contract

object MapPreviewContract {
    const val MAPS_PACKAGE = "com.miniivi.maps"
    const val MAP_ACTIVITY = "com.miniivi.maps.MapActivity"
    const val PREVIEW_SERVICE = "com.miniivi.maps.preview.MapPreviewService"
    const val BIND_PREVIEW_PERMISSION = "com.miniivi.maps.permission.BIND_MAP_PREVIEW"
    const val SURFACE_PACKAGE_KEY = "com.miniivi.maps.extra.SURFACE_PACKAGE"

    const val STATE_LOCATING = 1
    const val STATE_READY = 2
    const val STATE_LOCATION_UNAVAILABLE = 3
    const val STATE_TILE_UNAVAILABLE = 4
    const val STATE_LAST_KNOWN = 5

    const val ERROR_UNSUPPORTED_PLATFORM = 1
    const val ERROR_INVALID_HOST = 2
    const val ERROR_RENDERER_UNAVAILABLE = 3
}
