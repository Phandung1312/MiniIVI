package com.miniivi.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.LruCache
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.roundToInt

internal enum class MapAppearance {
    LIGHT,
    DASHBOARD_DARK,
}

internal object MapAppearanceTransform {
    private const val SATURATION = 0.25f
    private const val SCALE = 0.65f
    private const val BASE_RED = 32f
    private const val BASE_GREEN = 26f
    private const val BASE_BLUE = 41f

    fun dashboardDarkColor(argb: Int): Int {
        val red = ((argb shr 16) and 0xFF).toFloat()
        val green = ((argb shr 8) and 0xFF).toFloat()
        val blue = (argb and 0xFF).toFloat()
        val luminance = red * 0.2126f + green * 0.7152f + blue * 0.0722f
        fun transform(channel: Float, base: Float): Int =
            (base + SCALE * (255f - (luminance * (1f - SATURATION) + channel * SATURATION)))
                .roundToInt()
                .coerceIn(0, 255)

        return (0xFF shl 24) or
            (transform(red, BASE_RED) shl 16) or
            (transform(green, BASE_GREEN) shl 8) or
            transform(blue, BASE_BLUE)
    }

    fun colorFilter(): ColorMatrixColorFilter {
        val luminanceRed = 0.2126f
        val luminanceGreen = 0.7152f
        val luminanceBlue = 0.0722f
        val redScale = -SCALE * (luminanceRed * (1f - SATURATION) + SATURATION)
        val greenScale = -SCALE * (luminanceGreen * (1f - SATURATION))
        val blueScale = -SCALE * (luminanceBlue * (1f - SATURATION))
        val redOffset = BASE_RED + SCALE * 255f
        val greenOffset = BASE_GREEN + SCALE * 255f
        val blueOffset = BASE_BLUE + SCALE * 255f
        return ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    redScale, greenScale, blueScale, 0f, redOffset,
                    -SCALE * (luminanceRed * (1f - SATURATION)),
                    -SCALE * (luminanceGreen * (1f - SATURATION) + SATURATION),
                    -SCALE * (luminanceBlue * (1f - SATURATION)), 0f, greenOffset,
                    -SCALE * (luminanceRed * (1f - SATURATION)),
                    -SCALE * (luminanceGreen * (1f - SATURATION)),
                    -SCALE * (luminanceBlue * (1f - SATURATION) + SATURATION), 0f, blueOffset,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
}

internal class OpenStreetMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var appearance: MapAppearance = MapAppearance.LIGHT
        set(value) {
            field = value
            updateAppearance()
            invalidate()
        }
    var onLoadingChanged: ((Boolean) -> Unit)? = null
    var onInitialLoadFailed: (() -> Unit)? = null
    var onUserGesture: (() -> Unit)? = null
    var gesturesEnabled: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val diskCache = File(context.cacheDir, "osm_tiles").apply { mkdirs() }
    private val pendingTiles = mutableSetOf<String>()
    private val failedTiles = mutableSetOf<String>()
    private var visibleTiles = emptySet<String>()
    private var zoom = DEFAULT_ZOOM
    private var centerX = OpenStreetMapTileSource.worldX(DEFAULT_LONGITUDE, zoom)
    private var centerY = OpenStreetMapTileSource.worldY(DEFAULT_LATITUDE, zoom)
    private var markerLatitude: Double? = null
    private var markerLongitude: Double? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastLoadingState: Boolean? = null
    private var failureReported = false
    private var scaleAccumulator = 1f
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(224, 222, 213)
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(198, 195, 184)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(190, 54, 92)
        style = Paint.Style.FILL
    }
    private val markerOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val attributionBackground = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val attributionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(45, 45, 45)
        textSize = 16f * resources.displayMetrics.scaledDensity
    }

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onDoubleTap(event: MotionEvent): Boolean {
                onUserGesture?.invoke()
                setZoom(zoom + 1)
                return true
            }
        },
    )
    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                onUserGesture?.invoke()
                scaleAccumulator = 1f
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleAccumulator *= detector.scaleFactor
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                when {
                    scaleAccumulator > 1.15f -> setZoom(zoom + 1)
                    scaleAccumulator < 0.85f -> setZoom(zoom - 1)
                }
            }
        },
    )

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isFocusable = true
        contentDescription = context.getString(R.string.app_name)
        updateAppearance()
    }

    fun showLocation(latitude: Double, longitude: Double) {
        markerLatitude = latitude
        markerLongitude = longitude
        zoom = LOCATION_ZOOM
        centerX = OpenStreetMapTileSource.worldX(longitude, zoom)
        centerY = OpenStreetMapTileSource.worldY(latitude, zoom)
        resetVisibleRequests()
    }

    fun retryFailedTiles() {
        failedTiles.clear()
        failureReported = false
        lastLoadingState = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(
            if (appearance == MapAppearance.DASHBOARD_DARK) {
                Color.rgb(32, 26, 41)
            } else {
                Color.rgb(232, 229, 218)
            },
        )
        val tileSize = OpenStreetMapTileSource.TILE_SIZE.toDouble()
        val tileCount = 1 shl zoom
        val worldSize = OpenStreetMapTileSource.worldSize(zoom)
        centerX = wrap(centerX, worldSize)
        centerY = centerY.coerceIn(0.0, worldSize)
        val left = centerX - width / 2.0
        val top = centerY - height / 2.0
        val firstTileX = floor(left / tileSize).toInt()
        val lastTileX = floor((left + width) / tileSize).toInt()
        val firstTileY = floor(top / tileSize).toInt()
        val lastTileY = floor((top + height) / tileSize).toInt()
        val newVisibleTiles = mutableSetOf<String>()

        for (tileY in firstTileY..lastTileY) {
            if (tileY !in 0 until tileCount) continue
            for (unwrappedTileX in firstTileX..lastTileX) {
                val tileX = floorMod(unwrappedTileX, tileCount)
                val key = tileKey(zoom, tileX, tileY)
                newVisibleTiles += key
                val destination = RectF(
                    (unwrappedTileX * tileSize - left).toFloat(),
                    (tileY * tileSize - top).toFloat(),
                    ((unwrappedTileX + 1) * tileSize - left).toFloat(),
                    ((tileY + 1) * tileSize - top).toFloat(),
                )
                val bitmap = memoryCache.get(key)
                if (bitmap == null) {
                    canvas.drawRect(destination, placeholderPaint)
                    canvas.drawRect(destination, gridPaint)
                    requestTile(zoom, tileX, tileY, key)
                } else {
                    canvas.drawBitmap(bitmap, null, destination, tilePaint)
                }
            }
        }
        visibleTiles = newVisibleTiles
        drawMarker(canvas, left, top, worldSize)
        drawAttribution(canvas)
        updateLoadingState()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!gesturesEnabled) return false
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress && event.pointerCount == 1) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }

                MotionEvent.ACTION_MOVE -> {
                    onUserGesture?.invoke()
                    centerX -= event.x - lastTouchX
                    centerY -= event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        scope.cancel()
        super.onDetachedFromWindow()
    }

    private fun setZoom(requestedZoom: Int) {
        val newZoom = requestedZoom.coerceIn(
            OpenStreetMapTileSource.MIN_ZOOM,
            OpenStreetMapTileSource.MAX_ZOOM,
        )
        if (newZoom == zoom) return
        val latitude = OpenStreetMapTileSource.latitude(centerY, zoom)
        val longitude = OpenStreetMapTileSource.longitude(centerX, zoom)
        zoom = newZoom
        centerX = OpenStreetMapTileSource.worldX(longitude, zoom)
        centerY = OpenStreetMapTileSource.worldY(latitude, zoom)
        resetVisibleRequests()
    }

    private fun resetVisibleRequests() {
        failureReported = false
        lastLoadingState = null
        invalidate()
    }

    private fun requestTile(zoom: Int, x: Int, y: Int, key: String) {
        if (key in pendingTiles || key in failedTiles) return
        pendingTiles += key
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { loadTile(zoom, x, y, key) }
            pendingTiles -= key
            if (bitmap == null) {
                failedTiles += key
            } else {
                failedTiles -= key
                memoryCache.put(key, bitmap)
            }
            invalidate()
        }
    }

    private fun loadTile(zoom: Int, x: Int, y: Int, key: String): Bitmap? {
        val cacheFile = File(diskCache, "$key.png")
        if (cacheFile.isFile) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { return it }
            cacheFile.delete()
        }

        return runCatching {
            val connection = URL(OpenStreetMapTileSource.tileUrl(zoom, x, y))
                .openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "image/png")
            connection.useCaches = true
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                val bytes = connection.inputStream.use { it.readBytes() }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                runCatching { cacheFile.writeBytes(bytes) }
                bitmap
            } finally {
                connection.disconnect()
            }
        }.onFailure {
            Log.w(TAG, "Unable to load OSM tile $key", it)
        }.getOrNull()
    }

    private fun updateLoadingState() {
        val hasVisibleTile = visibleTiles.any { memoryCache.get(it) != null }
        val hasPendingTile = visibleTiles.any { it in pendingTiles }
        val loading = !hasVisibleTile && hasPendingTile
        if (lastLoadingState != loading) {
            lastLoadingState = loading
            onLoadingChanged?.invoke(loading)
        }
        val allFailed = visibleTiles.isNotEmpty() &&
            visibleTiles.all { it in failedTiles } &&
            !hasPendingTile
        if (allFailed && !failureReported) {
            failureReported = true
            onInitialLoadFailed?.invoke()
        }
    }

    private fun drawMarker(canvas: Canvas, left: Double, top: Double, worldSize: Double) {
        val latitude = markerLatitude ?: return
        val longitude = markerLongitude ?: return
        var markerX = OpenStreetMapTileSource.worldX(longitude, zoom)
        val markerY = OpenStreetMapTileSource.worldY(latitude, zoom)
        while (markerX - centerX > worldSize / 2.0) markerX -= worldSize
        while (centerX - markerX > worldSize / 2.0) markerX += worldSize
        val x = (markerX - left).toFloat()
        val y = (markerY - top).toFloat()
        canvas.drawCircle(x, y, MARKER_RADIUS, markerOutlinePaint)
        canvas.drawCircle(x, y, MARKER_RADIUS, markerPaint)
    }

    private fun drawAttribution(canvas: Canvas) {
        val text = "© OpenStreetMap contributors"
        val padding = 10f
        val textWidth = attributionPaint.measureText(text)
        val textHeight = attributionPaint.fontMetrics.run { bottom - top }
        val left = width - textWidth - padding * 2
        val top = height - textHeight - padding * 2
        canvas.drawRect(left, top, width.toFloat(), height.toFloat(), attributionBackground)
        canvas.drawText(
            text,
            left + padding,
            height - padding - attributionPaint.fontMetrics.bottom,
            attributionPaint,
        )
    }

    private fun updateAppearance() {
        val dark = appearance == MapAppearance.DASHBOARD_DARK
        tilePaint.colorFilter = if (dark) MapAppearanceTransform.colorFilter() else null
        placeholderPaint.color = if (dark) Color.rgb(39, 33, 50) else Color.rgb(224, 222, 213)
        gridPaint.color = if (dark) Color.rgb(58, 49, 71) else Color.rgb(198, 195, 184)
        markerPaint.color = if (dark) Color.rgb(169, 140, 245) else Color.rgb(190, 54, 92)
        markerOutlinePaint.color = if (dark) Color.rgb(32, 26, 41) else Color.WHITE
        attributionBackground.color = if (dark) Color.argb(190, 32, 26, 41) else Color.argb(210, 255, 255, 255)
        attributionPaint.color = if (dark) Color.rgb(248, 244, 252) else Color.rgb(45, 45, 45)
    }

    private fun tileKey(zoom: Int, x: Int, y: Int): String = "${zoom}_${x}_$y"

    private fun floorMod(value: Int, modulus: Int): Int =
        ((value % modulus) + modulus) % modulus

    private fun wrap(value: Double, modulus: Double): Double =
        ((value % modulus) + modulus) % modulus

    private companion object {
        const val TAG = "MiniIviMaps"
        const val USER_AGENT = "MiniIVIMaps/1.0"
        const val MEMORY_CACHE_BYTES = 24 * 1024 * 1024
        const val CONNECT_TIMEOUT_MILLIS = 8_000
        const val READ_TIMEOUT_MILLIS = 10_000
        const val DEFAULT_ZOOM = 6
        const val LOCATION_ZOOM = 15
        const val DEFAULT_LATITUDE = 16.0
        const val DEFAULT_LONGITUDE = 106.0
        const val MARKER_RADIUS = 12f
    }
}
