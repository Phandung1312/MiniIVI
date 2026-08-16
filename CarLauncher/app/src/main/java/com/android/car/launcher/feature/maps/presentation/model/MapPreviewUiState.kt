package com.android.car.launcher.feature.maps.presentation.model

internal enum class MapPreviewUiState {
    CONNECTING,
    LOCATING,
    READY,
    LAST_KNOWN,
    LOCATION_UNAVAILABLE,
    TILE_UNAVAILABLE,
    UNAVAILABLE,
}
