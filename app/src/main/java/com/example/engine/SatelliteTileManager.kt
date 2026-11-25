package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.data.remote.CallidusRetrofitProvider
import com.example.domain.model.GlobeImageryStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

data class SatelliteTileKey(
    val z: Int,
    val x: Int,
    val y: Int
) {
    val id: String get() = "$z/$x/$y"
}

data class TileGeoBounds(
    val latNorth: Double,
    val latSouth: Double,
    val lonWest: Double,
    val lonEast: Double
)

/**
 * Streams and caches real-world Standard, Satellite, and Terrain imagery tiles
 * (ArcGIS World Street Map, World Imagery, and World Topo Relief)
 * and computes Web Mercator tile coordinates for spherical 3D globe texturing.
 */
class SatelliteTileManager(
    private val scope: CoroutineScope
) {
    private val cache = object : LruCache<String, Bitmap>(180) {}
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val failedTimestamps = ConcurrentHashMap<String, Long>()

    private val _tileRevision = MutableStateFlow(0)
    val tileRevision: StateFlow<Int> = _tileRevision.asStateFlow()

    private var basePrewarmJob: Job? = null

    init {
        prewarmGlobalBaseTiles(GlobeImageryStyle.SATELLITE)
    }

    fun prewarmGlobalBaseTiles(style: GlobeImageryStyle) {
        basePrewarmJob?.cancel()
        basePrewarmJob = scope.launch(Dispatchers.IO) {
            // 4x4 = 16 tiles cover the entire planet at zoom level 2
            for (x in 0..3) {
                for (y in 0..3) {
                    requestTile(SatelliteTileKey(2, x, y), style)
                }
            }
        }
    }

    fun getTileBitmap(
        key: SatelliteTileKey,
        style: GlobeImageryStyle = GlobeImageryStyle.SATELLITE
    ): Bitmap? {
        val cacheId = styleCacheKey(style, key)
        val bmp = cache.get(cacheId)
        if (bmp == null) {
            requestTile(key, style)
        }
        return bmp
    }

    fun getOrFallbackBaseTiles(
        style: GlobeImageryStyle = GlobeImageryStyle.SATELLITE
    ): List<Pair<SatelliteTileKey, Bitmap>> {
        val result = ArrayList<Pair<SatelliteTileKey, Bitmap>>(16)
        for (x in 0..3) {
            for (y in 0..3) {
                val key = SatelliteTileKey(2, x, y)
                val cacheId = styleCacheKey(style, key)
                val bmp = cache.get(cacheId)
                if (bmp != null) {
                    result.add(key to bmp)
                } else {
                    requestTile(key, style)
                }
            }
        }
        return result
    }

    fun requestTile(
        key: SatelliteTileKey,
        style: GlobeImageryStyle = GlobeImageryStyle.SATELLITE
    ) {
        val cacheId = styleCacheKey(style, key)
        if (cache.get(cacheId) != null) return
        val lastFail = failedTimestamps[cacheId]
        if (lastFail != null && System.currentTimeMillis() - lastFail < 15_000L) return
        if (!inFlight.add(cacheId)) return

        scope.launch(Dispatchers.IO) {
            try {
                val url = buildTileUrl(style, key)
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "CallidusAndroidApp/1.0")
                    .build()
                CallidusRetrofitProvider.okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bytes = resp.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val opts = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.RGB_565
                            }
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                            if (bmp != null) {
                                cache.put(cacheId, bmp)
                                _tileRevision.value = _tileRevision.value + 1
                            } else {
                                failedTimestamps[cacheId] = System.currentTimeMillis()
                            }
                        }
                    } else {
                        failedTimestamps[cacheId] = System.currentTimeMillis()
                    }
                }
            } catch (_: Throwable) {
                failedTimestamps[cacheId] = System.currentTimeMillis()
            } finally {
                inFlight.remove(cacheId)
            }
        }
    }

    companion object {
        fun styleCacheKey(style: GlobeImageryStyle, key: SatelliteTileKey): String =
            "${style.id}/${key.z}/${key.x}/${key.y}"

        fun buildTileUrl(style: GlobeImageryStyle, key: SatelliteTileKey): String =
            style.tileUrlTemplate
                .replace("{z}", key.z.toString())
                .replace("{y}", key.y.toString())
                .replace("{x}", key.x.toString())

        fun detailZoomForCamera(cameraZoom: Float): Int = when {
            cameraZoom >= 20.0f -> 16
            cameraZoom >= 15.0f -> 14
            cameraZoom >= 11.0f -> 12
            cameraZoom >= 8.0f -> 10
            cameraZoom >= 5.2f -> 8
            cameraZoom >= 3.2f -> 6
            cameraZoom >= 1.85f -> 4
            else -> 2
        }

        fun tileBounds(key: SatelliteTileKey): TileGeoBounds {
            val n = (1 shl key.z).toDouble()
            val lonWest = key.x / n * 360.0 - 180.0
            val lonEast = (key.x + 1) / n * 360.0 - 180.0
            val latNorth = tileYToLat(key.y, n)
            val latSouth = tileYToLat(key.y + 1, n)
            return TileGeoBounds(latNorth, latSouth, lonWest, lonEast)
        }

        fun lonToTileX(lon: Double, z: Int): Int {
            val n = 1 shl z
            val norm = ((Globe3DEngine.normalizeLon(lon) + 180.0) / 360.0) * n
            return floor(norm).toInt().coerceIn(0, n - 1)
        }

        fun latToTileY(lat: Double, z: Int): Int {
            val clamped = lat.coerceIn(-85.0511, 85.0511)
            val rad = Math.toRadians(clamped)
            val n = 1 shl z
            val y = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * n
            return floor(y).toInt().coerceIn(0, n - 1)
        }

        private fun tileYToLat(y: Int, n: Double): Double {
            val mercN = PI * (1.0 - 2.0 * y / n)
            return Math.toDegrees(atan(sinh(mercN)))
        }

        /**
         * Returns the surrounding grid of detail tiles centered around the camera target.
         */
        fun detailTilesAroundCamera(centerLat: Double, centerLon: Double, cameraZoom: Float): List<SatelliteTileKey> {
            val z = detailZoomForCamera(cameraZoom)
            if (z <= 2) return emptyList()
            val n = 1 shl z
            val cx = lonToTileX(centerLon, z)
            val cy = latToTileY(centerLat, z)
            val radiusX = if (z == 4) 3 else 2
            val radiusY = if (z == 4) 2 else 2

            val list = ArrayList<SatelliteTileKey>((2 * radiusX + 1) * (2 * radiusY + 1))
            for (dy in -radiusY..radiusY) {
                val y = cy + dy
                if (y !in 0 until n) continue
                for (dx in -radiusX..radiusX) {
                    val x = ((cx + dx) % n + n) % n
                    list.add(SatelliteTileKey(z, x, y))
                }
            }
            return list
        }
    }
}
