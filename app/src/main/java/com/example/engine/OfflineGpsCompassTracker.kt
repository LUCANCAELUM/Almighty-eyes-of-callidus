package com.example.engine

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.GeoPoint3D
import com.example.ui.components.TelemetryCalculator
import java.util.Locale
import kotlin.math.abs

data class LiveGpsCompassState(
    val hasGpsFix: Boolean = false,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val speedMetersPerSec: Float = 0f,
    val accuracyMeters: Float = 0f,
    val gpsBearingDeg: Float = 0f,
    val provider: String = "STANDBY",
    val updatedAtMs: Long = 0L,
    val compassHeadingDeg: Float = 0f,
    val cameraPitchDeg: Float = 15f,
    val compassRollDeg: Float = 0f,
    val hasHardwareCompass: Boolean = false,
    val breadcrumbTrail: List<GeoPoint3D> = emptyList()
)

data class OfflineSteeringVector(
    val targetBearingDeg: Float,
    val cardinalDirection: String,
    val relativeTurnDeg: Float,
    val distanceMeters: Double,
    val steerInstruction: String
)

object OfflineSteeringCalculator {

    fun computeSteering(
        currentLat: Double,
        currentLon: Double,
        compassHeadingDeg: Float,
        destLat: Double,
        destLon: Double
    ): OfflineSteeringVector {
        val targetBearing = TelemetryCalculator.initialBearingDeg(
            currentLat,
            currentLon,
            destLat,
            destLon
        )
        val cardinal = TelemetryCalculator.compassCardinal(targetBearing)
        val relTurn = ArSkyProjectionEngine.shortestAngleDiffDeg(targetBearing, compassHeadingDeg)
        val distMeters = Globe3DEngine.haversineKm(currentLat, currentLon, destLat, destLon) * 1000.0

        val instruction = when {
            distMeters < 20.0 -> "ARRIVED AT WAYPOINT"
            abs(relTurn) <= 12f -> "HOLD COURSE STRAIGHT"
            relTurn in 12f..45f -> String.format(Locale.US, "BEAR RIGHT %.0f°", relTurn)
            relTurn > 45f && relTurn < 145f -> String.format(Locale.US, "TURN RIGHT %.0f°", relTurn)
            relTurn < -12f && relTurn >= -45f -> String.format(Locale.US, "BEAR LEFT %.0f°", -relTurn)
            relTurn < -45f && relTurn > -145f -> String.format(Locale.US, "TURN LEFT %.0f°", -relTurn)
            else -> "TURN AROUND (%.0f°)".format(Locale.US, abs(relTurn))
        }

        return OfflineSteeringVector(
            targetBearingDeg = targetBearing,
            cardinalDirection = cardinal,
            relativeTurnDeg = relTurn,
            distanceMeters = distMeters,
            steerInstruction = instruction
        )
    }

    fun formatDistance(meters: Double): String = when {
        meters < 1000.0 -> String.format(Locale.US, "%.0f m", meters)
        meters < 25_000.0 -> String.format(Locale.US, "%.2f km", meters / 1000.0)
        else -> String.format(Locale.US, "%.0f km", meters / 1000.0)
    }

    fun formatDuration(seconds: Double): String {
        val totalMin = (seconds / 60.0).toInt().coerceAtLeast(1)
        val hrs = totalMin / 60
        val mins = totalMin % 60
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins} min"
    }
}

/**
 * Hardware GPS (GPS_PROVIDER + Network fallback) & Sensor Compass tracker.
 * Works completely OFFLINE with zero internet connectivity for real field navigation.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberOfflineGpsCompassState(
    locationPermissionGranted: Boolean,
    onFixUpdated: ((Double, Double) -> Unit)? = null
): State<LiveGpsCompassState> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(LiveGpsCompassState()) }

    DisposableEffect(context, locationPermissionGranted) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeo = false
        val rotMatrix = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)
        var smoothAzimuth = 0f
        var smoothPitch = 15f

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var updated = false
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        // Remap for upright camera / AR sky looking forward along -Z
                        SensorManager.remapCoordinateSystem(
                            rotMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Z,
                            remapped
                        )
                        SensorManager.getOrientation(remapped, orientation)
                        updated = true
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                        if (hasGeo && rotationSensor == null &&
                            SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)
                        ) {
                            SensorManager.remapCoordinateSystem(
                                rotMatrix,
                                SensorManager.AXIS_X,
                                SensorManager.AXIS_Z,
                                remapped
                            )
                            SensorManager.getOrientation(remapped, orientation)
                            updated = true
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasGeo = true
                    }
                }

                if (updated) {
                    val rawAz = (((Math.toDegrees(orientation[0].toDouble()).toFloat()) % 360f) + 360f) % 360f
                    val rawPitch = (-Math.toDegrees(orientation[1].toDouble()).toFloat()).coerceIn(-85f, 85f)
                    val roll = Math.toDegrees(orientation[2].toDouble()).toFloat().coerceIn(-45f, 45f)

                    val dAz = ArSkyProjectionEngine.shortestAngleDiffDeg(rawAz, smoothAzimuth)
                    smoothAzimuth = (((smoothAzimuth + dAz * 0.22f) % 360f) + 360f) % 360f
                    smoothPitch += (rawPitch - smoothPitch) * 0.22f

                    state.value = state.value.copy(
                        compassHeadingDeg = smoothAzimuth,
                        cameraPitchDeg = smoothPitch,
                        compassRollDeg = roll,
                        hasHardwareCompass = true
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelSensor != null) sensorManager?.registerListener(sensorListener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            if (magSensor != null) sensorManager?.registerListener(sensorListener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        fun applyLocation(loc: Location) {
            val prevTrail = state.value.breadcrumbTrail
            val last = prevTrail.lastOrNull()
            val shouldAppend = last == null ||
                Globe3DEngine.haversineKm(last.lat, last.lon, loc.latitude, loc.longitude) >= 0.015
            val nextTrail = if (shouldAppend) {
                (prevTrail + GeoPoint3D(loc.latitude, loc.longitude, loc.altitude / 1000.0)).takeLast(180)
            } else prevTrail

            state.value = state.value.copy(
                hasGpsFix = true,
                lat = loc.latitude,
                lon = loc.longitude,
                altitudeMeters = loc.altitude,
                speedMetersPerSec = loc.speed,
                accuracyMeters = loc.accuracy,
                gpsBearingDeg = loc.bearing,
                provider = (loc.provider ?: "GPS").uppercase(Locale.US),
                updatedAtMs = System.currentTimeMillis(),
                breadcrumbTrail = nextTrail
            )
            onFixUpdated?.invoke(loc.latitude, loc.longitude)
        }

        val locListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                applyLocation(location)
            }
            @Deprecated("Deprecated in Android API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        if (locationPermissionGranted && locationManager != null) {
            runCatching {
                val gpsLast = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val netLast = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val bestLast = listOfNotNull(gpsLast, netLast).maxByOrNull { it.time }
                if (bestLast != null) applyLocation(bestLast)

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2_000L,
                        2f,
                        locListener
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        5_000L,
                        10f,
                        locListener
                    )
                }
            }
        }

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
            runCatching { locationManager?.removeUpdates(locListener) }
        }
    }

    return state
}
