package com.miniivi.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapAppearanceTransformTest {
    @Test
    fun dashboardDarkTransformMapsTileBackgroundToThemeSurface() {
        assertEquals(
            color(32, 26, 41),
            MapAppearanceTransform.dashboardDarkColor(color(255, 255, 255)),
        )
    }

    @Test
    fun dashboardDarkTransformKeepsDarkRoadDetailsReadable() {
        val transformed = MapAppearanceTransform.dashboardDarkColor(color(0, 0, 0))

        val transformedWhite = MapAppearanceTransform.dashboardDarkColor(color(255, 255, 255))
        assertTrue(luminance(transformed) > luminance(transformedWhite))
        assertTrue(channelSpread(transformed) > 0)
    }

    @Test
    fun dashboardDarkTransformReducesTileSaturation() {
        val source = color(24, 120, 232)
        val transformed = MapAppearanceTransform.dashboardDarkColor(source)

        assertTrue(channelSpread(transformed) < channelSpread(source))
    }

    private fun luminance(color: Int): Int =
        red(color) + green(color) + blue(color)

    private fun channelSpread(color: Int): Int {
        val channels = intArrayOf(red(color), green(color), blue(color))
        return channels.maxOrNull()!! - channels.minOrNull()!!
    }

    private fun color(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

    private fun red(color: Int): Int = (color shr 16) and 0xFF

    private fun green(color: Int): Int = (color shr 8) and 0xFF

    private fun blue(color: Int): Int = color and 0xFF
}
