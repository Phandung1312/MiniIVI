package com.miniivi.maps

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

internal object OpenStreetMapTileSource {
    const val TILE_SIZE = 256
    const val MIN_ZOOM = 3
    const val MAX_ZOOM = 18

    fun tileUrl(zoom: Int, x: Int, y: Int): String =
        "https://tile.openstreetmap.org/$zoom/$x/$y.png"

    fun worldX(longitude: Double, zoom: Int): Double =
        (longitude + 180.0) / 360.0 * worldSize(zoom)

    fun worldY(latitude: Double, zoom: Int): Double {
        val clampedLatitude = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
        val latitudeRadians = Math.toRadians(clampedLatitude)
        val normalized = (1.0 - ln(
            (1.0 + sin(latitudeRadians)) / (1.0 - sin(latitudeRadians)),
        ) / (2.0 * PI)) / 2.0
        return normalized * worldSize(zoom)
    }

    fun longitude(worldX: Double, zoom: Int): Double =
        worldX / worldSize(zoom) * 360.0 - 180.0

    fun latitude(worldY: Double, zoom: Int): Double {
        val mercator = PI * (1.0 - 2.0 * worldY / worldSize(zoom))
        return Math.toDegrees(atan((exp(mercator) - exp(-mercator)) / 2.0))
    }

    fun worldSize(zoom: Int): Double = TILE_SIZE.toDouble() * (1 shl zoom)

    private const val MAX_LATITUDE = 85.05112878
}
