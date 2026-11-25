package com.example.data.orbital

import com.example.domain.model.GeoPoint3D
import com.example.domain.model.SatelliteElement
import com.example.domain.model.SatelliteTarget
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-Kotlin Keplerian / SGP-inspired orbital propagator.
 * Converts CelesTrak GP orbital elements into real-time ECI -> ECEF -> Geodetic (lat, lon, altKm)
 * and generates 95-minute 3D orbital ground tracks.
 */
object OrbitalPropagator {

    private const val EARTH_RADIUS_KM = 6371.0
    private const val MU_EARTH = 398600.4418 // km^3 / s^2
    private const val TWO_PI = 2.0 * PI
    private const val DEG_TO_RAD = PI / 180.0
    private const val RAD_TO_DEG = 180.0 / PI

    fun parseEpochMillis(epochIso: String?): Long {
        if (epochIso.isNullOrBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(epochIso.take(26))
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    fun propagate(element: SatelliteElement, atTimeMs: Long): SatelliteTarget {
        val current = computePointAt(element, atTimeMs)
        // Generate full orbital track (-25 min to +70 min) in 2.5 min steps
        val track = ArrayList<GeoPoint3D>(39)
        for (step in -10..28) {
            val t = atTimeMs + step * 150_000L
            val pt = computePointAt(element, t)
            track.add(GeoPoint3D(pt.lat, pt.lon, pt.altKm))
        }
        return SatelliteTarget(
            element = element,
            lat = current.lat,
            lon = current.lon,
            altitudeKm = current.altKm,
            velocityKmS = current.velKmS,
            groundTrack = track
        )
    }

    private data class PropagatedPoint(
        val lat: Double,
        val lon: Double,
        val altKm: Double,
        val velKmS: Double
    )

    private fun computePointAt(el: SatelliteElement, timeMs: Long): PropagatedPoint {
        val dtSec = (timeMs - el.epochMs) / 1000.0
        val nRadPerSec = (el.meanMotionRevPerDay * TWO_PI) / 86400.0
        val semiMajorAxisKm = Math.cbrt(MU_EARTH / (nRadPerSec * nRadPerSec))

        val m0 = el.meanAnomalyDeg * DEG_TO_RAD
        var meanAnomaly = (m0 + nRadPerSec * dtSec) % TWO_PI
        if (meanAnomaly < 0) meanAnomaly += TWO_PI

        // Solve Kepler's Equation: M = E - e * sin(E) via Newton-Raphson
        val e = el.eccentricity.coerceIn(0.0, 0.25)
        var eccentricAnomaly = meanAnomaly
        repeat(6) {
            val f = eccentricAnomaly - e * sin(eccentricAnomaly) - meanAnomaly
            val fPrime = 1.0 - e * cos(eccentricAnomaly)
            eccentricAnomaly -= f / fPrime
        }

        val cosE = cos(eccentricAnomaly)
        val sinE = sin(eccentricAnomaly)
        val sqrtOneMinusE2 = sqrt(1.0 - e * e)
        val trueAnomaly = atan2(sqrtOneMinusE2 * sinE, cosE - e)
        val radiusKm = semiMajorAxisKm * (1.0 - e * cosE)

        // Argument of latitude in orbital plane
        val inc = el.inclinationDeg * DEG_TO_RAD
        // Subtle J2 nodal precession correction
        val j2Factor = -1.5 * 1.08263e-3 * (EARTH_RADIUS_KM / semiMajorAxisKm).let { it * it } * nRadPerSec * cos(inc)
        val raan = el.raanDeg * DEG_TO_RAD + j2Factor * dtSec
        val argPerigee = el.argPerigeeDeg * DEG_TO_RAD
        val u = argPerigee + trueAnomaly

        // Position in ECI frame
        val cosO = cos(raan)
        val sinO = sin(raan)
        val cosU = cos(u)
        val sinU = sin(u)
        val cosI = cos(inc)
        val sinI = sin(inc)

        val xEci = radiusKm * (cosO * cosU - sinO * sinU * cosI)
        val yEci = radiusKm * (sinO * cosU + cosO * sinU * cosI)
        val zEci = radiusKm * (sinU * sinI)

        // Greenwich Mean Sidereal Time (GMST) in radians
        val gmst = computeGmstRad(timeMs)
        val cosG = cos(gmst)
        val sinG = sin(gmst)

        // Rotate ECI -> ECEF
        val xEcef = cosG * xEci + sinG * yEci
        val yEcef = -sinG * xEci + cosG * yEci
        val zEcef = zEci

        val latDeg = atan2(zEcef, sqrt(xEcef * xEcef + yEcef * yEcef)) * RAD_TO_DEG
        var lonDeg = atan2(yEcef, xEcef) * RAD_TO_DEG
        while (lonDeg > 180.0) lonDeg -= 360.0
        while (lonDeg < -180.0) lonDeg += 360.0

        val altKm = (radiusKm - EARTH_RADIUS_KM).coerceAtLeast(160.0)
        // Vis-viva orbital velocity
        val velKmS = sqrt(MU_EARTH * ((2.0 / radiusKm) - (1.0 / semiMajorAxisKm)))

        return PropagatedPoint(latDeg, lonDeg, altKm, velKmS)
    }

    private fun computeGmstRad(timeMs: Long): Double {
        // Julian Date from Unix epoch
        val jd = timeMs / 86400000.0 + 2440587.5
        val d = jd - 2451545.0
        val gmstDeg = (280.46061837 + 360.98564736629 * d) % 360.0
        return ((gmstDeg + 360.0) % 360.0) * DEG_TO_RAD
    }
}
