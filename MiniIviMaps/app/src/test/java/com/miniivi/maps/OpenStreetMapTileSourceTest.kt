package com.miniivi.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenStreetMapTileSourceTest {
    @Test
    fun tileUrlUsesOpenStreetMapHttpsEndpoint() {
        assertEquals(
            "https://tile.openstreetmap.org/15/26091/15349.png",
            OpenStreetMapTileSource.tileUrl(15, 26091, 15349),
        )
    }

    @Test
    fun worldCoordinatesRoundTrip() {
        val latitude = 10.7769
        val longitude = 106.7009
        val zoom = 15

        assertEquals(
            longitude,
            OpenStreetMapTileSource.longitude(
                OpenStreetMapTileSource.worldX(longitude, zoom),
                zoom,
            ),
            0.000001,
        )
        assertEquals(
            latitude,
            OpenStreetMapTileSource.latitude(
                OpenStreetMapTileSource.worldY(latitude, zoom),
                zoom,
            ),
            0.000001,
        )
    }
}
