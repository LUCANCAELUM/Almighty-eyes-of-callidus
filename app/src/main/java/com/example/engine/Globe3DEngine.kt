package com.example.engine

import com.example.domain.model.CameraState3D
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-6f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 1f)
    }
}

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depthZ: Float,
    val isVisible: Boolean,
    val perspectiveScale: Float,
    val camVec: Vec3
)

/**
 * Full 3D Spherical + Perspective Projection Engine.
 * Supports arbitrary yaw/pitch globe orientation, oblique 3D camera tilt (pitchDeg),
 * banking roll (rollDeg), altitude extrusion, and real-time UTC solar terminator math.
 */
class Globe3DEngine(
    private val viewportWidth: Float,
    private val viewportHeight: Float,
    private val camera: CameraState3D
) {
    private val minDim = minOf(viewportWidth, viewportHeight).coerceAtLeast(1f)
    val baseRadius: Float = minDim * 0.37f * camera.zoom

    // Camera perspective parameters: tighter focal ratio gives dramatic 3D depth on closeups
    private val cameraDistance: Float = baseRadius * (3.2f / camera.zoom.coerceAtLeast(1f).let { 1f + (it - 1f) * 0.14f })
        .coerceAtLeast(baseRadius * 1.45f)
    private val focalLength: Float = cameraDistance * 0.96f

    val centerX: Float = viewportWidth * 0.5f
    // Shift globe center slightly lower when tilted obliquely so the upper horizon shows dramatic 3D space!
    val centerY: Float = viewportHeight * (0.50f + (camera.pitchDeg / 68f) * 0.08f)

    private val latRad = Math.toRadians(camera.centerLat).toFloat()
    private val lonRad = Math.toRadians(camera.centerLon).toFloat()
    private val tiltRad = Math.toRadians(camera.pitchDeg.toDouble()).toFloat()
    private val rollRad = Math.toRadians(camera.rollDeg.toDouble()).toFloat()

    private val cosLat = cos(latRad)
    private val sinLat = sin(latRad)
    private val cosTilt = cos(tiltRad)
    private val sinTilt = sin(tiltRad)
    private val cosRoll = cos(rollRad)
    private val sinRoll = sin(rollRad)

    // Subsolar vector in world space for real-time UTC Day/Night shading
    val sunWorldVec: Vec3 = computeSunVectorWorld()

    /**
     * Converts geodetic (lat, lon) + altitudeKm to 3D world unit sphere (or scaled radius).
     * World frame: +Z = (lat=0, lon=0), +X = (lat=0, lon=90E), +Y = North Pole.
     */
    fun geoToWorldUnit(lat: Double, lon: Double): Vec3 {
        val phi = Math.toRadians(lat)
        val lambda = Math.toRadians(lon)
        val cosPhi = cos(phi)
        return Vec3(
            x = (cosPhi * sin(lambda)).toFloat(),
            y = sin(phi).toFloat(),
            z = (cosPhi * cos(lambda)).toFloat()
        )
    }

    /**
     * Rotates a 3D world vector into camera space (+Z toward viewer, +Y up, +X right).
     */
    fun worldToCamera(world: Vec3): Vec3 {
        // 1. Yaw around Y by -lonRad so target longitude faces +Z
        val dLon = lonRad
        val cosL = cos(dLon)
        val sinL = sin(dLon)
        val x1 = world.x * cosL - world.z * sinL
        val z1 = world.x * sinL + world.z * cosL
        val y1 = world.y

        // 2. Pitch around X by +latRad so target latitude faces +Z
        val y2 = y1 * cosLat - z1 * sinLat
        val z2 = y1 * sinLat + z1 * cosLat
        val x2 = x1

        // 3. Oblique 3D Camera Tilt around X by -tiltRad (tilts globe back to reveal 3D horizon!)
        val y3 = y2 * cosTilt + z2 * sinTilt
        val z3 = -y2 * sinTilt + z2 * cosTilt
        val x3 = x2

        // 4. Camera Roll around Z (banking during 3D flight transitions!)
        val x4 = x3 * cosRoll - y3 * sinRoll
        val y4 = x3 * sinRoll + y3 * cosRoll
        val z4 = z3

        return Vec3(x4, y4, z4)
    }

    /**
     * Projects (lat, lon, altitudeRadiusMultiplier) into 2D screen coordinates with 3D perspective.
     * radiusScale = 1.0f for Earth surface, > 1.0f for elevated aircraft/satellites.
     */
    fun project(lat: Double, lon: Double, radiusScale: Float = 1.0f): ProjectedPoint {
        val unitWorld = geoToWorldUnit(lat, lon)
        val camUnit = worldToCamera(unitWorld)
        val camPos = camUnit * (baseRadius * radiusScale)

        // When tilted obliquely, push the tilted surface point down slightly to pivot around camera focus
        val zDist = (cameraDistance - camPos.z).coerceAtLeast(baseRadius * 0.25f)
        val pScale = focalLength / zDist

        val sx = centerX + camPos.x * pScale
        val sy = centerY - camPos.y * pScale

        // Horizon visibility test: point is visible if it's in front of Earth's geometric horizon limb
        // For surface points (radiusScale == 1), camUnit.z > -0.04 gives crisp limb wrap;
        // for elevated satellites (radiusScale > 1), they remain visible around the limb until occluded by sphere!
        val minVisibleZ = if (radiusScale <= 1.001f) {
            -0.02f
        } else {
            val limbCos = -sqrt((1f - 1f / (radiusScale * radiusScale)).coerceIn(0f, 0.99f))
            limbCos
        }

        val visible = camUnit.z > minVisibleZ && zDist > baseRadius * 0.35f

        return ProjectedPoint(
            screenX = sx,
            screenY = sy,
            depthZ = camPos.z,
            isVisible = visible,
            perspectiveScale = pScale,
            camVec = camUnit
        )
    }

    /**
     * Projects an arbitrary world-space 3D vector scaled by radiusScale.
     */
    fun projectWorldVec(unitWorld: Vec3, radiusScale: Float = 1.0f): ProjectedPoint {
        val camUnit = worldToCamera(unitWorld)
        val camPos = camUnit * (baseRadius * radiusScale)
        val zDist = (cameraDistance - camPos.z).coerceAtLeast(baseRadius * 0.25f)
        val pScale = focalLength / zDist
        return ProjectedPoint(
            screenX = centerX + camPos.x * pScale,
            screenY = centerY - camPos.y * pScale,
            depthZ = camPos.z,
            isVisible = camUnit.z > -0.05f,
            perspectiveScale = pScale,
            camVec = camUnit
        )
    }

    /**
     * Computes local East & North tangent unit vectors at (lat, lon) in World space
     * so we can render true 3D surface-tangent shockwave rings & aircraft orientation arrows.
     */
    fun tangentFrameWorld(lat: Double, lon: Double): Triple<Vec3, Vec3, Vec3> {
        val up = geoToWorldUnit(lat, lon).normalized()
        val northPole = Vec3(0f, 1f, 0f)
        val east = northPole.cross(up).let {
            if (it.length() < 1e-4f) Vec3(1f, 0f, 0f) else it.normalized()
        }
        val north = up.cross(east).normalized()
        return Triple(east, north, up)
    }

    /**
     * Returns solar illumination factor in [0f..1f] where 1.0 = noon, 0.0 = deep night.
     */
    fun solarIlluminationAt(lat: Double, lon: Double): Float {
        val n = geoToWorldUnit(lat, lon)
        val d = n.dot(sunWorldVec)
        // Smooth twilight terminator band across [-0.15, +0.25]
        return ((d + 0.18f) / 0.43f).coerceIn(0f, 1f)
    }

    /**
     * Inverse raycast from screen tap (x, y) onto the 3D unit sphere (returns Lat/Lon if hit).
     */
    fun screenToGeo(screenX: Float, screenY: Float): GeoCoord? {
        val nx = (screenX - centerX) / (baseRadius * (focalLength / cameraDistance))
        val ny = -(screenY - centerY) / (baseRadius * (focalLength / cameraDistance))
        val r2 = nx * nx + ny * ny
        if (r2 > 1.0f) return null
        val nz = sqrt((1.0f - r2).coerceAtLeast(0f))

        // Un-roll
        val rx = nx * cosRoll + ny * sinRoll
        val ry = -nx * sinRoll + ny * cosRoll
        val rz = nz

        // Un-tilt
        val ty = ry * cosTilt - rz * sinTilt
        val tz = ry * sinTilt + rz * cosTilt
        val tx = rx

        // Un-pitch (around X by -latRad)
        val py = ty * cosLat + tz * sinLat
        val pz = -ty * sinLat + tz * cosLat
        val px = tx

        // Un-yaw (around Y by +lonRad)
        val wx = px * cos(lonRad) + pz * sin(lonRad)
        val wz = -px * sin(lonRad) + pz * cos(lonRad)
        val wy = py

        val lat = Math.toDegrees(kotlin.math.asin(wy.coerceIn(-1f, 1f).toDouble()))
        val lon = Math.toDegrees(atan2(wx.toDouble(), wz.toDouble()))
        return GeoCoord(lat, lon)
    }

    companion object {
        fun computeSunVectorWorld(): Vec3 {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val utcHours = cal.get(Calendar.HOUR_OF_DAY) +
                cal.get(Calendar.MINUTE) / 60.0 +
                cal.get(Calendar.SECOND) / 3600.0

            // Approximate solar declination (degrees)
            val declDeg = 23.44 * sin(2.0 * PI * (dayOfYear - 81) / 365.25)
            // Subsolar longitude: 12:00 UTC -> 0 deg lon; +15 deg west per hour after 12:00
            val subsolarLon = ((12.0 - utcHours) * 15.0)

            val phi = Math.toRadians(declDeg)
            val lambda = Math.toRadians(subsolarLon)
            val cosPhi = cos(phi)
            return Vec3(
                x = (cosPhi * sin(lambda)).toFloat(),
                y = sin(phi).toFloat(),
                z = (cosPhi * cos(lambda)).toFloat()
            ).normalized()
        }

        fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
            return r * c
        }

        fun normalizeLon(lon: Double): Double {
            var res = lon
            while (res > 180.0) res -= 360.0
            while (res < -180.0) res += 360.0
            return res
        }

        fun shortestLonDelta(fromLon: Double, toLon: Double): Double {
            var d = toLon - fromLon
            while (d > 180.0) d -= 360.0
            while (d < -180.0) d += 360.0
            return d
        }

        fun greatCircleArc(a: GeoCoord, b: GeoCoord, steps: Int = 16): List<GeoCoord> {
            val lat1 = Math.toRadians(a.lat)
            val lon1 = Math.toRadians(a.lon)
            val lat2 = Math.toRadians(b.lat)
            val lon2 = Math.toRadians(b.lon)

            val x1 = cos(lat1) * cos(lon1)
            val y1 = cos(lat1) * sin(lon1)
            val z1 = sin(lat1)

            val x2 = cos(lat2) * cos(lon2)
            val y2 = cos(lat2) * sin(lon2)
            val z2 = sin(lat2)

            val dot = (x1 * x2 + y1 * y2 + z1 * z2).coerceIn(-1.0, 1.0)
            val omega = acos(dot)
            if (omega < 1e-4) return listOf(a, b)

            val sinOmega = sin(omega)
            return (0..steps).map { i ->
                val t = i.toDouble() / steps
                val s1 = sin((1.0 - t) * omega) / sinOmega
                val s2 = sin(t * omega) / sinOmega
                val x = s1 * x1 + s2 * x2
                val y = s1 * y1 + s2 * y2
                val z = s1 * z1 + s2 * z2
                GeoCoord(
                    lat = Math.toDegrees(atan2(z, sqrt(x * x + y * y))),
                    lon = Math.toDegrees(atan2(y, x))
                )
            }
        }
    }
}
