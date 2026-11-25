package com.example.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class ArSkyPolar(
    val azimuthDeg: Float,
    val elevationDeg: Float,
    val slantRangeKm: Double
)

data class ArScreenProjection(
    val screenX: Float,
    val screenY: Float,
    val deltaAzimuthDeg: Float,
    val deltaElevationDeg: Float,
    val isInFov: Boolean,
    val polar: ArSkyPolar
)

/**
 * Computes true East-North-Up (ENU) horizon azimuth, elevation angle, and slant range
 * from an observer's Earth coordinates to any airborne Aircraft or orbital Satellite,
 * and projects it onto a live mobile camera viewport given camera look azimuth & pitch.
 */
object ArSkyProjectionEngine {

    private const val EARTH_RADIUS_KM = 6371.0

    fun computeSkyPolar(
        obsLat: Double,
        obsLon: Double,
        obsAltKm: Double,
        tgtLat: Double,
        tgtLon: Double,
        tgtAltKm: Double
    ): ArSkyPolar {
        val phi0 = Math.toRadians(obsLat)
        val lam0 = Math.toRadians(obsLon)
        val r0 = EARTH_RADIUS_KM + obsAltKm

        val phi1 = Math.toRadians(tgtLat)
        val lam1 = Math.toRadians(tgtLon)
        val r1 = EARTH_RADIUS_KM + tgtAltKm

        // ECEF coordinates
        val x0 = r0 * cos(phi0) * cos(lam0)
        val y0 = r0 * cos(phi0) * sin(lam0)
        val z0 = r0 * sin(phi0)

        val x1 = r1 * cos(phi1) * cos(lam1)
        val y1 = r1 * cos(phi1) * sin(lam1)
        val z1 = r1 * sin(phi1)

        val dx = x1 - x0
        val dy = y1 - y0
        val dz = z1 - z0
        val slantKm = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.01)

        // Rotate ECEF difference into local East-North-Up (ENU) frame at observer
        val sinPhi = sin(phi0)
        val cosPhi = cos(phi0)
        val sinLam = sin(lam0)
        val cosLam = cos(lam0)

        val east = -sinLam * dx + cosLam * dy
        val north = -sinPhi * cosLam * dx - sinPhi * sinLam * dy + cosPhi * dz
        val up = cosPhi * cosLam * dx + cosPhi * sinLam * dy + sinPhi * dz

        val horiz = sqrt(east * east + north * north).coerceAtLeast(1e-6)
        val azDeg = (((Math.toDegrees(atan2(east, north)).toFloat()) % 360f) + 360f) % 360f
        val elDeg = Math.toDegrees(atan2(up, horiz)).toFloat()

        return ArSkyPolar(
            azimuthDeg = azDeg,
            elevationDeg = elDeg,
            slantRangeKm = slantKm
        )
    }

    fun shortestAngleDiffDeg(targetDeg: Float, currentDeg: Float): Float {
        var diff = (targetDeg - currentDeg) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    fun projectToCameraViewport(
        obsLat: Double,
        obsLon: Double,
        obsAltKm: Double,
        tgtLat: Double,
        tgtLon: Double,
        tgtAltKm: Double,
        camAzimuthDeg: Float,
        camElevationDeg: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        hFovDeg: Float = 64f,
        vFovDeg: Float = 50f
    ): ArScreenProjection {
        val polar = computeSkyPolar(obsLat, obsLon, obsAltKm, tgtLat, tgtLon, tgtAltKm)
        val dAz = shortestAngleDiffDeg(polar.azimuthDeg, camAzimuthDeg)
        val dEl = polar.elevationDeg - camElevationDeg

        val inFov = abs(dAz) <= (hFovDeg * 0.56f) && abs(dEl) <= (vFovDeg * 0.56f)

        val halfW = viewportWidth * 0.5f
        val halfH = viewportHeight * 0.5f
        val tanHalfH = tan(Math.toRadians(hFovDeg * 0.5).toFloat()).coerceAtLeast(0.1f)
        val tanHalfV = tan(Math.toRadians(vFovDeg * 0.5).toFloat()).coerceAtLeast(0.1f)

        val rawNormX = tan(Math.toRadians(dAz.coerceIn(-75f, 75f).toDouble()).toFloat()) / tanHalfH
        val rawNormY = -tan(Math.toRadians(dEl.coerceIn(-75f, 75f).toDouble()).toFloat()) / tanHalfV

        val sx = (halfW + rawNormX * halfW).coerceIn(28f, (viewportWidth - 28f).coerceAtLeast(28f))
        val sy = (halfH + rawNormY * halfH).coerceIn(48f, (viewportHeight - 48f).coerceAtLeast(48f))

        return ArScreenProjection(
            screenX = sx,
            screenY = sy,
            deltaAzimuthDeg = dAz,
            deltaElevationDeg = dEl,
            isInFov = inFov,
            polar = polar
        )
    }
}
